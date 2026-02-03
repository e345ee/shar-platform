#!/usr/bin/env bash
set -euo pipefail

# ------------------------------------------------------------
# v6: add TEXT + OPEN questions, manual grading, teacher pending queue, lesson open gating (2nd start may return 201) + json_len + 2 tests across 2 lessons
# E2E smoke-test for:
# - roles/users: ADMIN -> create METHODIST; METHODIST -> create TEACHER
# - METHODIST: create COURSE + CLASS
# - anonymous: create join request
# - TEACHER: approve join request
# - ADMIN: set known password for the approved STUDENT
# - METHODIST: create lessons with numbering + upload PDF
# - access: METHODIST/TEACHER/STUDENT can view lessons in their course;
#           STUDENT cannot view lessons from another course
# - PDF slide paging: /presentation/info and /presentation/pages/{n}
# - TESTS (homework) + QUESTIONS:
#     * METHODIST: create draft test, update, add/update/delete questions
#     * METHODIST: publish (ready)
#     * STUDENT: start attempt, submit answers, auto-grading
#     * TEACHER: can view student's results (attempts list + attempt detail)
#
# Prereqs:
# - backend is running (default: http://localhost:8080)
# - DB is migrated (table.sql applied)
# - S3/MinIO configured and reachable (PDF upload works)
# - curl available
# - jq OR python3 available
# ------------------------------------------------------------

BASE_URL="${BASE_URL:-http://localhost:8080}"

ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
ADMIN_AUTH="$ADMIN_USER:$ADMIN_PASS"

# Test users passwords
METHODIST_PASS="MethodistPass1!"
TEACHER_PASS="TeacherPass1!"
STUDENT_PASS="StudentPass1!"

SUF="$(date +%Y%m%d%H%M%S)-$RANDOM"

WORKDIR="$(mktemp -d)"
KEEP_ARTIFACTS="${KEEP_ARTIFACTS:-1}"
cleanup() {
  if [[ "${KEEP_ARTIFACTS}" == "1" ]]; then
    echo -e "\nArtifacts kept in: $WORKDIR"
  else
    rm -rf "$WORKDIR"
  fi
}
trap cleanup EXIT

log() { printf "\n==> %s\n" "$*"; }
fail() { echo -e "\n[FAIL] $*" >&2; exit 1; }
pass() { echo "[OK] $*"; }

need_tool() {
  command -v "$1" >/dev/null 2>&1 || fail "Required tool '$1' not found";
}

need_tool curl
need_tool base64

json_get() {
  local json="$1"
  local expr="$2"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -r "$expr"
    return 0
  fi
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$expr" <<'PY' <<<"$json"
import json,sys,re
expr=sys.argv[1].strip()
obj=json.load(sys.stdin)
if expr.startswith('.'): expr=expr[1:]
if expr=='':
    print(json.dumps(obj))
    sys.exit(0)
parts=[]
buf=''
for ch in expr:
    if ch=='.':
        if buf: parts.append(buf); buf=''
        continue
    buf+=ch
if buf: parts.append(buf)
for p in parts:
    if p.startswith('['):
        for im in re.finditer(r'\[(\d+)\]', p):
            obj=obj[int(im.group(1))]
        continue
    m=re.match(r'^([A-Za-z0-9_\-]+)(\[.*\])?$', p)
    if not m:
        raise SystemExit(1)
    key=m.group(1)
    if isinstance(obj, dict):
        obj=obj[key]
    else:
        obj=obj[int(key)]
    idx_part=m.group(2)
    if idx_part:
        for im in re.finditer(r'\[(\d+)\]', idx_part):
            obj=obj[int(im.group(1))]
if obj is None:
    print('')
elif isinstance(obj,(dict,list)):
    print(json.dumps(obj))
else:
    print(obj)
PY
    return 0
  fi
  fail "Need jq or python3 for JSON parsing"
}

json_len() {
  # Usage: json_len "$json"
  echo "$1" | jq 'length'
}

HTTP_BODY=""
HTTP_CODE=""

request_json() {
  local method="$1"; shift
  local url="$1"; shift
  local auth="$1"; shift

  local out="$WORKDIR/resp.json"
  if [[ -n "$auth" ]]; then
    HTTP_CODE=$(curl -sS -o "$out" -w "%{http_code}" -u "$auth" -X "$method" "$BASE_URL$url" "$@")
  else
    HTTP_CODE=$(curl -sS -o "$out" -w "%{http_code}" -X "$method" "$BASE_URL$url" "$@")
  fi
  HTTP_BODY=$(cat "$out")
}

request_png() {
  local url="$1"; shift
  local auth="$1"; shift
  local out_file="$1"; shift

  local hdr="$WORKDIR/headers.txt"
  HTTP_CODE=$(curl -sS -D "$hdr" -o "$out_file" -w "%{http_code}" -u "$auth" "$BASE_URL$url" "$@")
  HTTP_BODY=$(cat "$hdr")
}

expect_code() {
  local expected="$1"; shift
  local ctx="$*"
  [[ "$HTTP_CODE" == "$expected" ]] || {
    echo "--- response body ---" >&2
    echo "$HTTP_BODY" >&2
    echo "---------------------" >&2
    fail "$ctx (expected HTTP $expected, got $HTTP_CODE)"
  }
}

expect_code_one_of() {
  local ctx="$1"; shift
  local got="$HTTP_CODE"
  for e in "$@"; do
    if [[ "$got" == "$e" ]]; then return 0; fi
  done
  echo "--- response body ---" >&2
  echo "$HTTP_BODY" >&2
  echo "---------------------" >&2
  fail "$ctx (expected one of: $*, got $got)"
}

# ------------------------------------------------------------
# 0) Health
# ------------------------------------------------------------
log "Health check"
request_json GET "/actuator/health" "" -H "Accept: application/json"
expect_code 200 "health check"
pass "Backend is up"

# ------------------------------------------------------------
# 1) Create METHODIST (ADMIN)
# ------------------------------------------------------------
METHODIST_NAME="methodist_$SUF"
METHODIST_EMAIL="methodist_$SUF@example.com"

