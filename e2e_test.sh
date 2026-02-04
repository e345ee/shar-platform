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
  local json="$1"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq 'length'
  else
    python3 - <<'PY' <<<"$json"
import json,sys
obj=json.load(sys.stdin)
print(len(obj) if isinstance(obj, list) else 0)
PY
  fi
}

json_filter_first() {
  # Usage: json_filter_first "$json" "field" "value"
  local json="$1"
  local field="$2"
  local value="$3"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -c --arg f "$field" --arg v "$value" 'map(select(.[$f] == $v))[0]'
  else
    python3 - "$field" "$value" <<'PY' <<<"$json"
import json,sys
field=sys.argv[1]; value=sys.argv[2]
arr=json.load(sys.stdin)
res=None
if isinstance(arr, list):
    for it in arr:
        if isinstance(it, dict) and str(it.get(field)) == value:
            res=it; break
print('null' if res is None else json.dumps(res))
PY
  fi
}

json_filter_first2() {
  # Usage: json_filter_first2 "$json" "field1" "value1" "field2" "value2"
  local json="$1"
  local f1="$2"; local v1="$3"
  local f2="$4"; local v2="$5"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -c --arg f1 "$f1" --arg v1 "$v1" --arg f2 "$f2" --arg v2 "$v2" 'map(select(.[$f1] == $v1 and .[$f2] == $v2))[0]'
  else
    python3 - "$f1" "$v1" "$f2" "$v2" <<'PY' <<<"$json"
import json,sys
f1,v1,f2,v2=sys.argv[1],sys.argv[2],sys.argv[3],sys.argv[4]
arr=json.load(sys.stdin)
res=None
if isinstance(arr, list):
    for it in arr:
        if isinstance(it, dict) and str(it.get(f1))==v1 and str(it.get(f2))==v2:
            res=it; break
print('null' if res is None else json.dumps(res))
PY
  fi
}

float_ge() {
  # Usage: float_ge "a" "b"  => returns 0 if ok, 1 if not
  python3 - "$1" "$2" <<'PY'
import sys
a=float(sys.argv[1]); b=float(sys.argv[2])
sys.exit(0 if a>=b else 1)
PY
}

