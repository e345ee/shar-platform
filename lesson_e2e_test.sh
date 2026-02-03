#!/usr/bin/env bash
set -euo pipefail

# ------------------------------------------------------------
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
fail() { echo "\n[FAIL] $*" >&2; exit 1; }
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
    # Minimal jq-like evaluator for common cases: .a, .a.b, .a[0].b
    python3 - "$expr" <<'PY' <<<"$json"
import json,sys,re
expr=sys.argv[1].strip()
obj=json.load(sys.stdin)
if expr.startswith('.'): expr=expr[1:]
if expr=='':
    print(json.dumps(obj))
    sys.exit(0)
# split by '.' but keep [idx] parts
parts=[]
buf=''
for ch in expr:
    if ch=='.':
        if buf: parts.append(buf); buf=''
        continue
    buf+=ch
if buf: parts.append(buf)
for p in parts:
    # handle root indexes like [0] or [0][1]
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
        # allow numeric keys when current is list
        obj=obj[int(key)]
    idx_part=m.group(2)
    if idx_part:
        for im in re.finditer(r'\[(\d+)\]', idx_part):
            obj=obj[int(im.group(1))]
# print scalar
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

# ------------------------------------------------------------
# 0) Health
# ------------------------------------------------------------
log "Health check"
request_json GET "/actuator/health" "" \
  -H "Accept: application/json"
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

# Expect first lesson in list is B (orderIndex=1), second is A (orderIndex=2)
FIRST_ID=$(json_get "$HTTP_BODY" '.[0].id')
FIRST_ORDER=$(json_get "$HTTP_BODY" '.[0].orderIndex')
SECOND_ID=$(json_get "$HTTP_BODY" '.[1].id')
SECOND_ORDER=$(json_get "$HTTP_BODY" '.[1].orderIndex')

[[ "$FIRST_ID" == "$LESSON_B_ID" && "$FIRST_ORDER" == "1" ]] || fail "Expected first lesson to be B with orderIndex=1"
[[ "$SECOND_ID" == "$LESSON_A_ID" && "$SECOND_ORDER" == "2" ]] || fail "Expected second lesson to be A with orderIndex=2"
pass "Lesson ordering is correct"

# We'll use LESSON_B for slide paging tests
LESSON_ID="$LESSON_B_ID"

# ------------------------------------------------------------
# 11) Access tests: METHODIST / TEACHER / STUDENT can view lesson in course
# ------------------------------------------------------------
log "Access: METHODIST can GET lesson"
request_json GET "/api/lessons/$LESSON_ID" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist get lesson"
pass "Methodist can view lesson"

log "Access: TEACHER can GET lesson"
request_json GET "/api/lessons/$LESSON_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher get lesson"
pass "Teacher can view lesson"

log "Access: STUDENT (in class) can GET lesson"
request_json GET "/api/lessons/$LESSON_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get lesson"
pass "Student can view lesson (belongs to course)"

# ------------------------------------------------------------
# 12) Negative access: STUDENT cannot view lesson from another course
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
# 13) PDF paging: info + pages 1..2 for METHODIST / TEACHER / STUDENT
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

log "All tests passed ✅"
echo "Users created:"
echo "  METHODIST: $METHODIST_EMAIL / $METHODIST_PASS"
echo "  TEACHER:   $TEACHER_EMAIL / $TEACHER_PASS"
echo "  STUDENT:   $STUDENT_EMAIL / $STUDENT_PASS"
echo "Course id: $COURSE_ID, Class id: $CLASS_ID, Lesson id: $LESSON_ID"