log "Create METHODIST as ADMIN"
request_json POST "/api/users/admin/methodists" "$ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$METHODIST_NAME\",\"email\":\"$METHODIST_EMAIL\",\"password\":\"$METHODIST_PASS\"}"
expect_code 201 "create methodist"
METHODIST_ID=$(json_get "$HTTP_BODY" '.id')
[[ -n "$METHODIST_ID" && "$METHODIST_ID" != "null" ]] || fail "No methodist id in response"
pass "Methodist created: id=$METHODIST_ID"

METHODIST_AUTH="$METHODIST_EMAIL:$METHODIST_PASS"

# ------------------------------------------------------------
# 2) Create TEACHER (METHODIST)
# ------------------------------------------------------------
TEACHER_NAME="teacher_$SUF"
TEACHER_EMAIL="teacher_$SUF@example.com"

log "Create TEACHER as METHODIST"
request_json POST "/api/users/methodists/$METHODIST_ID/teachers" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$TEACHER_NAME\",\"email\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASS\"}"
expect_code 201 "create teacher"
TEACHER_ID=$(json_get "$HTTP_BODY" '.id')
[[ -n "$TEACHER_ID" && "$TEACHER_ID" != "null" ]] || fail "No teacher id in response"
pass "Teacher created: id=$TEACHER_ID"

TEACHER_AUTH="$TEACHER_EMAIL:$TEACHER_PASS"

# ------------------------------------------------------------
# 3) Create COURSE (METHODIST)
# ------------------------------------------------------------
COURSE_NAME="Course_$SUF"
log "Create COURSE as METHODIST"
request_json POST "/api/courses" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$COURSE_NAME\",\"description\":\"E2E test course $SUF\"}"
expect_code 201 "create course"
COURSE_ID=$(json_get "$HTTP_BODY" '.id')
pass "Course created: id=$COURSE_ID"

