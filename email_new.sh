#!/usr/bin/env bash
set -euo pipefail

# Prefer python3 if available, otherwise fall back to python
PY="python3"
command -v python3 >/dev/null 2>&1 || PY="python"
command -v "$PY" >/dev/null 2>&1 || { echo "[ERROR] Need python (python3 or python)" >&2; exit 1; }

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"


TARGET_STUDENT_EMAIL="gsad1030@gmail.com"


: "${APP_MAIL_ENABLED:?APP_MAIL_ENABLED must be set (true/false)}"
if [[ "${APP_MAIL_ENABLED}" != "true" ]]; then
  echo "[ERROR] APP_MAIL_ENABLED is not 'true'. Set it to true to actually send email." >&2
  exit 1
fi


json_get() {
  local json="$1"
  local expr="$2"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -r "$expr"
    return 0
  fi
  "$PY" - "$expr" <<'PYCODE' <<<"$json"
import json,sys
expr=sys.argv[1].strip()
obj=json.load(sys.stdin)
if expr.startswith("."):
    expr=expr[1:]
if not expr:
    print("")
    raise SystemExit(0)
cur=obj
for part in expr.split("."):
    if isinstance(cur, dict):
        cur=cur.get(part)
    else:
        cur=None
        break
if cur is None:
    print("")
elif isinstance(cur,(dict,list)):
    print(json.dumps(cur, ensure_ascii=False))
else:
    print(cur)
PYCODE
}

