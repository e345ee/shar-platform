
.MAIN: all

TOOLS_DIR?=.tools
ENV_FILE?=.env

MAVEN_VERSION?=3.9.6
MAVEN_HOME?=${TOOLS_DIR}/apache-maven-${MAVEN_VERSION}
MAVEN_TGZ?=${TOOLS_DIR}/apache-maven-${MAVEN_VERSION}-bin.tar.gz

MINIO_HOME?=${TOOLS_DIR}/minio
MINIO_TGZ?=${MINIO_HOME}/freebsd-amd64.tar.gz
MINIO_BIN?=${MINIO_HOME}/freebsd-amd64/minio

MC_HOME?=${TOOLS_DIR}/mc
MC_TGZ?=${MC_HOME}/freebsd-amd64.tar.gz
MC_BIN?=${MC_HOME}/freebsd-amd64/mc

PROCCTL?=proccontrol -m aslr -s disable
PYTHON?=python

MINIO_PORT?=9100
MINIO_DATA_DIR?=${HOME}/minio-data
MINIO_LOG?=${HOME}/minio.log

BACKEND_DIR?=backend
BACKEND_TARGET_DIR?=${BACKEND_DIR}/target
DIST_DIR?=dist

.PHONY: all help env tools maven-install minio-install mc-install minio-start minio-stop minio-bucket db-init db-drop backend-build backend-clean run clean

help:
	@echo "Targets:"
	@echo "  make / make all     - tools + minio + bucket policy + db init + backend build"
	@echo "  make run            - run backend (java -jar) using .env"
	@echo "  make minio-stop     - stop MinIO"
	@echo "  make db-init        - apply db/init/01_schema.sql and 02_seed.sql"
	@echo "  make db-drop        - DROP all DB objects (uses db/init/00_drop.sql)"
	@echo "  make backend-build  - build Spring Boot jar"
	@echo "  make clean          - remove local build artifacts (.tools/dist)"

all: env tools minio-start minio-bucket db-init backend-build
	@echo "\n✅ Done. Next: run 'make run' to start the backend."

env:
	@if [ ! -f "${ENV_FILE}" ]; then \
		if [ -f .env.helios ]; then \
			cp .env.helios "${ENV_FILE}"; \
			echo "Created ${ENV_FILE} from .env.helios"; \
		else \
			cp .env.example "${ENV_FILE}"; \
			echo "Created ${ENV_FILE} from .env.example"; \
		fi; \
		echo "Edit ${ENV_FILE} if you need different ports/URLs."; \
	fi

tools: maven-install minio-install mc-install

maven-install:
	@if command -v mvn >/dev/null 2>&1; then \
		echo "Using system Maven: $$(command -v mvn)"; \
	else \
		if [ -x "${MAVEN_HOME}/bin/mvn" ]; then \
			echo "Using local Maven: ${MAVEN_HOME}/bin/mvn"; \
		else \
			mkdir -p "${TOOLS_DIR}"; \
			echo "Downloading Apache Maven ${MAVEN_VERSION}..."; \
			${PYTHON} - <<'PY'\
import urllib.request\
url = "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"\
out = "${MAVEN_TGZ}"\
urllib.request.urlretrieve(url, out)\
print("Downloaded:", out)\
PY \
			tar -xzf "${MAVEN_TGZ}" -C "${TOOLS_DIR}"; \
			echo "Installed local Maven at ${MAVEN_HOME}"; \
		fi; \
	fi

minio-install:
	@if [ -x "${MINIO_BIN}" ]; then \
		echo "MinIO already present: ${MINIO_BIN}"; \
	else \
		mkdir -p "${MINIO_HOME}"; \
		echo "Downloading MinIO (FreeBSD amd64 legacy build)..."; \
		${PYTHON} - <<'PY'\
import urllib.request\
url = "https://dl.min.io/server/minio/freebsd-amd64.tar.gz"\
out = "${MINIO_TGZ}"\
urllib.request.urlretrieve(url, out)\
print("Downloaded:", out)\
PY \
		tar -xzf "${MINIO_TGZ}" -C "${MINIO_HOME}"; \
		chmod +x "${MINIO_BIN}"; \
		echo "Installed MinIO at ${MINIO_BIN}"; \
	fi

mc-install:
	@if [ -x "${MC_BIN}" ]; then \
		echo "mc already present: ${MC_BIN}"; \
	else \
		mkdir -p "${MC_HOME}"; \
		echo "Downloading MinIO client (mc) for FreeBSD amd64..."; \
		${PYTHON} - <<'PY'\
import urllib.request\
url = "https://dl.min.io/client/mc/freebsd-amd64.tar.gz"\
out = "${MC_TGZ}"\
urllib.request.urlretrieve(url, out)\
print("Downloaded:", out)\
PY \
		tar -xzf "${MC_TGZ}" -C "${MC_HOME}"; \
		chmod +x "${MC_BIN}"; \
		echo "Installed mc at ${MC_BIN}"; \
	fi