# ------------------------------------------------------------
# 4) Create CLASS (METHODIST) with assigned TEACHER
# ------------------------------------------------------------
CLASS_NAME="Class_$SUF"
log "Create CLASS as METHODIST"
request_json POST "/api/classes" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$CLASS_NAME\",\"courseId\":$COURSE_ID,\"teacherId\":$TEACHER_ID}"
expect_code 201 "create class"
CLASS_ID=$(json_get "$HTTP_BODY" '.id')
CLASS_CODE=$(json_get "$HTTP_BODY" '.joinCode')
[[ ${#CLASS_CODE} -eq 8 ]] || fail "joinCode must be 8 chars, got '$CLASS_CODE'"
pass "Class created: id=$CLASS_ID joinCode=$CLASS_CODE"

# ------------------------------------------------------------
# 5) Create JOIN REQUEST (anonymous)
# ------------------------------------------------------------
STUDENT_NAME="student_$SUF"
STUDENT_EMAIL="student_$SUF@example.com"
STUDENT_TG="tg_$SUF"

log "Create JOIN REQUEST (no auth)"
request_json POST "/api/join-requests" "" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$STUDENT_NAME\",\"email\":\"$STUDENT_EMAIL\",\"tgId\":\"$STUDENT_TG\",\"classCode\":\"$CLASS_CODE\"}"
expect_code 201 "create join request"
REQUEST_ID=$(json_get "$HTTP_BODY" '.id')
pass "Join request created: id=$REQUEST_ID"

# ------------------------------------------------------------
# 6) Approve JOIN REQUEST (TEACHER)
# ------------------------------------------------------------
log "Approve JOIN REQUEST as TEACHER"
request_json POST "/api/classes/$CLASS_ID/join-requests/$REQUEST_ID/approve" "$TEACHER_AUTH" \
  -H "Accept: application/json"
expect_code 200 "approve join request"
STUDENT_ID=$(json_get "$HTTP_BODY" '.id')
pass "Student approved: id=$STUDENT_ID"

# ------------------------------------------------------------
# 7) Set known STUDENT password (ADMIN) so we can login in this script
# ------------------------------------------------------------
log "Fetch STUDENT role id (ADMIN)"
request_json GET "/api/roles/name/STUDENT" "$ADMIN_AUTH" -H "Accept: application/json"
expect_code 200 "get STUDENT role"
ROLE_STUDENT_ID=$(json_get "$HTTP_BODY" '.id')

log "Set known password for the approved STUDENT (ADMIN)"
request_json PUT "/api/users/$STUDENT_ID" "$ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"id\":$STUDENT_ID,\"roleId\":$ROLE_STUDENT_ID,\"name\":\"$STUDENT_NAME\",\"email\":\"$STUDENT_EMAIL\",\"password\":\"$STUDENT_PASS\",\"tgId\":\"$STUDENT_TG\"}"
expect_code 200 "set student password"
pass "Student password set"

STUDENT_AUTH="$STUDENT_EMAIL:$STUDENT_PASS"

# ------------------------------------------------------------
# 8) Prepare a tiny 2-page PDF for upload
# ------------------------------------------------------------
PDF_FILE="$WORKDIR/test_presentation.pdf"
log "Create 2-page test PDF"
cat > "$PDF_FILE.b64" <<'B64'
JVBERi0xLjMKJZOMi54gUmVwb3J0TGFiIEdlbmVyYXRlZCBQREYgZG9jdW1lbnQgaHR0cDovL3d3
dy5yZXBvcnRsYWIuY29tCjEgMCBvYmoKPDwKL0YxIDIgMCBSCj4+CmVuZG9iagoyIDAgb2JqCjw8
Ci9CYXNlRm9udCAvSGVsdmV0aWNhIC9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIC9OYW1lIC9G
MSAvU3VidHlwZSAvVHlwZTEgL1R5cGUgL0ZvbnQKPj4KZW5kb2JqCjMgMCBvYmoKPDwKL0NvbnRl
bnRzIDggMCBSIC9NZWRpYUJveCBbIDAgMCA2MTIgNzkyIF0gL1BhcmVudCA3IDAgUiAvUmVzb3Vy
Y2VzIDw8Ci9Gb250IDEgMCBSIC9Qcm9jU2V0IFsgL1BERiAvVGV4dCAvSW1hZ2VCIC9JbWFnZUMg
L0ltYWdlSSBdCj4+IC9Sb3RhdGUgMCAvVHJhbnMgPDwKCj4+IAogIC9UeXBlIC9QYWdlCj4+CmVu
ZG9iago0IDAgb2JqCjw8Ci9Db250ZW50cyA5IDAgUiAvTWVkaWFCb3ggWyAwIDAgNjEyIDc5MiBd
IC9QYXJlbnQgNyAwIFIgL1Jlc291cmNlcyA8PAovRm9udCAxIDAgUiAvUHJvY1NldCBbIC9QREYg
L1RleHQgL0ltYWdlQiAvSW1hZ2VDIC9JbWFnZUkgXQo+PiAvUm90YXRlIDAgL1RyYW5zIDw8Cgo+
PiAKICAvVHlwZSAvUGFnZQo+PgplbmRvYmoKNSAwIG9iago8PAovUGFnZU1vZGUgL1VzZU5vbmUg
L1BhZ2VzIDcgMCBSIC9UeXBlIC9DYXRhbG9nCj4+CmVuZG9iago2IDAgb2JqCjw8Ci9BdXRob3Ig
KGFub255bW91cykgL0NyZWF0aW9uRGF0ZSAoRDoyMDI2MDIwMjIxNDc0MiswMCcwMCcpIC9DcmVh
dG9yIChSZXBvcnRMYWIgUERGIExpYnJhcnkgLSB3d3cucmVwb3J0bGFiLmNvbSkgL0tleXdvcmRz
ICgpIC9Nb2REYXRlIChEOjIwMjYwMjAyMjE0NzQyKzAwJzAwJykgL1Byb2R1Y2VyIChSZXBvcnRM
YWIgUERGIExpYnJhcnkgLSB3d3cucmVwb3J0bGFiLmNvbSkgCiAgL1N1YmplY3QgKHVuc3BlY2lm
aWVkKSAvVGl0bGUgKHVudGl0bGVkKSAvVHJhcHBlZCAvRmFsc2UKPj4KZW5kb2JqCjcgMCBvYmoK
PDwKL0NvdW50IDIgL0tpZHMgWyAzIDAgUiA0IDAgUiBdIC9UeXBlIC9QYWdlcwo+PgplbmRvYmoK
OCAwIG9iago8PAovRmlsdGVyIFsgL0FTQ0lJODVEZWNvZGUgL0ZsYXRlRGVjb2RlIF0gL0xlbmd0
aCAxNjEKPj4Kc3RyZWFtCkdhclcwWW1TPzUmLV9yWWBLWStXQXJmO2gvIjRVSUUuOi5GIWcrYFVc
YmdsOClYbiplYHRlYjU2OTFUYXJpTHNoWkU9QTlCUy87KWNrQW8kQW8/Rm9nV0VKWy9vdEs7Vmox
ckFpbVA1WU8iQVpgXSNzciNoOT1kQ146c0gyMyQybigwW2ZiNl9uXj9jZDE+SzFnK1xucTlecjB1
YSJDLX4+ZW5kc3RyZWFtCmVuZG9iago5IDAgb2JqCjw8Ci9GaWx0ZXIgWyAvQVNDSUk4NURlY29k
ZSAvRmxhdGVEZWNvZGUgXSAvTGVuZ3RoIDEzNgo+PgpzdHJlYW0KR2FwUWgwRT1GLDBVXEgzVFxw
TllUXlFLaz90Yz5JUCw7VyNVMV4yM2loUEVNXz9DWjUxJyo0Tj1tMCEyYCFyRDBRbD8nZ0pPMm8x
SmpzXVNBa0soVE9ldEg5UTlwQkNQKCU2MCwzXyVtS14pPy1jSGYtXC1xSSQtZU8pKVtKSTtJcFo2
amV+PmVuZHN0cmVhbQplbmRvYmoKeHJlZgowIDEwCjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAw
MDA3MyAwMDAwMCBuIAowMDAwMDAwMTA0IDAwMDAwIG4gCjAwMDAwMDAyMTEgMDAwMDAgbiAKMDAw
MDAwMDQwNCAwMDAwMCBuIAowMDAwMDAwNTk3IDAwMDAwIG4gCjAwMDAwMDA2NjUgMDAwMDAgbiAK
MDAwMDAwMDk2MSAwMDAwMCBuIAowMDAwMDAxMDI2IDAwMDAwIG4gCjAwMDAwMDEyNzcgMDAwMDAg
biAKdHJhaWxlcgo8PAovSUQgCls8NzY1ZTA1MTU3ZTZkZGFlYjRjNDBlYmM5MTExYmI2YzQ+PDc2
NWUwNTE1N2U2ZGRhZWI0YzQwZWJjOTExMWJiNmM0Pl0KJSBSZXBvcnRMYWIgZ2VuZXJhdGVkIFBE
RiBkb2N1bWVudCAtLSBkaWdlc3QgKGh0dHA6Ly93d3cucmVwb3J0bGFiLmNvbSkKCi9JbmZvIDYg
MCBSCi9Sb290IDUgMCBSCi9TaXplIDEwCj4+CnN0YXJ0eHJlZgoxNTAzCiUlRU9GCg==
B64

base64 -d "$PDF_FILE.b64" > "$PDF_FILE"
[[ -s "$PDF_FILE" ]] || fail "PDF was not created"
pass "PDF created at $PDF_FILE"

# ------------------------------------------------------------
# 9) Create LESSON 1 (METHODIST) - appended, should become orderIndex=1
# ------------------------------------------------------------
log "Create LESSON #1 (append) with PDF"
request_json POST "/api/courses/$COURSE_ID/lessons" "$METHODIST_AUTH" \
  -H "Accept: application/json" \
  -F "title=Lesson_A_$SUF" \
  -F "description=First lesson" \
  -F "presentation=@$PDF_FILE;type=application/pdf"
expect_code 201 "create lesson 1"
LESSON_A_ID=$(json_get "$HTTP_BODY" '.id')
LESSON_A_ORDER=$(json_get "$HTTP_BODY" '.orderIndex')
[[ "$LESSON_A_ORDER" == "1" ]] || fail "Expected lesson A orderIndex=1, got $LESSON_A_ORDER"
pass "Lesson A created: id=$LESSON_A_ID orderIndex=$LESSON_A_ORDER"

# ------------------------------------------------------------
# 10) Create LESSON 2 (METHODIST) - insert at position 1; should reorder
# ------------------------------------------------------------
log "Create LESSON #2 (insert at orderIndex=1)"
request_json POST "/api/courses/$COURSE_ID/lessons" "$METHODIST_AUTH" \
  -H "Accept: application/json" \
  -F "orderIndex=1" \
  -F "title=Lesson_B_$SUF" \
  -F "description=Second lesson but should be first" \
  -F "presentation=@$PDF_FILE;type=application/pdf"
expect_code 201 "create lesson 2"
LESSON_B_ID=$(json_get "$HTTP_BODY" '.id')
LESSON_B_ORDER=$(json_get "$HTTP_BODY" '.orderIndex')
[[ "$LESSON_B_ORDER" == "1" ]] || fail "Expected lesson B orderIndex=1, got $LESSON_B_ORDER"
pass "Lesson B created: id=$LESSON_B_ID orderIndex=$LESSON_B_ORDER"

log "Verify lesson order by listing lessons in course"
request_json GET "/api/courses/$COURSE_ID/lessons" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "list lessons as methodist"

FIRST_ID=$(json_get "$HTTP_BODY" '.[0].id')
FIRST_ORDER=$(json_get "$HTTP_BODY" '.[0].orderIndex')
SECOND_ID=$(json_get "$HTTP_BODY" '.[1].id')
SECOND_ORDER=$(json_get "$HTTP_BODY" '.[1].orderIndex')

[[ "$FIRST_ID" == "$LESSON_B_ID" && "$FIRST_ORDER" == "1" ]] || fail "Expected first lesson to be B with orderIndex=1"
[[ "$SECOND_ID" == "$LESSON_A_ID" && "$SECOND_ORDER" == "2" ]] || fail "Expected second lesson to be A with orderIndex=2"
pass "Lesson ordering is correct"

LESSON_ID="$LESSON_B_ID"
LESSON2_ID="$LESSON_A_ID"

# ------------------------------------------------------------
# 11) Access tests + gating: STUDENT cannot view lesson until TEACHER opens it for the class
# ------------------------------------------------------------
log "Access: METHODIST can GET lesson"
request_json GET "/api/lessons/$LESSON_ID" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist get lesson"
pass "Methodist can view lesson"

log "Access: TEACHER can GET lesson"
request_json GET "/api/lessons/$LESSON_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher get lesson"
pass "Teacher can view lesson"

log "Access: STUDENT cannot GET lesson before teacher opens it (should be 403)"
request_json GET "/api/lessons/$LESSON_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student get lesson blocked before open" 403 404
pass "Student is blocked until lesson is opened by teacher"

log "TEACHER opens lesson for the class"
request_json POST "/api/teachers/me/classes/$CLASS_ID/lessons/$LESSON_ID/open" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher open lesson" 200 201 204
pass "Lesson opened for class"

log "Access: STUDENT can GET lesson after open"
request_json GET "/api/lessons/$LESSON_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get lesson after open"
pass "Student can view opened lesson"

# ------------------------------------------------------------
# 12) TESTS: create draft test + questions (SINGLE_CHOICE + TEXT + OPEN) + publish + attempt submit + manual grade
# ------------------------------------------------------------
DEADLINE="$(date -u -d "+3 days" +"%Y-%m-%dT%H:%M:%S")"
# (macOS users can export DEADLINE manually or adjust date command)

log "Student list tests in lesson: should be empty initially"
request_json GET "/api/lessons/$LESSON_ID/tests" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student list lesson tests (initial)"
# should be [] (or empty array)
if [[ "$(echo "$HTTP_BODY" | tr -d ' \n\r\t')" != "[]" ]]; then
  fail "Expected empty list for student before test is created/published, got: $HTTP_BODY"
fi
pass "Student sees no tests initially"

log "Create TEST (DRAFT) as METHODIST"
request_json POST "/api/lessons/$LESSON2_ID/tests" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Test_$SUF\",\"description\":\"Desc_$SUF\",\"topic\":\"Topic_$SUF\",\"deadline\":\"$DEADLINE\"}"
expect_code 201 "create test draft"
TEST_ID=$(json_get "$HTTP_BODY" '.id')
TEST_STATUS=$(json_get "$HTTP_BODY" '.status')
[[ -n "$TEST_ID" && "$TEST_ID" != "null" ]] || fail "No test id"
[[ "$TEST_STATUS" == "DRAFT" ]] || fail "Expected status DRAFT, got $TEST_STATUS"
pass "Test draft created: id=$TEST_ID"

log "Student list tests in lesson: still empty because test is DRAFT"
request_json GET "/api/lessons/$LESSON_ID/tests" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student list lesson tests (draft hidden)"
if [[ "$(echo "$HTTP_BODY" | tr -d ' \n\r\t')" != "[]" ]]; then
  fail "Expected empty list for student while test is DRAFT, got: $HTTP_BODY"
fi
pass "Draft test is hidden from student"

log "Update TEST (still DRAFT) as METHODIST"
request_json PUT "/api/tests/$TEST_ID" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"TestUpdated_$SUF\",\"description\":\"DescUpdated_$SUF\",\"topic\":\"TopicUpdated_$SUF\",\"deadline\":\"$DEADLINE\"}"
expect_code 200 "update test"
NEW_TITLE=$(json_get "$HTTP_BODY" '.title')
[[ "$NEW_TITLE" == "TestUpdated_$SUF" ]] || fail "Expected updated title"
pass "Test updated"

log "Create QUESTION #1 (SINGLE_CHOICE, points=2) as METHODIST"
request_json POST "/api/tests/$TEST_ID/questions" "$METHODIST_AUTH"   -H "Content-Type: application/json"   -d "{\"orderIndex\":1,\"questionText\":\"2+2=?\",\"questionType\":\"SINGLE_CHOICE\",\"points\":2,\"option1\":\"3\",\"option2\":\"4\",\"option3\":\"5\",\"option4\":\"22\",\"correctOption\":2}"
expect_code 201 "create question 1"
Q1_ID=$(json_get "$HTTP_BODY" '.id')
pass "Question 1 created: id=$Q1_ID"

log "Create QUESTION #2 (TEXT, points=3) as METHODIST"
request_json POST "/api/tests/$TEST_ID/questions" "$METHODIST_AUTH"   -H "Content-Type: application/json"   -d "{\"orderIndex\":2,\"questionText\":\"Capital of France? (type answer)\",\"questionType\":\"TEXT\",\"points\":3,\"correctTextAnswer\":\"Paris\"}"
expect_code 201 "create text question"
Q2_ID=$(json_get "$HTTP_BODY" '.id')
pass "Question 2 (TEXT) created: id=$Q2_ID"

log "Create QUESTION #3 (OPEN, points=5) as METHODIST"
request_json POST "/api/tests/$TEST_ID/questions" "$METHODIST_AUTH"   -H "Content-Type: application/json"   -d "{\"orderIndex\":3,\"questionText\":\"Explain why testing matters (2-3 sentences).\",\"questionType\":\"OPEN\",\"points\":5}"
expect_code 201 "create open question"
Q3_ID=$(json_get "$HTTP_BODY" '.id')
pass "Question 3 (OPEN) created: id=$Q3_ID"

log "Update QUESTION #2 text as METHODIST"
request_json PUT "/api/tests/$TEST_ID/questions/$Q2_ID" "$METHODIST_AUTH"   -H "Content-Type: application/json"   -d "{\"orderIndex\":2,\"questionText\":\"What is the capital of France? (type answer)\",\"questionType\":\"TEXT\",\"points\":3,\"correctTextAnswer\":\"Paris\"}"
expect_code 200 "update text question"
pass "Question 2 updated"

log "Create and DELETE a temp QUESTION #4 as METHODIST (delete path check)"
request_json POST "/api/tests/$TEST_ID/questions" "$METHODIST_AUTH"   -H "Content-Type: application/json"   -d "{\"orderIndex\":4,\"questionText\":\"Temp question\",\"questionType\":\"SINGLE_CHOICE\",\"points\":1,\"option1\":\"A\",\"option2\":\"B\",\"option3\":\"C\",\"option4\":\"D\",\"correctOption\":1}"
expect_code 201 "create temp question"
Q4_ID=$(json_get "$HTTP_BODY" '.id')

request_json DELETE "/api/tests/$TEST_ID/questions/$Q4_ID" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code_one_of "delete temp question" 200 204
pass "Temp question deleted"

log "Publish TEST: DRAFT -> READY"
request_json POST "/api/tests/$TEST_ID/ready" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "publish test ready"
READY_STATUS=$(json_get "$HTTP_BODY" '.status')
[[ "$READY_STATUS" == "READY" ]] || fail "Expected READY after publish, got $READY_STATUS"
pass "Test published"


# ------------------------------------------------------------
# 12b) Create SECOND TEST on same lesson: more quiz coverage
# ------------------------------------------------------------
log "Create SECOND TEST (DRAFT) as METHODIST on second lesson"
request_json POST "/api/lessons/$LESSON_ID/tests" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Test2_$SUF\",\"description\":\"Desc2_$SUF\",\"topic\":\"Topic2_$SUF\",\"deadline\":\"$DEADLINE\"}"
expect_code 201 "create second test draft"
TEST2_ID=$(json_get "$HTTP_BODY" '.id')
TEST2_STATUS=$(json_get "$HTTP_BODY" '.status')
[[ -n "$TEST2_ID" && "$TEST2_ID" != "null" ]] || fail "No test2 id"
[[ "$TEST2_STATUS" == "DRAFT" ]] || fail "Expected status DRAFT for test2, got $TEST2_STATUS"
pass "Second test draft created: id=$TEST2_ID"

log "Add 3 questions to SECOND TEST as METHODIST"
request_json POST "/api/tests/$TEST2_ID/questions" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"orderIndex\":1,\"questionType\":\"SINGLE_CHOICE\",\"points\":1,\"questionText\":\"1+1=?\",\"option1\":\"1\",\"option2\":\"2\",\"option3\":\"3\",\"option4\":\"4\",\"correctOption\":2}"
expect_code 201 "create test2 q1"
T2Q1_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/tests/$TEST2_ID/questions" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"orderIndex\":2,\"questionType\":\"SINGLE_CHOICE\",\"points\":1,\"questionText\":\"Select the vowel\",\"option1\":\"b\",\"option2\":\"c\",\"option3\":\"a\",\"option4\":\"d\",\"correctOption\":3}"
expect_code 201 "create test2 q2"
T2Q2_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/tests/$TEST2_ID/questions" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"orderIndex\":3,\"questionType\":\"SINGLE_CHOICE\",\"points\":1,\"questionText\":\"HTTP status for forbidden?\",\"option1\":\"200\",\"option2\":\"401\",\"option3\":\"403\",\"option4\":\"500\",\"correctOption\":3}"
expect_code 201 "create test2 q3"
T2Q3_ID=$(json_get "$HTTP_BODY" '.id')
pass "Second test questions created"

log "Publish SECOND TEST: DRAFT -> READY"
request_json POST "/api/tests/$TEST2_ID/ready" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "publish test2 ready"
TEST2_READY_STATUS=$(json_get "$HTTP_BODY" '.status')
[[ "$TEST2_READY_STATUS" == "READY" ]] || fail "Expected READY for test2 after publish, got $TEST2_READY_STATUS"
pass "Second test published"

log "TEACHER opens lesson2 for the class (required for student visibility)"
request_json POST "/api/teachers/me/classes/$CLASS_ID/lessons/$LESSON2_ID/open" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher open lesson2" 200 201 204
pass "Lesson2 opened for class"

log "Student list tests in lesson2: should include READY test2"
request_json GET "/api/lessons/$LESSON2_ID/tests" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student list tests in lesson2"
LIST2_COUNT=$(json_len "$HTTP_BODY")
[[ "$LIST2_COUNT" == "1" ]] || fail "Expected 1 visible test in lesson2, got $LIST2_COUNT: $HTTP_BODY"
pass "Student sees READY test in lesson2"


log "Student list tests in lesson: should include READY test"
request_json GET "/api/lessons/$LESSON_ID/tests" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student list lesson tests (ready visible)"
LIST_COUNT=$(json_get "$HTTP_BODY" 'length')
[[ "$LIST_COUNT" == "1" ]] || fail "Expected 1 visible test, got $LIST_COUNT: $HTTP_BODY"
pass "Student sees published test"

log "Student GET test (public view): should NOT contain correctOption/correctTextAnswer"
request_json GET "/api/tests/$TEST_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get test"
if echo "$HTTP_BODY" | grep -q "correctOption"; then
  fail "Public test view leaked correctOption: $HTTP_BODY"
fi
if echo "$HTTP_BODY" | grep -q "correctTextAnswer"; then
  fail "Public test view leaked correctTextAnswer: $HTTP_BODY"
fi
pass "Public view doesn't leak answers"

log "Attempt to EDIT published test as METHODIST should fail"
request_json PUT "/api/tests/$TEST_ID" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"ShouldFail_$SUF\",\"description\":\"x\",\"topic\":\"x\",\"deadline\":\"$DEADLINE\"}"
# depending on your exception mapping it might be 400/409/403
expect_code_one_of "edit published test" 400 403 409
pass "Editing published test is blocked"

# ---------------- Student attempt flow ----------------
log "Student START attempt"
request_json POST "/api/tests/$TEST_ID/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 201 "start attempt"
ATTEMPT_ID=$(json_get "$HTTP_BODY" '.id')
ATT_STATUS=$(json_get "$HTTP_BODY" '.status')
[[ -n "$ATTEMPT_ID" && "$ATTEMPT_ID" != "null" ]] || fail "No attempt id"
[[ "$ATT_STATUS" == "IN_PROGRESS" ]] || fail "Expected IN_PROGRESS, got $ATT_STATUS"
pass "Attempt started: id=$ATTEMPT_ID"

log "Student SUBMIT attempt (SINGLE_CHOICE correct, TEXT correct, OPEN requires manual grading)"
request_json POST "/api/attempts/$ATTEMPT_ID/submit" "$STUDENT_AUTH"   -H "Content-Type: application/json"   -d "{\"answers\":[{\"questionId\":$Q1_ID,\"selectedOption\":2},{\"questionId\":$Q2_ID,\"textAnswer\":\"  paris  \"},{\"questionId\":$Q3_ID,\"textAnswer\":\"Testing matters because it catches bugs early and protects users. It also makes changes safer.\"}]}"
expect_code 200 "submit attempt"
SCORE=$(json_get "$HTTP_BODY" '.score')
MAXS=$(json_get "$HTTP_BODY" '.maxScore')
STATUS=$(json_get "$HTTP_BODY" '.status')
[[ "$SCORE" == "5" ]] || fail "Expected score=5 (2+3), got $SCORE ($HTTP_BODY)"
[[ "$MAXS" == "10" ]] || fail "Expected maxScore=10 (2+3+5), got $MAXS ($HTTP_BODY)"
[[ "$STATUS" == "SUBMITTED" ]] || fail "Expected status SUBMITTED (needs manual grading), got $STATUS"
pass "Attempt submitted: $SCORE/$MAXS (manual grading pending)"

log "Student GET attempt detail: should show isCorrect flags"
request_json GET "/api/attempts/$ATTEMPT_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get attempt detail"
# Ensure at least one isCorrect present
if ! echo "$HTTP_BODY" | grep -q "\"isCorrect\""; then
  fail "Expected isCorrect fields in attempt detail: $HTTP_BODY"
fi
pass "Student can view graded attempt with correctness flags"

# ---------------- Teacher can view results ----------------
log "Teacher LIST attempts for test: should include student's attempt with score"
request_json GET "/api/tests/$TEST_ID/attempts" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher list attempts"
TEACHER_COUNT=$(json_get "$HTTP_BODY" 'length')
[[ "$TEACHER_COUNT" -ge 1 ]] || fail "Expected >=1 attempt in teacher view, got: $HTTP_BODY"
pass "Teacher can list attempts"

log "Teacher GET attempt detail"
request_json GET "/api/attempts/$ATTEMPT_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher get attempt detail"
pass "Teacher can view attempt detail"

# ---------------- Teacher pending queue + partial manual grading ----------------
log "Teacher PENDING attempts list (should include our SUBMITTED attempt with 1 ungraded OPEN answer)"
request_json GET "/api/teachers/me/attempts/pending?courseId=$COURSE_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher pending attempts"
# must contain ATTEMPT_ID somewhere
if ! echo "$HTTP_BODY" | grep -q "\"attemptId\":$ATTEMPT_ID"; then
  fail "Expected pending list to contain attemptId=$ATTEMPT_ID, got: $HTTP_BODY"
fi
pass "Pending queue contains submitted attempt"

log "Negative: Teacher grade with too-long feedback should fail (validation max=2048)"
LONG_FEEDBACK="$(python3 - <<'PY'
print('x'*2050)
PY
)"
request_json PUT "/api/attempts/$ATTEMPT_ID/grade" "$TEACHER_AUTH"   -H "Content-Type: application/json"   -d "{\"grades\":[{\"questionId\":$Q3_ID,\"pointsAwarded\":4,\"feedback\":\"$LONG_FEEDBACK\"}]}"
expect_code_one_of "too-long feedback rejected" 400 422
pass "Feedback length validation works"

log "Teacher PARTIAL grade: grade only OPEN question with pointsAwarded=4 and feedback"
request_json PUT "/api/attempts/$ATTEMPT_ID/grade" "$TEACHER_AUTH"   -H "Content-Type: application/json"   -d "{\"grades\":[{\"questionId\":$Q3_ID,\"pointsAwarded\":4,\"feedback\":\"Good explanation, but add an example.\"}]}"
expect_code 200 "grade open question"
STATUS_G=$(json_get "$HTTP_BODY" '.status')
SCORE_G=$(json_get "$HTTP_BODY" '.score')
MAXS_G=$(json_get "$HTTP_BODY" '.maxScore')
[[ "$STATUS_G" == "GRADED" ]] || fail "Expected status GRADED after grading all OPEN, got $STATUS_G ($HTTP_BODY)"
[[ "$SCORE_G" == "9" ]] || fail "Expected score=9 after grading (5+4), got $SCORE_G ($HTTP_BODY)"
[[ "$MAXS_G" == "10" ]] || fail "Expected maxScore=10, got $MAXS_G ($HTTP_BODY)"
pass "Attempt graded: $SCORE_G/$MAXS_G"

log "Teacher PENDING attempts list should NOT include attempt anymore"
request_json GET "/api/teachers/me/attempts/pending?courseId=$COURSE_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher pending attempts after grade"
if echo "$HTTP_BODY" | grep -q "\"attemptId\":$ATTEMPT_ID"; then
  fail "Attempt still appears in pending after grading: $HTTP_BODY"
fi
pass "Pending queue cleared after grading"

log "Student GET attempt detail: should contain feedback and pointsAwarded for OPEN answer"
request_json GET "/api/attempts/$ATTEMPT_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get attempt detail after grade"
if ! echo "$HTTP_BODY" | grep -q "\"feedback\""; then
  fail "Expected feedback field in attempt detail: $HTTP_BODY"
fi
if ! echo "$HTTP_BODY" | grep -q "\"pointsAwarded\""; then
  fail "Expected pointsAwarded field in attempt detail: $HTTP_BODY"
fi
pass "Student sees feedback and awarded points after grading"

log "Negative: Student submit with too-long OPEN answer should fail (validation max=4096)"
request_json POST "/api/tests/$TEST_ID/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "start second attempt (platform dependent)" 200 201
ATT3_ID=$(json_get "$HTTP_BODY" '.id')
HUGE_ANSWER="$(python3 - <<'PY'
print('a'*4100)
PY
)"
request_json POST "/api/attempts/$ATT3_ID/submit" "$STUDENT_AUTH"   -H "Content-Type: application/json"   -d "{\"answers\":[{\"questionId\":$Q1_ID,\"selectedOption\":2},{\"questionId\":$Q2_ID,\"textAnswer\":\"Paris\"},{\"questionId\":$Q3_ID,\"textAnswer\":\"$HUGE_ANSWER\"}]}"
expect_code_one_of "too-long student answer rejected" 400 409 422
pass "Student answer length validation works"

# ------------------------------------------------------------
# 13) Negative access: STUDENT cannot view lesson from another course
# ------------------------------------------------------------
log "Create another course+lesson (METHODIST) to test forbidden access for student"
request_json POST "/api/courses" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"OtherCourse_$SUF\",\"description\":\"Other course\"}"
expect_code 201 "create other course"
OTHER_COURSE_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/courses/$OTHER_COURSE_ID/lessons" "$METHODIST_AUTH" \
  -H "Accept: application/json" \
  -F "title=OtherLesson_$SUF" \
  -F "description=Should be forbidden to student" \
  -F "presentation=@$PDF_FILE;type=application/pdf"
expect_code 201 "create other lesson"
OTHER_LESSON_ID=$(json_get "$HTTP_BODY" '.id')

log "Student tries to GET lesson from another course (should be 403)"
request_json GET "/api/lessons/$OTHER_LESSON_ID" "$STUDENT_AUTH" -H "Accept: application/json"
[[ "$HTTP_CODE" == "403" ]] || {
  echo "--- response body ---" >&2
  echo "$HTTP_BODY" >&2
  fail "Expected 403, got $HTTP_CODE"
}
pass "Student is correctly forbidden from other course lesson"

# ------------------------------------------------------------
# 14) Optional: create + delete a DRAFT test on other lesson (delete endpoint check)
# ------------------------------------------------------------
log "Create DRAFT test on other lesson and DELETE it (METHODIST)"
request_json POST "/api/lessons/$OTHER_LESSON_ID/tests" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"TempDelete_$SUF\",\"description\":\"x\",\"topic\":\"x\",\"deadline\":\"$DEADLINE\"}"
expect_code 201 "create temp test"
TEMP_TEST_ID=$(json_get "$HTTP_BODY" '.id')

request_json DELETE "/api/tests/$TEMP_TEST_ID" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code_one_of "delete temp test" 200 204
pass "Draft test deleted successfully"

# ------------------------------------------------------------
# 15) PDF paging: info + pages 1..2 for METHODIST / TEACHER / STUDENT
# ------------------------------------------------------------
log "PDF: presentation info (METHODIST)"
request_json GET "/api/lessons/$LESSON_ID/presentation/info" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "presentation info methodist"
HAS=$(json_get "$HTTP_BODY" '.hasPresentation')
PAGES=$(json_get "$HTTP_BODY" '.pageCount')
[[ "$HAS" == "true" ]] || fail "Expected hasPresentation=true"
[[ "$PAGES" == "2" ]] || fail "Expected pageCount=2, got $PAGES"
pass "Presentation info OK (2 pages)"

fetch_pages_for() {
  local who="$1" auth="$2"
  log "PDF: render pages as PNG for $who"
  for p in 1 2; do
    local out="$WORKDIR/${who}_page_${p}.png"
    request_png "/api/lessons/$LESSON_ID/presentation/pages/$p?dpi=144" "$auth" "$out"
    expect_code 200 "$who page $p"
    [[ -s "$out" ]] || fail "$who page $p: empty PNG"
  done
  pass "$who can render pages 1..2"
}

fetch_pages_for "methodist" "$METHODIST_AUTH"
fetch_pages_for "teacher" "$TEACHER_AUTH"
fetch_pages_for "student" "$STUDENT_AUTH"


# ------------------------------------------------------------
# 16) Security regression: role isolation + negative access tests
# ------------------------------------------------------------
log "Negative: STUDENT cannot access teacher attempts list endpoint"
request_json GET "/api/tests/$TEST_ID/attempts" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student list attempts forbidden" 403 401
pass "Student cannot list attempts (as expected)"

log "Negative: STUDENT cannot delete a test"
request_json DELETE "/api/tests/$TEST_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student delete test forbidden" 403 401
pass "Student cannot delete tests"

log "Negative: TEACHER cannot create or update tests (methodist-only)"
request_json POST "/api/lessons/$LESSON_ID/tests" "$TEACHER_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"TeacherShouldFail_$SUF\",\"description\":\"x\",\"topic\":\"x\",\"deadline\":\"$DEADLINE\"}"
expect_code_one_of "teacher create test forbidden" 403 401
pass "Teacher cannot create tests"

request_json PUT "/api/tests/$TEST_ID" "$TEACHER_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"TeacherUpdateShouldFail_$SUF\",\"description\":\"x\",\"topic\":\"x\",\"deadline\":\"$DEADLINE\"}"
expect_code_one_of "teacher update test forbidden" 403 401
pass "Teacher cannot update tests"

log "Negative: TEACHER cannot delete tests"
request_json DELETE "/api/tests/$TEST_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher delete test forbidden" 403 401
pass "Teacher cannot delete tests"

log "Negative: STUDENT cannot start attempt twice (should fail on second start)"
request_json POST "/api/tests/$TEST2_ID/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 201 "start attempt on test2"
ATTEMPT2_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/tests/$TEST2_ID/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
# Implementation-dependent: 400/409 typical, sometimes 200 returns existing attempt
expect_code_one_of "start attempt twice" 200 201 400 409
if [[ "$HTTP_CODE" == "200" ]]; then
  EXISTING_ID=$(json_get "$HTTP_BODY" '.id')
  [[ "$EXISTING_ID" == "$ATTEMPT2_ID" ]] || fail "Expected same attempt id on second start (200), got $EXISTING_ID vs $ATTEMPT2_ID"
elif [[ "$HTTP_CODE" == "201" ]]; then
  NEW_ID=$(json_get "$HTTP_BODY" '.id')
  NEW_NUM=$(json_get "$HTTP_BODY" '.attemptNumber')
  [[ "$NEW_ID" != "$ATTEMPT2_ID" ]] || fail "Expected new attempt id on second start (201), got same id=$NEW_ID"
  # If platform allows multiple attempts, attemptNumber should increase (usually 2)
  if [[ "$NEW_NUM" != "null" && -n "$NEW_NUM" ]]; then
    [[ "$NEW_NUM" -ge 2 ]] || fail "Expected attemptNumber >= 2 on second start, got $NEW_NUM"
  fi
fi
pass "Double-start attempt is handled safely"

log "Submit attempt2 with all correct -> score 3/3"
request_json POST "/api/attempts/$ATTEMPT2_ID/submit" "$STUDENT_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"answers\":[{\"questionId\":$T2Q1_ID,\"selectedOption\":2},{\"questionId\":$T2Q2_ID,\"selectedOption\":3},{\"questionId\":$T2Q3_ID,\"selectedOption\":3}]}"
expect_code 200 "submit attempt2"
S2=$(json_get "$HTTP_BODY" '.score')
M2=$(json_get "$HTTP_BODY" '.maxScore')
[[ "$S2" == "3" ]] || fail "Expected score=3, got $S2 ($HTTP_BODY)"
[[ "$M2" == "3" ]] || fail "Expected maxScore=3, got $M2 ($HTTP_BODY)"
pass "Attempt2 graded 3/3"

log "Negative: STUDENT cannot view other student's attempt (create another student in same class)"
STUDENT2_NAME="student2_$SUF"
STUDENT2_EMAIL="student2_$SUF@example.com"
STUDENT2_TG="tg2_$SUF"

request_json POST "/api/join-requests" "" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$STUDENT2_NAME\",\"email\":\"$STUDENT2_EMAIL\",\"tgId\":\"$STUDENT2_TG\",\"classCode\":\"$CLASS_CODE\"}"
expect_code 201 "create join request 2"
REQUEST2_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/classes/$CLASS_ID/join-requests/$REQUEST2_ID/approve" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "approve join request 2"
STUDENT2_ID=$(json_get "$HTTP_BODY" '.id')

# set password for student2
request_json PUT "/api/users/$STUDENT2_ID" "$ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"id\":$STUDENT2_ID,\"roleId\":$ROLE_STUDENT_ID,\"name\":\"$STUDENT2_NAME\",\"email\":\"$STUDENT2_EMAIL\",\"password\":\"$STUDENT_PASS\",\"tgId\":\"$STUDENT2_TG\"}"
expect_code 200 "set student2 password"
STUDENT2_AUTH="$STUDENT2_EMAIL:$STUDENT_PASS"

# student2 starts attempt on test2 to create another attempt record
request_json POST "/api/tests/$TEST2_ID/attempts/start" "$STUDENT2_AUTH" -H "Accept: application/json"
expect_code 201 "student2 start attempt2"
ATTEMPT2B_ID=$(json_get "$HTTP_BODY" '.id')

# student1 tries to read student2 attempt -> should be forbidden (or 404)
request_json GET "/api/attempts/$ATTEMPT2B_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student cannot view others attempt" 403 404
pass "Student cannot view other student's attempt"

log "Negative: create second methodist and ensure access isolation (methodist cannot manage чужой курс/класс)"
METHODIST2_NAME="methodist2_$SUF"
METHODIST2_EMAIL="methodist2_$SUF@example.com"
request_json POST "/api/users/admin/methodists" "$ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$METHODIST2_NAME\",\"email\":\"$METHODIST2_EMAIL\",\"password\":\"$METHODIST_PASS\"}"
expect_code 201 "create methodist2"
METHODIST2_ID=$(json_get "$HTTP_BODY" '.id')
METHODIST2_AUTH="$METHODIST2_EMAIL:$METHODIST_PASS"

# methodist2 tries to delete test from methodist1 course (should be forbidden)
request_json DELETE "/api/tests/$TEST_ID" "$METHODIST2_AUTH" -H "Accept: application/json"
expect_code_one_of "methodist2 cannot delete чужой test" 403 404
pass "Methodist isolation on tests OK"

# methodist2 tries to create lesson under чужой course (should be forbidden)
request_json POST "/api/courses/$COURSE_ID/lessons" "$METHODIST2_AUTH" \
  -H "Accept: application/json" \
  -F "title=ShouldFail_$SUF" \
  -F "description=No access" \
  -F "presentation=@$PDF_FILE;type=application/pdf"
expect_code_one_of "methodist2 cannot add lesson to чужой course" 403 404
pass "Methodist isolation on lessons OK"

log "All tests passed ✅"
echo "Users created:"
echo "  METHODIST: $METHODIST_EMAIL / $METHODIST_PASS"
echo "  TEACHER:   $TEACHER_EMAIL / $TEACHER_PASS"
echo "  STUDENT:   $STUDENT_EMAIL / $STUDENT_PASS"
echo "Course id: $COURSE_ID, Class id: $CLASS_ID, Lesson id: $LESSON_ID, Test id: $TEST_ID"
