#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# Config
# -----------------------------
BASE_URL="${BASE_URL:-http://localhost:8080}"
PROJECT_DIR="${PROJECT_DIR:-$(pwd)}"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker-compose.yml}"
SKIP_COMPOSE="${SKIP_COMPOSE:-0}"

ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"

# -----------------------------
# Helpers
# -----------------------------
need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

hr() { echo "------------------------------------------------------------"; }

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

curl_code() {
  # args: <method> <url> [curl extra args...]
  local method="$1"; shift
  local url="$1"; shift
  local out="$TMP_DIR/resp.json"
  local code
  code=$(curl -sS -o "$out" -w "%{http_code}" -X "$method" "$url" "$@" || true)
  echo "$code"
}

resp_body() {
  cat "$TMP_DIR/resp.json"
}

expect_code() {
  local expected="$1"; shift
  local got="$1"; shift
  local desc="$1"; shift || true

  if [[ "$got" != "$expected" ]]; then
    echo "❌ FAIL: $desc (expected HTTP $expected, got $got)" >&2
    echo "Response body:" >&2
    resp_body >&2
    exit 1
  fi
  echo "✅ OK: $desc (HTTP $got)"
}

json_post() {
  local user="$1" pass="$2" path="$3" body="$4"
  curl -sS -u "$user:$pass" -H "Accept: application/json" -H "Content-Type: application/json" \
    -d "$body" "$BASE_URL$path"
}

json_put_code() {
  local user="$1" pass="$2" path="$3" body="$4"
  curl_code PUT "$BASE_URL$path" -u "$user:$pass" -H "Accept: application/json" -H "Content-Type: application/json" -d "$body"
}

json_post_code() {
  local user="$1" pass="$2" path="$3" body="$4"
  curl_code POST "$BASE_URL$path" -u "$user:$pass" -H "Accept: application/json" -H "Content-Type: application/json" -d "$body"
}

get_code() {
  local user="$1" pass="$2" path="$3"
  curl_code GET "$BASE_URL$path" -u "$user:$pass" -H "Accept: application/json"
}

delete_code() {
  local user="$1" pass="$2" path="$3"
  curl_code DELETE "$BASE_URL$path" -u "$user:$pass" -H "Accept: application/json"
}

# -----------------------------
# Pre-flight
# -----------------------------
need curl
need jq
need docker

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "docker-compose.yml not found. Set PROJECT_DIR or COMPOSE_FILE." >&2
  echo "Current PROJECT_DIR=$PROJECT_DIR" >&2
  exit 1
fi

if [[ "$SKIP_COMPOSE" != "1" ]]; then
  hr
  echo "Starting docker compose..."
  (cd "$PROJECT_DIR" && docker compose up -d --build postgres minio minio_init app)
fi

hr
echo "Waiting for backend health: $BASE_URL/actuator/health"
for i in {1..60}; do
  if curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    echo "Backend is UP"
    break
  fi
  sleep 2
  if [[ "$i" == "60" ]]; then
    echo "Backend did not become healthy. Check logs: docker compose logs app" >&2
    exit 1
  fi
done

# -----------------------------
# IDs / test data
# -----------------------------
RUN_ID="$(date +%s)"
M1_NAME="methodist_${RUN_ID}_1"
M1_EMAIL="${M1_NAME}@example.com"
M1_PASS="mPass_${RUN_ID}_1"

M2_NAME="methodist_${RUN_ID}_2"
M2_EMAIL="${M2_NAME}@example.com"
M2_PASS="mPass_${RUN_ID}_2"

T1_NAME="teacher_${RUN_ID}_1"
T1_EMAIL="${T1_NAME}@example.com"
T1_PASS="tPass_${RUN_ID}_1"

STUDENT_NAME="student_${RUN_ID}_1"
STUDENT_EMAIL="${STUDENT_NAME}@example.com"

COURSE_NAME="Course ${RUN_ID}"
CLASS_NAME="Class ${RUN_ID}"

# tiny 1x1 pngs for upload
PHOTO1="$TMP_DIR/photo1.png"
PHOTO2="$TMP_DIR/photo2.png"
BASE64_DECODE_FLAG="-d"
if ! echo "Zg==" | base64 -d >/dev/null 2>&1; then
  BASE64_DECODE_FLAG="-D"
fi
echo 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X6srsAAAAASUVORK5CYII=' | base64 $BASE64_DECODE_FLAG > "$PHOTO1"
echo 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8z8AAAAMBAQAY2y3gAAAAAElFTkSuQmCC' | base64 $BASE64_DECODE_FLAG > "$PHOTO2" 2>/dev/null || cp "$PHOTO1" "$PHOTO2"

# -----------------------------
# 1) Authorization: METHODIST can manage only own teachers
# -----------------------------
hr
echo "1) Creating 2 methodists as ADMIN..."

body_m1=$(jq -nc --arg name "$M1_NAME" --arg email "$M1_EMAIL" --arg pass "$M1_PASS" '{name:$name,email:$email,password:$pass}')
code=$(json_post_code "$ADMIN_USER" "$ADMIN_PASS" "/api/users/admin/methodists" "$body_m1")
expect_code 201 "$code" "ADMIN creates Methodist #1"
M1_ID=$(resp_body | jq -r '.id')