float_between() {
  # Usage: float_between "x" "lo" "hi"
  python3 - "$1" "$2" "$3" <<'PY'
import sys
x=float(sys.argv[1]); lo=float(sys.argv[2]); hi=float(sys.argv[3])
sys.exit(0 if (lo<=x<=hi) else 1)
PY
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
# 7c) ACHIEVEMENTS: create 2 achievements + verify "My achievements" page + award + class feed
# ------------------------------------------------------------

PNG_FILE="$WORKDIR/ach_photo.png"
log "Create tiny PNG for achievement photo"
cat > "$PNG_FILE.b64" <<'B64'
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X0h0cAAAAASUVORK5CYII=
B64
base64 -d "$PNG_FILE.b64" > "$PNG_FILE"
[[ -s "$PNG_FILE" ]] || fail "PNG was not created"
pass "PNG created at $PNG_FILE"

log "Create ACHIEVEMENT #1 as METHODIST"
request_json POST "/api/courses/$COURSE_ID/achievements" "$METHODIST_AUTH" \
  -H "Accept: application/json" \
  -F "title=Achievement1_$SUF" \
  -F "jokeDescription=Joke1_$SUF" \
  -F "description=Desc1_$SUF" \
  -F "photo=@$PNG_FILE;type=image/png"
expect_code 201 "create achievement 1"
ACH1_ID=$(json_get "$HTTP_BODY" '.id')
pass "Achievement1 created: id=$ACH1_ID"

log "Create ACHIEVEMENT #2 as METHODIST"
request_json POST "/api/courses/$COURSE_ID/achievements" "$METHODIST_AUTH" \
  -H "Accept: application/json" \
  -F "title=Achievement2_$SUF" \
  -F "jokeDescription=Joke2_$SUF" \
  -F "description=Desc2_$SUF" \
  -F "photo=@$PNG_FILE;type=image/png"
expect_code 201 "create achievement 2"
ACH2_ID=$(json_get "$HTTP_BODY" '.id')
pass "Achievement2 created: id=$ACH2_ID"

log "Student MY ACHIEVEMENTS PAGE before awarding: should have 0 earned and 2 recommendations"
request_json GET "/api/users/me/achievements/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "get my achievements page (before award)"
TA=$(json_get "$HTTP_BODY" '.totalAvailable')
TE=$(json_get "$HTTP_BODY" '.totalEarned')
[[ "$TA" == "2" ]] || fail "Expected totalAvailable=2, got $TA ($HTTP_BODY)"
[[ "$TE" == "0" ]] || fail "Expected totalEarned=0, got $TE ($HTTP_BODY)"
REC_CNT=$(json_get "$HTTP_BODY" '.recommendations | length')
[[ "$REC_CNT" == "2" ]] || fail "Expected recommendations length=2, got $REC_CNT ($HTTP_BODY)"
pass "My achievements page (before award) OK"

log "Teacher AWARDS achievement #1 to student"
request_json POST "/api/achievements/$ACH1_ID/award/$STUDENT_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "award achievement"
AW_AID=$(json_get "$HTTP_BODY" '.achievementId')
[[ "$AW_AID" == "$ACH1_ID" ]] || fail "Expected awarded achievementId=$ACH1_ID, got $AW_AID ($HTTP_BODY)"
pass "Achievement awarded"

log "Student MY ACHIEVEMENTS PAGE after awarding: should have 1 earned and 1 recommendation"
request_json GET "/api/users/me/achievements/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "get my achievements page (after award)"
TA=$(json_get "$HTTP_BODY" '.totalAvailable')
TE=$(json_get "$HTTP_BODY" '.totalEarned')
[[ "$TA" == "2" ]] || fail "Expected totalAvailable=2, got $TA ($HTTP_BODY)"
[[ "$TE" == "1" ]] || fail "Expected totalEarned=1, got $TE ($HTTP_BODY)"
EARN_CNT=$(json_get "$HTTP_BODY" '.earned | length')
REC_CNT=$(json_get "$HTTP_BODY" '.recommendations | length')
[[ "$EARN_CNT" == "1" ]] || fail "Expected earned length=1, got $EARN_CNT ($HTTP_BODY)"
[[ "$REC_CNT" == "1" ]] || fail "Expected recommendations length=1, got $REC_CNT ($HTTP_BODY)"
echo "$HTTP_BODY" | grep -q "achievementDescription" || fail "Expected earned item to contain achievementDescription"
pass "My achievements page (after award) OK"

log "Student CLASS ACHIEVEMENT FEED: should include awarded record"
request_json GET "/api/classes/$CLASS_ID/achievement-feed" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get class achievement feed"
FEED_CNT=$(json_get "$HTTP_BODY" 'length')
[[ "$FEED_CNT" == "1" ]] || fail "Expected feed length=1, got $FEED_CNT ($HTTP_BODY)"
F_STUDENT=$(json_get "$HTTP_BODY" '.[0].studentId')
F_ACH=$(json_get "$HTTP_BODY" '.[0].achievementId')
[[ "$F_STUDENT" == "$STUDENT_ID" ]] || fail "Expected feed studentId=$STUDENT_ID, got $F_STUDENT"
[[ "$F_ACH" == "$ACH1_ID" ]] || fail "Expected feed achievementId=$ACH1_ID, got $F_ACH"
pass "Class feed OK"
log "Update ACHIEVEMENT #2 as METHODIST (JSON update)"
request_json PUT "/api/achievements/$ACH2_ID" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Achievement2_UPDATED_$SUF\",\"jokeDescription\":\"Joke2_UPDATED_$SUF\",\"description\":\"Desc2_UPDATED_$SUF\"}"
expect_code 200 "update achievement 2"
UP_TITLE=$(json_get "$HTTP_BODY" '.title')
[[ "$UP_TITLE" == "Achievement2_UPDATED_$SUF" ]] || fail "Expected updated achievement title, got $UP_TITLE ($HTTP_BODY)"
pass "Achievement2 updated"

log "Student MY ACHIEVEMENTS PAGE after update: recommendation title should be updated"
request_json GET "/api/users/me/achievements/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "get my achievements page (after achievement update)"
TA=$(json_get "$HTTP_BODY" '.totalAvailable')
TE=$(json_get "$HTTP_BODY" '.totalEarned')
[[ "$TA" == "2" ]] || fail "Expected totalAvailable=2, got $TA ($HTTP_BODY)"
[[ "$TE" == "1" ]] || fail "Expected totalEarned=1, got $TE ($HTTP_BODY)"
REC_CNT=$(json_get "$HTTP_BODY" '.recommendations | length')
[[ "$REC_CNT" == "1" ]] || fail "Expected recommendations length=1, got $REC_CNT ($HTTP_BODY)"
REC_TITLE=$(json_get "$HTTP_BODY" '.recommendations[0].title')
[[ "$REC_TITLE" == "Achievement2_UPDATED_$SUF" ]] || fail "Expected updated recommendation title, got $REC_TITLE ($HTTP_BODY)"
pass "Achievement update is visible to student"

log "Negative: TEACHER cannot update achievement (methodist-only)"
request_json PUT "/api/achievements/$ACH1_ID" "$TEACHER_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"ShouldFail_$SUF\",\"jokeDescription\":\"x\",\"description\":\"x\"}"
expect_code_one_of "teacher update achievement forbidden" 401 403
pass "Teacher cannot update achievements"

log "Negative: STUDENT cannot delete achievement"
request_json DELETE "/api/achievements/$ACH1_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student delete achievement forbidden" 401 403
pass "Student cannot delete achievements"

log "Delete ACHIEVEMENT #2 as METHODIST"
request_json DELETE "/api/achievements/$ACH2_ID" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code_one_of "delete achievement2" 200 204
pass "Achievement2 deleted"

log "Deleted achievement should return 404 on GET"
request_json GET "/api/achievements/$ACH2_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "get deleted achievement" 404 403
pass "Deleted achievement is not accessible"

log "Student MY ACHIEVEMENTS PAGE after deletion: totalAvailable=1, totalEarned=1, recommendations=0"
request_json GET "/api/users/me/achievements/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "get my achievements page (after achievement delete)"
TA=$(json_get "$HTTP_BODY" '.totalAvailable')
TE=$(json_get "$HTTP_BODY" '.totalEarned')
[[ "$TA" == "1" ]] || fail "Expected totalAvailable=1 after delete, got $TA ($HTTP_BODY)"
[[ "$TE" == "1" ]] || fail "Expected totalEarned=1 after delete, got $TE ($HTTP_BODY)"
REC_CNT=$(json_get "$HTTP_BODY" '.recommendations | length')
[[ "$REC_CNT" == "0" ]] || fail "Expected recommendations length=0 after delete, got $REC_CNT ($HTTP_BODY)"
pass "My achievements page updated after delete"

log "List achievements by course should have 1"
request_json GET "/api/courses/$COURSE_ID/achievements" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "list achievements by course"
ACH_CNT=$(json_len "$HTTP_BODY")
[[ "$ACH_CNT" == "1" ]] || fail "Expected achievements count=1, got $ACH_CNT ($HTTP_BODY)"
pass "Course achievements list updated after delete"

log "Student CLASS ACHIEVEMENT FEED still includes awarded record after delete"
request_json GET "/api/classes/$CLASS_ID/achievement-feed" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get class achievement feed after achievement delete"
FEED_CNT=$(json_get "$HTTP_BODY" 'length')
[[ "$FEED_CNT" == "1" ]] || fail "Expected feed length=1, got $FEED_CNT ($HTTP_BODY)"
F_ACH=$(json_get "$HTTP_BODY" '.[0].achievementId')
[[ "$F_ACH" == "$ACH1_ID" ]] || fail "Expected feed achievementId=$ACH1_ID, got $F_ACH"
pass "Class feed stays correct after achievement delete"


log "Negative: Student cannot view feed of a class they are not enrolled in"
request_json POST "/api/classes" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"OtherClass_$SUF\",\"courseId\":$COURSE_ID,\"teacherId\":$TEACHER_ID}"
expect_code 201 "create other class"
OTHER_CLASS_ID=$(json_get "$HTTP_BODY" '.id')

request_json GET "/api/classes/$OTHER_CLASS_ID/achievement-feed" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student feed for чужой class forbidden" 403 404
pass "Student cannot view чужой class feed"

# ------------------------------------------------------------
# 7b) Student COURSE PAGE aggregated endpoint (initially empty)
# ------------------------------------------------------------
log "Student COURSE PAGE (before lessons/tests): should return empty lessons and empty weekly"
request_json GET "/api/student/courses/$COURSE_ID/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student course page"
echo "$HTTP_BODY" | grep -q '"course"' || fail "course page must contain course"
echo "$HTTP_BODY" | grep -q '"lessons"' || fail "course page must contain lessons"
pass "Course page endpoint works"

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

# ------------------------------------------------------------
# WEEKLY_STAR activity: create -> add questions -> publish -> assign current week
# ------------------------------------------------------------
WEEK_START=$(date -d "$(date +%F) -$(( $(date +%u) - 1 )) days" +%F)
log "Create WEEKLY_STAR activity (DRAFT) as METHODIST"
request_json POST "/api/courses/$COURSE_ID/activities" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"activityType\":\"WEEKLY_STAR\",\"title\":\"Weekly Star $SUF\",\"description\":\"Hard task\",\"topic\":\"WeekTopic\",\"deadline\":\"$DEADLINE\",\"weightMultiplier\":2}"
expect_code 201 "create weekly activity"
WEEKLY_ID=$(json_get "$HTTP_BODY" '.id')
pass "Weekly activity created: id=$WEEKLY_ID"

log "Add WEEKLY question #1 (OPEN, points=8)"
request_json POST "/api/tests/$WEEKLY_ID/questions" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"orderIndex\":1,\"questionType\":\"OPEN\",\"points\":8,\"questionText\":\"Explain your reasoning\"}"
expect_code 201 "create weekly open question"
WQ1_ID=$(json_get "$HTTP_BODY" '.id')

log "Add WEEKLY question #2 (TEXT, points=2)"
request_json POST "/api/tests/$WEEKLY_ID/questions" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"orderIndex\":2,\"questionType\":\"TEXT\",\"points\":2,\"questionText\":\"Capital of Germany?\",\"correctTextAnswer\":\"Berlin\"}"
expect_code 201 "create weekly text question"
WQ2_ID=$(json_get "$HTTP_BODY" '.id')

log "Publish WEEKLY activity: DRAFT -> READY"
request_json POST "/api/tests/$WEEKLY_ID/ready" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "publish weekly ready"

log "Assign WEEKLY activity to current week (weekStart=$WEEK_START)"
request_json POST "/api/activities/$WEEKLY_ID/assign-week" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"weekStart\":\"$WEEK_START\"}"
expect_code 200 "assign weekly"
pass "Weekly activity published and assigned"

log "Student COURSE PAGE should now include weeklyThisWeek with WEEKLY_ID"
request_json GET "/api/student/courses/$COURSE_ID/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student course page with weekly"
echo "$HTTP_BODY" | grep -q "\"weeklyThisWeek\"" || fail "course page missing weeklyThisWeek"
echo "$HTTP_BODY" | grep -q "\"id\":$WEEKLY_ID" || fail "course page does not include weekly id=$WEEKLY_ID"
pass "Course page includes weekly activity"

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

# ---------------- Weekly attempt flow ----------------
log "Student START WEEKLY attempt"
request_json POST "/api/tests/$WEEKLY_ID/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 201 "start weekly attempt"
WATT_ID=$(json_get "$HTTP_BODY" '.id')
pass "Weekly attempt started: id=$WATT_ID"

log "Student SUBMIT WEEKLY attempt (TEXT correct, OPEN pending)"
request_json POST "/api/attempts/$WATT_ID/submit" "$STUDENT_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"answers\":[{\"questionId\":$WQ1_ID,\"textAnswer\":\"Because...\"},{\"questionId\":$WQ2_ID,\"textAnswer\":\"berlin\"}]}"
expect_code 200 "submit weekly attempt"
WSTATUS=$(json_get "$HTTP_BODY" '.status')
[[ "$WSTATUS" == "SUBMITTED" ]] || fail "Expected WEEKLY attempt SUBMITTED, got $WSTATUS ($HTTP_BODY)"
pass "Weekly attempt submitted (pending manual grading)"

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
if ! echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID"; then
  fail "Expected pending list to contain weekly attemptId=$WATT_ID, got: $HTTP_BODY"
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
if ! echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID"; then
  fail "Weekly attempt should still be pending after grading homework: $HTTP_BODY"
fi
pass "Pending queue updated after grading homework"

log "Teacher grades WEEKLY OPEN question with feedback"
request_json PUT "/api/attempts/$WATT_ID/grade" "$TEACHER_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"grades\":[{\"questionId\":$WQ1_ID,\"pointsAwarded\":6,\"feedback\":\"Nice attempt\"}]}"
expect_code 200 "grade weekly open"
W_STATUS_G=$(json_get "$HTTP_BODY" '.status')
[[ "$W_STATUS_G" == "GRADED" ]] || fail "Expected weekly status GRADED, got $W_STATUS_G ($HTTP_BODY)"
pass "Weekly attempt graded"

log "Teacher PENDING attempts list should now be empty for this course"
request_json GET "/api/teachers/me/attempts/pending?courseId=$COURSE_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher pending after weekly grade"
if echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID"; then
  fail "Weekly attempt still appears in pending after grading: $HTTP_BODY"
fi
pass "Pending queue cleared after grading weekly"

log "Student GET WEEKLY attempt detail: should contain feedback and pointsAwarded"
request_json GET "/api/attempts/$WATT_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student get weekly attempt detail"
if ! echo "$HTTP_BODY" | grep -q "\"feedback\""; then
  fail "Expected feedback field in weekly attempt detail: $HTTP_BODY"
fi
if ! echo "$HTTP_BODY" | grep -q "\"pointsAwarded\""; then
  fail "Expected pointsAwarded field in weekly attempt detail: $HTTP_BODY"
fi
pass "Student sees weekly feedback and awarded points"

log "Student COURSE PAGE should include latestAttempt status for weekly and homework"
request_json GET "/api/student/courses/$COURSE_ID/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student course page with attempts"
echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID" || fail "Course page missing weekly latestAttempt"
echo "$HTTP_BODY" | grep -q "\"attemptId\":$ATTEMPT_ID" || fail "Course page missing homework latestAttempt"
pass "Course page includes latest attempt statuses"

log "Methodist COURSE results should include weekly attempt summary"
request_json GET "/api/methodist/courses/$COURSE_ID/test-attempts" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist course attempts"
echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID" || fail "Methodist results missing weekly attemptId=$WATT_ID"
pass "Methodist sees weekly attempt in course results"

log "Student COURSE PAGE should include latestAttempt for weekly (status GRADED)"
request_json GET "/api/student/courses/$COURSE_ID/page" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student course page after weekly grade"
echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID" || fail "Course page must include latestAttempt attemptId=$WATT_ID: $HTTP_BODY"
echo "$HTTP_BODY" | grep -q "\"status\":\"GRADED\"" || fail "Course page must include GRADED status: $HTTP_BODY"
pass "Course page includes graded weekly attempt status"

log "Methodist RESULTS for course should include weekly attempt summary"
request_json GET "/api/methodist/courses/$COURSE_ID/test-attempts" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist course attempts"
echo "$HTTP_BODY" | grep -q "\"attemptId\":$WATT_ID" || fail "Methodist results must include weekly attemptId=$WATT_ID: $HTTP_BODY"
pass "Methodist sees weekly attempt in results"

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

# ------------------------------------------------------------
# 17) STATISTICS by topics + overview (STUDENT / TEACHER / METHODIST) + security checks
# ------------------------------------------------------------

log "Create TEACHER #2 + CLASS #2 + STUDENT #3 (for чужой access checks)"
TEACHER2_NAME="teacher2_$SUF"
TEACHER2_EMAIL="teacher2_$SUF@example.com"
TEACHER2_PASS="Teacher2Pass1!"

request_json POST "/api/users/methodists/$METHODIST_ID/teachers" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$TEACHER2_NAME\",\"email\":\"$TEACHER2_EMAIL\",\"password\":\"$TEACHER2_PASS\"}"
expect_code 201 "create teacher2"
TEACHER2_ID=$(json_get "$HTTP_BODY" '.id')
TEACHER2_AUTH="$TEACHER2_EMAIL:$TEACHER2_PASS"
pass "Teacher2 created: id=$TEACHER2_ID"

CLASS2_NAME="Class2_$SUF"
request_json POST "/api/classes" "$METHODIST_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$CLASS2_NAME\",\"courseId\":$COURSE_ID,\"teacherId\":$TEACHER2_ID}"
expect_code 201 "create class2"
CLASS2_ID=$(json_get "$HTTP_BODY" '.id')
CLASS2_CODE=$(json_get "$HTTP_BODY" '.joinCode')
[[ ${#CLASS2_CODE} -eq 8 ]] || fail "class2 joinCode must be 8 chars, got '$CLASS2_CODE'"
pass "Class2 created: id=$CLASS2_ID joinCode=$CLASS2_CODE"

STUDENT3_NAME="student3_$SUF"
STUDENT3_EMAIL="student3_$SUF@example.com"
STUDENT3_TG="tg3_$SUF"
STUDENT3_PASS="StudentPass3!"

request_json POST "/api/join-requests" "" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$STUDENT3_NAME\",\"email\":\"$STUDENT3_EMAIL\",\"tgId\":\"$STUDENT3_TG\",\"classCode\":\"$CLASS2_CODE\"}"
expect_code 201 "create join request 3"
REQUEST3_ID=$(json_get "$HTTP_BODY" '.id')

request_json POST "/api/classes/$CLASS2_ID/join-requests/$REQUEST3_ID/approve" "$TEACHER2_AUTH" -H "Accept: application/json"
expect_code 200 "approve join request 3"
STUDENT3_ID=$(json_get "$HTTP_BODY" '.id')

request_json PUT "/api/users/$STUDENT3_ID" "$ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"id\":$STUDENT3_ID,\"roleId\":$ROLE_STUDENT_ID,\"name\":\"$STUDENT3_NAME\",\"email\":\"$STUDENT3_EMAIL\",\"password\":\"$STUDENT3_PASS\",\"tgId\":\"$STUDENT3_TG\"}"
expect_code 200 "set student3 password"
STUDENT3_AUTH="$STUDENT3_EMAIL:$STUDENT3_PASS"
pass "Student3 created: id=$STUDENT3_ID"

log "Negative: TEACHER #1 cannot view statistics for чужой class (class2)"
request_json GET "/api/teachers/me/statistics/classes/$CLASS2_ID/topics" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher1 чужой class statistics forbidden" 401 403 404
pass "Teacher1 cannot view чужой class statistics"

log "Teacher #2 can view own class statistics (may be empty)"
request_json GET "/api/teachers/me/statistics/classes/$CLASS2_ID/topics" "$TEACHER2_AUTH" -H "Accept: application/json"
expect_code 200 "teacher2 class2 statistics"
pass "Teacher2 can view own class statistics"

log "Negative: TEACHER #1 cannot view achievements of чужой student (student3)"
request_json GET "/api/students/$STUDENT3_ID/achievements" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher1 чужой student achievements forbidden" 401 403 404
pass "Teacher1 cannot view чужой student achievements"

log "METHODIST can view achievements of student3 (same course)"
request_json GET "/api/students/$STUDENT3_ID/achievements" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist view student3 achievements"
pass "Methodist can view student3 achievements"

log "Negative: TEACHER #1 cannot award achievement to student3 (not in teacher1 classes)"
request_json POST "/api/achievements/$ACH1_ID/award/$STUDENT3_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher1 award to чужой student forbidden" 401 403 404
pass "Teacher1 cannot award to чужой student"

log "TEACHER #2 awards achievement #1 to student3 (should work)"
request_json POST "/api/achievements/$ACH1_ID/award/$STUDENT3_ID" "$TEACHER2_AUTH" -H "Accept: application/json"
expect_code 200 "teacher2 award achievement to student3"
pass "Teacher2 awarded achievement to student3"

log "Student3 MY ACHIEVEMENTS PAGE: should have totalAvailable=1 and totalEarned=1 after award"
request_json GET "/api/users/me/achievements/page" "$STUDENT3_AUTH" -H "Accept: application/json"
expect_code 200 "student3 my achievements page"
TA=$(json_get "$HTTP_BODY" '.totalAvailable')
TE=$(json_get "$HTTP_BODY" '.totalEarned')
[[ "$TA" == "1" ]] || fail "Expected student3 totalAvailable=1, got $TA ($HTTP_BODY)"
[[ "$TE" == "1" ]] || fail "Expected student3 totalEarned=1, got $TE ($HTTP_BODY)"
pass "Student3 achievements page OK"

log "Create 6 READY tests with the SAME topic (to validate topic aggregation)"
SHARED_TOPIC="SharedTopic_$SUF"
declare -a SHARED_TEST_IDS=()
declare -a SHARED_Q_IDS=()
for i in 1 2 3 4 5 6; do
  request_json POST "/api/lessons/$LESSON_ID/tests" "$METHODIST_AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"SharedTest${i}_$SUF\",\"description\":\"SharedDesc${i}\",\"topic\":\"$SHARED_TOPIC\",\"deadline\":\"$DEADLINE\"}"
  expect_code 201 "create shared test $i"
  TID=$(json_get "$HTTP_BODY" '.id')
  SHARED_TEST_IDS+=("$TID")

  request_json POST "/api/tests/$TID/questions" "$METHODIST_AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"orderIndex\":1,\"questionType\":\"SINGLE_CHOICE\",\"points\":1,\"questionText\":\"Shared Q${i}: 1+0=?\",\"option1\":\"0\",\"option2\":\"1\",\"option3\":\"2\",\"option4\":\"3\",\"correctOption\":2}"
  expect_code 201 "add question to shared test $i"
  QID=$(json_get "$HTTP_BODY" '.id')
  SHARED_Q_IDS+=("$QID")

  request_json POST "/api/tests/$TID/ready" "$METHODIST_AUTH" -H "Accept: application/json"
  expect_code 200 "publish shared test $i"
done
pass "Shared-topic tests created and published: ${#SHARED_TEST_IDS[@]}"

log "Student list tests in lesson should now include original + shared tests"
request_json GET "/api/lessons/$LESSON_ID/tests" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student list lesson tests after shared tests"
LIST_AFTER=$(json_len "$HTTP_BODY")
[[ "$LIST_AFTER" -ge 7 ]] || fail "Expected >=7 tests in lesson after shared tests, got $LIST_AFTER: $HTTP_BODY"
pass "Student sees shared-topic tests"

log "Student #1 solves all 6 shared-topic tests with 100% (auto-graded)"
for idx in "${!SHARED_TEST_IDS[@]}"; do
  tid="${SHARED_TEST_IDS[$idx]}"
  qid="${SHARED_Q_IDS[$idx]}"

  request_json POST "/api/tests/$tid/attempts/start" "$STUDENT_AUTH" -H "Accept: application/json"
  expect_code_one_of "student1 start shared attempt" 200 201
  AID=$(json_get "$HTTP_BODY" '.id')

  request_json POST "/api/attempts/$AID/submit" "$STUDENT_AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"answers\":[{\"questionId\":$qid,\"selectedOption\":2}]}"
  expect_code 200 "student1 submit shared attempt"
  SS=$(json_get "$HTTP_BODY" '.score')
  MM=$(json_get "$HTTP_BODY" '.maxScore')
  [[ "$SS" == "1" ]] || fail "Expected shared score=1, got $SS ($HTTP_BODY)"
  [[ "$MM" == "1" ]] || fail "Expected shared maxScore=1, got $MM ($HTTP_BODY)"
done
pass "Student1 completed all shared-topic tests"

log "Student #2 solves first 2 shared-topic tests with 0% (to validate class avg aggregation)"
for j in 0 1; do
  tid="${SHARED_TEST_IDS[$j]}"
  qid="${SHARED_Q_IDS[$j]}"

  request_json POST "/api/tests/$tid/attempts/start" "$STUDENT2_AUTH" -H "Accept: application/json"
  expect_code_one_of "student2 start shared attempt" 200 201
  AID=$(json_get "$HTTP_BODY" '.id')

  request_json POST "/api/attempts/$AID/submit" "$STUDENT2_AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"answers\":[{\"questionId\":$qid,\"selectedOption\":1}]}"
  expect_code 200 "student2 submit shared attempt"
  SS=$(json_get "$HTTP_BODY" '.score')
  [[ "$SS" == "0" ]] || fail "Expected student2 shared score=0, got $SS ($HTTP_BODY)"
done
pass "Student2 created low-score data for shared topic"

# ------------------- STUDENT STATISTICS -------------------
log "STUDENT statistics overview (counts + course progress)"
request_json GET "/api/student/statistics/overview" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student statistics overview"
AT_TOTAL=$(json_get "$HTTP_BODY" '.attemptsTotal')
AT_IP=$(json_get "$HTTP_BODY" '.attemptsInProgress')
AT_GRADED=$(json_get "$HTTP_BODY" '.attemptsGraded')
T_GRADED=$(json_get "$HTTP_BODY" '.testsGraded')
C_STARTED=$(json_get "$HTTP_BODY" '.coursesStarted')
C_COMPLETED=$(json_get "$HTTP_BODY" '.coursesCompleted')

[[ "$C_STARTED" == "1" ]] || fail "Expected coursesStarted=1, got $C_STARTED ($HTTP_BODY)"
[[ "$C_COMPLETED" == "1" ]] || fail "Expected coursesCompleted=1, got $C_COMPLETED ($HTTP_BODY)"
[[ "$AT_IP" -ge 1 ]] || fail "Expected attemptsInProgress >= 1 (due to rejected submit), got $AT_IP ($HTTP_BODY)"
[[ "$AT_GRADED" -ge 9 ]] || fail "Expected attemptsGraded >= 9, got $AT_GRADED ($HTTP_BODY)"
[[ "$T_GRADED" -ge 9 ]] || fail "Expected testsGraded >= 9, got $T_GRADED ($HTTP_BODY)"

COURSES_JSON=$(json_get "$HTTP_BODY" '.courses')
COURSE_OBJ=$(json_filter_first "$COURSES_JSON" "courseId" "$COURSE_ID")
[[ "$COURSE_OBJ" != "null" ]] || fail "Course progress missing for courseId=$COURSE_ID: $COURSES_JSON"
REQ=$(json_get "$COURSE_OBJ" '.requiredTests')
DONE=$(json_get "$COURSE_OBJ" '.completedTests')
PCT=$(json_get "$COURSE_OBJ" '.percent')
COMPL=$(json_get "$COURSE_OBJ" '.completed')
[[ "$REQ" == "8" ]] || fail "Expected requiredTests=8 (2 existing + 6 shared), got $REQ ($COURSE_OBJ)"
[[ "$DONE" == "8" ]] || fail "Expected completedTests=8, got $DONE ($COURSE_OBJ)"
[[ "$COMPL" == "true" ]] || fail "Expected completed=true, got $COMPL ($COURSE_OBJ)"
float_ge "$PCT" "99.0" || fail "Expected percent >= 99.0, got $PCT ($COURSE_OBJ)"
pass "Student overview stats OK (course progress + attempts/tests counts)"

log "STUDENT topic stats (course filter): shared topic must be aggregated across 6 tests"
request_json GET "/api/student/statistics/topics?courseId=$COURSE_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code 200 "student topic stats"
TOP_OBJ=$(json_filter_first "$HTTP_BODY" "topic" "$SHARED_TOPIC")
[[ "$TOP_OBJ" != "null" ]] || fail "Missing shared topic stats for student: $HTTP_BODY"
TTA=$(json_get "$TOP_OBJ" '.testsAttempted')
TAC=$(json_get "$TOP_OBJ" '.attemptsCount')
AVG=$(json_get "$TOP_OBJ" '.avgBestPercent')
[[ "$TTA" == "6" ]] || fail "Expected testsAttempted=6 for shared topic, got $TTA ($TOP_OBJ)"
[[ "$TAC" == "6" ]] || fail "Expected attemptsCount=6 for shared topic, got $TAC ($TOP_OBJ)"
float_ge "$AVG" "99.9" || fail "Expected avgBestPercent ~100 for shared topic, got $AVG ($TOP_OBJ)"
pass "Student topic aggregation OK (shared topic)"

log "Negative: STUDENT cannot request stats for a course they are not enrolled in"
request_json GET "/api/student/statistics/topics?courseId=$OTHER_COURSE_ID" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student topic stats for чужой course forbidden" 401 403 404
pass "Student cannot request stats for чужой course"

# ------------------- TEACHER STATISTICS -------------------
log "TEACHER class topic stats: shared topic should aggregate across students"
request_json GET "/api/teachers/me/statistics/classes/$CLASS_ID/topics" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher class topic stats"
CT_OBJ=$(json_filter_first "$HTTP_BODY" "topic" "$SHARED_TOPIC")
[[ "$CT_OBJ" != "null" ]] || fail "Missing shared topic in class stats: $HTTP_BODY"
ST_TOT=$(json_get "$CT_OBJ" '.studentsTotal')
ST_ACT=$(json_get "$CT_OBJ" '.studentsWithActivity')
TT_ATT=$(json_get "$CT_OBJ" '.testsAttempted')
CAVG=$(json_get "$CT_OBJ" '.avgPercent')
[[ "$ST_TOT" == "2" ]] || fail "Expected studentsTotal=2, got $ST_TOT ($CT_OBJ)"
[[ "$ST_ACT" == "2" ]] || fail "Expected studentsWithActivity=2, got $ST_ACT ($CT_OBJ)"
[[ "$TT_ATT" == "8" ]] || fail "Expected testsAttempted=8 (6+2), got $TT_ATT ($CT_OBJ)"
float_between "$CAVG" "45.0" "55.0" || fail "Expected avgPercent around 50, got $CAVG ($CT_OBJ)"
pass "Teacher class topic stats OK"

log "TEACHER student topic stats: shared topic for student1 should show 6 testsAttempted"
request_json GET "/api/teachers/me/statistics/students/$STUDENT_ID/topics?courseId=$COURSE_ID" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code 200 "teacher student topic stats"
TS_OBJ=$(json_filter_first "$HTTP_BODY" "topic" "$SHARED_TOPIC")
[[ "$TS_OBJ" != "null" ]] || fail "Missing shared topic in teacher->student stats: $HTTP_BODY"
TTA=$(json_get "$TS_OBJ" '.testsAttempted')
[[ "$TTA" == "6" ]] || fail "Expected teacher->student testsAttempted=6 for shared topic, got $TTA ($TS_OBJ)"
pass "Teacher student topic stats OK"

log "Negative: STUDENT cannot access teacher statistics endpoints"
request_json GET "/api/teachers/me/statistics/classes/$CLASS_ID/topics" "$STUDENT_AUTH" -H "Accept: application/json"
expect_code_one_of "student cannot access teacher stats" 401 403
pass "Student blocked from teacher stats"

log "Negative: TEACHER cannot access methodist statistics endpoints"
request_json GET "/api/methodist/statistics/courses/$COURSE_ID/topics" "$TEACHER_AUTH" -H "Accept: application/json"
expect_code_one_of "teacher cannot access methodist stats" 401 403
pass "Teacher blocked from methodist stats"

# ------------------- METHODIST STATISTICS -------------------
log "METHODIST course topic stats: shared topic should show 3 students total (incl. student3), activity for 2"
request_json GET "/api/methodist/statistics/courses/$COURSE_ID/topics" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist course topic stats"
MT_OBJ=$(json_filter_first "$HTTP_BODY" "topic" "$SHARED_TOPIC")
[[ "$MT_OBJ" != "null" ]] || fail "Missing shared topic in methodist course stats: $HTTP_BODY"
M_ST_TOT=$(json_get "$MT_OBJ" '.studentsTotal')
M_ST_ACT=$(json_get "$MT_OBJ" '.studentsWithActivity')
M_TT_ATT=$(json_get "$MT_OBJ" '.testsAttempted')
MAVG=$(json_get "$MT_OBJ" '.avgPercent')
[[ "$M_ST_TOT" == "3" ]] || fail "Expected methodist studentsTotal=3, got $M_ST_TOT ($MT_OBJ)"
[[ "$M_ST_ACT" == "2" ]] || fail "Expected methodist studentsWithActivity=2, got $M_ST_ACT ($MT_OBJ)"
[[ "$M_TT_ATT" == "8" ]] || fail "Expected methodist testsAttempted=8, got $M_TT_ATT ($MT_OBJ)"
float_between "$MAVG" "45.0" "55.0" || fail "Expected methodist avgPercent around 50, got $MAVG ($MT_OBJ)"
pass "Methodist course stats OK"

log "METHODIST course->classes topic stats: class1(shared topic) should be present"
request_json GET "/api/methodist/statistics/courses/$COURSE_ID/classes/topics" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code 200 "methodist course classes topic stats"
MC_OBJ=$(json_filter_first2 "$HTTP_BODY" "classId" "$CLASS_ID" "topic" "$SHARED_TOPIC")
[[ "$MC_OBJ" != "null" ]] || fail "Missing class1 shared topic in methodist classes stats: $HTTP_BODY"
MC_ST_TOT=$(json_get "$MC_OBJ" '.studentsTotal')
MC_ST_ACT=$(json_get "$MC_OBJ" '.studentsWithActivity')
[[ "$MC_ST_TOT" == "2" ]] || fail "Expected class1 studentsTotal=2, got $MC_ST_TOT ($MC_OBJ)"
[[ "$MC_ST_ACT" == "2" ]] || fail "Expected class1 studentsWithActivity=2, got $MC_ST_ACT ($MC_OBJ)"
pass "Methodist course->classes stats OK"

log "Negative: METHODIST #2 cannot view statistics for чужой course"
request_json GET "/api/methodist/statistics/courses/$COURSE_ID/topics" "$METHODIST2_AUTH" -H "Accept: application/json"
expect_code_one_of "methodist2 cannot view чужой course stats" 401 403 404
pass "Methodist2 is blocked from чужой course stats"

log "Create a course by METHODIST #2 and verify METHODIST #1 cannot access it"
request_json POST "/api/courses" "$METHODIST2_AUTH" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"M2Course_$SUF\",\"description\":\"Course of methodist2\"}"
expect_code 201 "methodist2 create course"
M2_COURSE_ID=$(json_get "$HTTP_BODY" '.id')

request_json GET "/api/methodist/statistics/courses/$M2_COURSE_ID/topics" "$METHODIST2_AUTH" -H "Accept: application/json"
expect_code 200 "methodist2 own course stats (may be empty)"
pass "Methodist2 can view own course stats"

request_json GET "/api/methodist/statistics/courses/$M2_COURSE_ID/topics" "$METHODIST_AUTH" -H "Accept: application/json"
expect_code_one_of "methodist1 cannot view methodist2 course stats" 401 403 404
pass "Methodist1 cannot view чужой course stats"

log "All tests passed ✅"
echo "Users created:"
echo "  METHODIST: $METHODIST_EMAIL / $METHODIST_PASS"
echo "  TEACHER:   $TEACHER_EMAIL / $TEACHER_PASS"
echo "  STUDENT:   $STUDENT_EMAIL / $STUDENT_PASS"
echo "Course id: $COURSE_ID, Class id: $CLASS_ID, Lesson id: $LESSON_ID, Test id: $TEST_ID"