request() {
  local method="$1"; shift
  local url="$1"; shift
  local token="$1"; shift
  local data="${1:-}"; shift || true

  local auth=()
  if [[ -n "$token" ]]; then
    auth=(-H "Authorization: Bearer $token")
  fi

  echo
  echo "---- HTTP ----"
  echo ">>> $method $BASE_URL$url"
  if [[ -n "$token" ]]; then
    echo ">>> Authorization: Bearer <redacted>"
  else
    echo ">>> Authorization: <none>"
  fi
  if [[ -n "$data" && "$data" != "{}" ]]; then
    echo ">>> Request body:"
    if command -v jq >/dev/null 2>&1; then
      echo "$data" | jq . 2>/dev/null || echo "$data"
    else
      "$PY" -m json.tool <<<"$data" 2>/dev/null || echo "$data"
    fi
  fi

  if [[ "$method" == "GET" ]]; then
    HTTP_BODY=$(curl -sS -w "\n%{http_code}" "${auth[@]}" -H "Accept: application/json" "$BASE_URL$url")
  else
    HTTP_BODY=$(curl -sS -w "\n%{http_code}" "${auth[@]}" -H "Accept: application/json" -H "Content-Type: application/json" -X "$method" "$BASE_URL$url" -d "$data")
  fi

  HTTP_CODE=$(echo "$HTTP_BODY" | tail -n 1)
  HTTP_BODY=$(echo "$HTTP_BODY" | sed '$d')

  echo "<<< HTTP $HTTP_CODE"
  if [[ -z "$HTTP_BODY" ]]; then
    echo "<<< (empty body)"
  elif [[ "$HTTP_BODY" =~ ^[[:space:]]*\{ || "$HTTP_BODY" =~ ^[[:space:]]*\[ ]]; then
    if command -v jq >/dev/null 2>&1; then
      echo "$HTTP_BODY" | jq . || echo "$HTTP_BODY"
    else
      "$PY" -m json.tool <<<"$HTTP_BODY" 2>/dev/null || echo "$HTTP_BODY"
    fi
  else
    echo "$HTTP_BODY"
  fi
  echo "-------------"
}


expect() {
  local code="$1"; shift
  local msg="$*"
  if [[ "$HTTP_CODE" != "$code" ]]; then
    echo "[FAIL] $msg (expected $code got $HTTP_CODE)" >&2
    echo "$HTTP_BODY" >&2
    exit 1
  fi
}

wait_health() {
  echo "[INFO] Waiting for backend at $BASE_URL/actuator/health ..."
  for i in {1..60}; do
    if curl -sS "$BASE_URL/actuator/health" >/dev/null 2>&1; then
      echo "[OK] Backend is up"
      return 0
    fi
    sleep 2
  done
  echo "[ERROR] Backend did not become healthy" >&2
  exit 1
}

SUF="$(date +%Y%m%d%H%M%S)-$RANDOM"
METHODIST_PASS="MethodistPass1!"
TEACHER_PASS="TeacherPass1!"
STUDENT_PASS="StudentPass1!"

METHODIST_NAME="methodist_$SUF"
METHODIST_EMAIL="methodist_$SUF@example.com"
TEACHER_NAME="teacher_$SUF"
TEACHER_EMAIL="teacher_$SUF@example.com"
STUDENT_NAME="student_$SUF"
STUDENT_TG="tg_$SUF"

COURSE_NAME="Course_$SUF"
CLASS_NAME="Class_$SUF"

echo "[INFO] Resetting docker environment (down -v)"
docker compose down -v >/dev/null 2>&1 || true

echo "[INFO] Starting services (postgres, minio, app)"
docker compose up -d --build postgres minio minio_init app

wait_health


request POST "/api/auth/login" "" "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}"
expect 200 "admin login"
ADMIN_JWT=$(json_get "$HTTP_BODY" '.accessToken')


request POST "/api/users/methodists" "$ADMIN_JWT" "{\"name\":\"$METHODIST_NAME\",\"email\":\"$METHODIST_EMAIL\",\"password\":\"$METHODIST_PASS\"}"
expect 201 "create methodist"
METHODIST_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/auth/login" "" "{\"username\":\"$METHODIST_EMAIL\",\"password\":\"$METHODIST_PASS\"}"
expect 200 "methodist login"
METHODIST_JWT=$(json_get "$HTTP_BODY" '.accessToken')


request POST "/api/users/teachers" "$METHODIST_JWT" "{\"name\":\"$TEACHER_NAME\",\"email\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASS\"}"
expect 201 "create teacher"
TEACHER_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/auth/login" "" "{\"username\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASS\"}"
expect 200 "teacher login"
TEACHER_JWT=$(json_get "$HTTP_BODY" '.accessToken')


request POST "/api/courses" "$METHODIST_JWT" "{\"name\":\"$COURSE_NAME\",\"description\":\"Email test course\"}"
expect 201 "create course"
COURSE_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/classes" "$METHODIST_JWT" "{\"name\":\"$CLASS_NAME\",\"courseId\":$COURSE_ID,\"teacherId\":$TEACHER_ID}"
expect 201 "create class"
CLASS_ID=$(json_get "$HTTP_BODY" '.id')
CLASS_CODE=$(json_get "$HTTP_BODY" '.joinCode')


request POST "/api/join-requests" "" "{\"name\":\"$STUDENT_NAME\",\"email\":\"$TARGET_STUDENT_EMAIL\",\"tgId\":\"$STUDENT_TG\",\"classCode\":\"$CLASS_CODE\"}"
expect 201 "create join request"
REQUEST_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/join-requests/$REQUEST_ID/approve?classId=$CLASS_ID" "$TEACHER_JWT" "{}"
expect 200 "approve join request"
STUDENT_ID=$(json_get "$HTTP_BODY" '.id')


request GET "/api/roles/name/STUDENT" "$ADMIN_JWT"
expect 200 "get student role"
ROLE_STUDENT_ID=$(json_get "$HTTP_BODY" '.id')

request PUT "/api/users/$STUDENT_ID" "$ADMIN_JWT" "{\"id\":$STUDENT_ID,\"roleId\":$ROLE_STUDENT_ID,\"name\":\"$STUDENT_NAME\",\"email\":\"$TARGET_STUDENT_EMAIL\",\"password\":\"$STUDENT_PASS\",\"tgId\":\"$STUDENT_TG\"}"
expect 200 "set student password"


request POST "/api/auth/login" "" "{\"username\":\"$TARGET_STUDENT_EMAIL\",\"password\":\"$STUDENT_PASS\"}"
expect 200 "student login"
STUDENT_JWT=$(json_get "$HTTP_BODY" '.accessToken')


DEADLINE="$("$PY" - <<\'PY\'
from datetime import datetime,timedelta
print((datetime.utcnow()+timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%S"))
PY
)"
request POST "/api/courses/$COURSE_ID/activities" "$METHODIST_JWT" "{\"activityType\":\"CONTROL_WORK\",\"title\":\"Control_$SUF\",\"description\":\"Simple control\",\"topic\":\"Topic_$SUF\",\"deadline\":\"$DEADLINE\",\"weightMultiplier\":1,\"timeLimitSeconds\":120}"
expect 201 "create control work"
ACTIVITY_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/activities/$ACTIVITY_ID/questions" "$METHODIST_JWT" "{\"orderIndex\":1,\"questionType\":\"SINGLE_CHOICE\",\"points\":2,\"questionText\":\"2+2=?\",\"option1\":\"3\",\"option2\":\"4\",\"option3\":\"5\",\"option4\":\"22\",\"correctOption\":2}"
expect 201 "add question"
Q_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/activities/$ACTIVITY_ID/publish" "$METHODIST_JWT" "{}"
expect 200 "publish activity"


request POST "/api/classes/$CLASS_ID/activities/$ACTIVITY_ID/open" "$TEACHER_JWT" "{}"
if [[ "$HTTP_CODE" != "200" && "$HTTP_CODE" != "201" && "$HTTP_CODE" != "204" ]]; then
  echo "[FAIL] open test (expected 200/201/204 got $HTTP_CODE)" >&2
  echo "$HTTP_BODY" >&2
  exit 1
fi


request POST "/api/activities/$ACTIVITY_ID/attempts" "$STUDENT_JWT" "{}"
expect 201 "start attempt"
ATTEMPT_ID=$(json_get "$HTTP_BODY" '.id')


request POST "/api/attempts/$ATTEMPT_ID/submit" "$STUDENT_JWT" "{\"answers\":[{\"questionId\":$Q_ID,\"selectedOption\":2}]}"
expect 200 "submit attempt"


request POST "/api/classes/$CLASS_ID/students/$STUDENT_ID/close-course" "$TEACHER_JWT" "{}"
expect 200 "close course"


request POST "/api/me/courses/$COURSE_ID/completion-email" "$STUDENT_JWT" "{}"
expect 200 "send completion email"

echo "[OK] Completion email requested. Check inbox of $TARGET_STUDENT_EMAIL."