body_m2=$(jq -nc --arg name "$M2_NAME" --arg email "$M2_EMAIL" --arg pass "$M2_PASS" '{name:$name,email:$email,password:$pass}')
code=$(json_post_code "$ADMIN_USER" "$ADMIN_PASS" "/api/users/admin/methodists" "$body_m2")
expect_code 201 "$code" "ADMIN creates Methodist #2"
M2_ID=$(resp_body | jq -r '.id')

echo "Methodist1 id=$M1_ID, Methodist2 id=$M2_ID"

hr
echo "Creating TEACHER under Methodist #1 (valid)..."
body_t1=$(jq -nc --arg name "$T1_NAME" --arg email "$T1_EMAIL" --arg pass "$T1_PASS" '{name:$name,email:$email,password:$pass}')
code=$(json_post_code "$M1_NAME" "$M1_PASS" "/api/users/methodists/$M1_ID/teachers" "$body_t1")
expect_code 201 "$code" "METHODIST#1 creates TEACHER"
T1_ID=$(resp_body | jq -r '.id')
echo "Teacher id=$T1_ID"

hr
echo "Negative test: Methodist #2 tries to create TEACHER under Methodist #1 (must be forbidden)..."
body_t_bad=$(jq -nc --arg name "bad_teacher_${RUN_ID}" --arg email "bad_teacher_${RUN_ID}@example.com" --arg pass "pass" '{name:$name,email:$email,password:$pass}')
code=$(json_post_code "$M2_NAME" "$M2_PASS" "/api/users/methodists/$M1_ID/teachers" "$body_t_bad")
expect_code 403 "$code" "METHODIST#2 cannot manage teachers of METHODIST#1"

# -----------------------------
# 2) Create course + class
# -----------------------------
hr
echo "2) Creating course + class as METHODIST#1..."
body_course=$(jq -nc --arg name "$COURSE_NAME" --arg desc "Demo course for run $RUN_ID" '{name:$name,description:$desc}')
code=$(json_post_code "$M1_NAME" "$M1_PASS" "/api/courses" "$body_course")
expect_code 201 "$code" "METHODIST#1 creates course"
COURSE_ID=$(resp_body | jq -r '.id')
echo "Course id=$COURSE_ID"

body_class=$(jq -nc --arg name "$CLASS_NAME" --argjson courseId "$COURSE_ID" --argjson teacherId "$T1_ID" '{name:$name,courseId:$courseId,teacherId:$teacherId}')
code=$(json_post_code "$M1_NAME" "$M1_PASS" "/api/classes" "$body_class")
expect_code 201 "$code" "METHODIST#1 creates class (assigned to TEACHER#1)"
CLASS_ID=$(resp_body | jq -r '.id')
JOIN_CODE=$(resp_body | jq -r '.joinCode')
echo "Class id=$CLASS_ID, joinCode=$JOIN_CODE"

# -----------------------------
# 3) Create achievement (with S3 photo), edit, replace photo
# -----------------------------
hr
echo "3) Creating achievement in course (multipart + S3 photo)..."
code=$(curl_code POST "$BASE_URL/api/courses/$COURSE_ID/achievements" \
  -u "$M1_NAME:$M1_PASS" \
  -H "Accept: application/json" \
  -F "title=Legendary Helper" \
  -F "jokeDescription=Saved the teacher from a bug\!" \
  -F "description=Given for helping classmates with difficult tasks" \
  -F "photo=@${PHOTO1};type=image/png")
expect_code 201 "$code" "METHODIST#1 creates achievement"
ACH_ID=$(resp_body | jq -r '.id')
ACH_PHOTO_URL=$(resp_body | jq -r '.photoUrl')
echo "Achievement id=$ACH_ID"
echo "Photo url: $ACH_PHOTO_URL"

hr
echo "Editing achievement descriptions via JSON..."
body_ach_upd=$(jq -nc --arg title "Legendary Helper (v2)" --arg joke "Saved the teacher twice" --arg desc "Given for consistent help to classmates" '{title:$title,jokeDescription:$joke,description:$desc}')
code=$(json_put_code "$M1_NAME" "$M1_PASS" "/api/achievements/$ACH_ID" "$body_ach_upd")
expect_code 200 "$code" "METHODIST#1 updates achievement (JSON)"

hr
echo "Replacing ONLY photo via /photo..."
code=$(curl_code PUT "$BASE_URL/api/achievements/$ACH_ID/photo" \
  -u "$M1_NAME:$M1_PASS" \
  -H "Accept: application/json" \
  -F "photo=@${PHOTO2};type=image/png")
expect_code 200 "$code" "METHODIST#1 replaces achievement photo"
NEW_PHOTO_URL=$(resp_body | jq -r '.photoUrl')
if [[ "$NEW_PHOTO_URL" == "$ACH_PHOTO_URL" ]]; then
  echo "❌ FAIL: photoUrl did not change after replace" >&2
  exit 1