minio-start: minio-install env
	@. "${ENV_FILE}"; \
	if sockstat -4 -l | grep -q ":${MINIO_PORT} "; then \
		echo "MinIO already listening on :${MINIO_PORT}"; \
	else \
		mkdir -p "${MINIO_DATA_DIR}"; \
		rm -f "${MINIO_LOG}"; \
		export MINIO_ACCESS_KEY="$${APP_S3_ACCESS_KEY}"; \
		export MINIO_SECRET_KEY="$${APP_S3_SECRET_KEY}"; \
		echo "Starting MinIO on :${MINIO_PORT} (data: ${MINIO_DATA_DIR})..."; \
		nohup ${PROCCTL} "${MINIO_BIN}" server --address ":${MINIO_PORT}" "${MINIO_DATA_DIR}" \
			> "${MINIO_LOG}" 2>&1 & \
		sleep 1; \
		if sockstat -4 -l | grep -q ":${MINIO_PORT} "; then \
			echo "MinIO started. Log: ${MINIO_LOG}"; \
		else \
			echo "MinIO did not start. See log: ${MINIO_LOG}"; \
			exit 1; \
		fi; \
	fi

minio-stop:
	@pids=$$(pgrep -f "minio.*--address.*${MINIO_PORT}" || true); \
	if [ -n "$$pids" ]; then \
		echo "Stopping MinIO (pids: $$pids)"; \
		kill $$pids; \
	else \
		echo "MinIO is not running on :${MINIO_PORT}"; \
	fi

minio-bucket: mc-install minio-start env
	@. "${ENV_FILE}"; \
	# configure alias (ignore if already exists)
	${PROCCTL} "${MC_BIN}" config host add local "http://127.0.0.1:${MINIO_PORT}" "$${APP_S3_ACCESS_KEY}" "$${APP_S3_SECRET_KEY}" >/dev/null 2>&1 || true; \
	echo "Ensuring bucket '$${APP_S3_BUCKET}' exists..."; \
	${PROCCTL} "${MC_BIN}" mb "local/$${APP_S3_BUCKET}" >/dev/null 2>&1 || true; \
	echo "Ensuring bucket policy: public read-only (download)..."; \
	if ${PROCCTL} "${MC_BIN}" policy set download "local/$${APP_S3_BUCKET}" >/dev/null 2>&1; then \
		echo "Policy set: download"; \
	elif ${PROCCTL} "${MC_BIN}" policy download "local/$${APP_S3_BUCKET}" >/dev/null 2>&1; then \
		echo "Policy set: download"; \
	else \
		echo "ERROR: could not set public read policy for bucket '$${APP_S3_BUCKET}'."; \
		echo "Open MinIO Browser and set Bucket Policy to Read Only."; \
		exit 1; \
	fi


db-init: env
	@. "${ENV_FILE}"; \
	echo "Applying DB schema to $$PGHOST:$$PGPORT/$$PGDATABASE as $$PGUSER..."; \
	PGPASSWORD="$$PGPASSWORD" PGSSLMODE="$$PGSSLMODE" psql -v ON_ERROR_STOP=1 -h "$$PGHOST" -p "$$PGPORT" -U "$$PGUSER" -d "$$PGDATABASE" -f db/init/01_schema.sql; \
	PGPASSWORD="$$PGPASSWORD" PGSSLMODE="$$PGSSLMODE" psql -v ON_ERROR_STOP=1 -h "$$PGHOST" -p "$$PGPORT" -U "$$PGUSER" -d "$$PGDATABASE" -f db/init/02_seed.sql; \
	echo "DB init complete."

db-drop: env
	@. "${ENV_FILE}"; \
	echo "⚠️  DROPPING all DB objects in $$PGDATABASE"; \
	PGPASSWORD="$$PGPASSWORD" PGSSLMODE="$$PGSSLMODE" psql -v ON_ERROR_STOP=1 -h "$$PGHOST" -p "$$PGPORT" -U "$$PGUSER" -d "$$PGDATABASE" -f db/init/00_drop.sql

backend-build: env maven-install
	@mkdir -p "${DIST_DIR}"; \
	if command -v mvn >/dev/null 2>&1; then \
		(cd "${BACKEND_DIR}" && mvn -DskipTests package); \
	else \
		(cd "${BACKEND_DIR}" && "../${MAVEN_HOME}/bin/mvn" -DskipTests package); \
	fi; \
	# Copy built jar into ./dist/app.jar
	jar=$$(ls -1 "${BACKEND_TARGET_DIR}"/*.jar 2>/dev/null | grep -vE '(sources|javadoc)\\.jar$$' | head -n 1); \
	if [ -z "$$jar" ]; then \
		echo "Jar not found in ${BACKEND_TARGET_DIR}. Build failed."; \
		exit 1; \
	fi; \
	cp "$$jar" "${DIST_DIR}/app.jar"; \
	echo "Built: ${DIST_DIR}/app.jar"

backend-clean:
	@rm -rf "${BACKEND_DIR}/target" "${DIST_DIR}/app.jar"

run: env
	@. "${ENV_FILE}"; \
	echo "Starting backend using ${DIST_DIR}/app.jar..."; \
	if [ ! -f "${DIST_DIR}/app.jar" ]; then \
		echo "Jar not found. Run 'make backend-build' first."; \
		exit 1; \
	fi; \
	java -jar "${DIST_DIR}/app.jar"

clean:
	@rm -rf "${TOOLS_DIR}" "${DIST_DIR}"