fi
echo "✅ OK: photoUrl changed"

hr
echo "Negative test: Methodist #2 tries to create achievement in course of Methodist #1 (must be forbidden)..."
code=$(curl_code POST "$BASE_URL/api/courses/$COURSE_ID/achievements" \
  -u "$M2_NAME:$M2_PASS" \
  -H "Accept: application/json" \
  -F "title=Hack" \
  -F "jokeDescription=Nope" \
  -F "description=Nope" \
  -F "photo=@${PHOTO1};type=image/png")
expect_code 403 "$code" "METHODIST#2 cannot create achievements in чужом курсе"

# -----------------------------
# 4) Create a student via join-request + approve; award achievement
# -----------------------------
hr
echo "4) Creating join request (no auth) + approving as TEACHER..."
body_join=$(jq -nc --arg name "$STUDENT_NAME" --arg email "$STUDENT_EMAIL" --arg tg "tg_${RUN_ID}" --arg code "$JOIN_CODE" '{name:$name,email:$email,tgId:$tg,classCode:$code}')
code=$(curl_code POST "$BASE_URL/api/join-requests" -H "Accept: application/json" -H "Content-Type: application/json" -d "$body_join")
expect_code 201 "$code" "Create join request"
REQ_ID=$(resp_body | jq -r '.id')
echo "JoinRequest id=$REQ_ID"

code=$(curl_code POST "$BASE_URL/api/classes/$CLASS_ID/join-requests/$REQ_ID/approve" -u "$T1_NAME:$T1_PASS" -H "Accept: application/json")
expect_code 200 "$code" "TEACHER approves join request (creates student + enrolls)"
STUDENT_ID=$(resp_body | jq -r '.id')
echo "Student id=$STUDENT_ID"

hr
echo "Awarding achievement to the student as TEACHER..."
code=$(curl_code POST "$BASE_URL/api/achievements/$ACH_ID/award/$STUDENT_ID" -u "$T1_NAME:$T1_PASS" -H "Accept: application/json")
expect_code 200 "$code" "TEACHER awards achievement"
AWARD_ID=$(resp_body | jq -r '.id')
AWARDED_AT=$(resp_body | jq -r '.awardedAt')
echo "Award record id=$AWARD_ID, awardedAt=$AWARDED_AT"

hr
echo "Viewing achievements of a specific child (TEACHER endpoint)..."
code=$(get_code "$T1_NAME" "$T1_PASS" "/api/students/$STUDENT_ID/achievements")
expect_code 200 "$code" "TEACHER views student's achievements"
count=$(resp_body | jq 'length')
if [[ "$count" -lt 1 ]]; then
  echo "❌ FAIL: expected at least 1 achievement for student" >&2
  resp_body >&2
  exit 1
fi
found=$(resp_body | jq -r --argjson aid "$ACH_ID" 'map(select(.achievementId == $aid)) | length')
if [[ "$found" != "1" ]]; then
  echo "❌ FAIL: expected awarded achievementId=$ACH_ID to appear in list" >&2
  resp_body >&2
  exit 1
fi
echo "✅ OK: awarded achievement is visible in child's achievements list"

# -----------------------------
# 5) Password change (per-user) demo
# -----------------------------
hr
echo "5) Password change demo (TEACHER)..."

code=$(get_code "$T1_NAME" "$T1_PASS" "/api/users/me")
expect_code 200 "$code" "TEACHER can authenticate (before password change)"

NEW_T1_PASS="${T1_PASS}_NEW"
body_pw=$(jq -nc --arg cur "$T1_PASS" --arg nw "$NEW_T1_PASS" '{currentPassword:$cur,newPassword:$nw}')
code=$(json_put_code "$T1_NAME" "$T1_PASS" "/api/users/me/password" "$body_pw")
expect_code 204 "$code" "TEACHER changes own password"

code=$(get_code "$T1_NAME" "$T1_PASS" "/api/users/me")
expect_code 401 "$code" "Old password no longer works"

code=$(get_code "$T1_NAME" "$NEW_T1_PASS" "/api/users/me")
expect_code 200 "$code" "New password works"

# -----------------------------
# 6) Delete achievement as METHODIST
# -----------------------------
hr
echo "6) Deleting achievement as METHODIST..."
code=$(delete_code "$M1_NAME" "$M1_PASS" "/api/achievements/$ACH_ID")
expect_code 204 "$code" "METHODIST deletes achievement"

code=$(get_code "$M1_NAME" "$M1_PASS" "/api/achievements/$ACH_ID")
expect_code 404 "$code" "Deleted achievement is not found"

code=$(get_code "$T1_NAME" "$NEW_T1_PASS" "/api/students/$STUDENT_ID/achievements")
expect_code 200 "$code" "Student achievements list still accessible"
count_after=$(resp_body | jq 'length')
if [[ "$count_after" != "0" ]]; then
  echo "❌ FAIL: expected 0 achievements after achievement deletion (cascade)" >&2
  resp_body >&2
  exit 1
fi
echo "✅ OK: cascade delete removed awarded achievement records"

hr
echo "ALL CHECKS PASSED ✅"

