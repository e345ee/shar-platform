
SHELL=/bin/sh

BACKEND_DIR=backend
DIST_DIR=dist
ENV_FILE=.env


MINIO_BIN?=$(HOME)/bin/freebsd-amd64/minio
MINIO_DATA?=$(HOME)/minio-data
MINIO_ADDR?=:9100


MINIO_ACCESS_KEY?=minio
MINIO_SECRET_KEY?=supersecret123


S3_BUCKET?=course

S3_PUBLIC_URL?=http://192.168.10.80:9100


PGHOST?=localhost
PGPORT?=5432
PGDATABASE?=studs
PGUSER?=s368748

PGPASSWORD?=


PSQL=psql -h "$(PGHOST)" -p "$(PGPORT)" -U "$(PGUSER)" -d "$(PGDATABASE)"

.PHONY: all env check-tools minio-start minio-stop bucket-hint db-init backend-build run help

all: env check-tools minio-start bucket-hint db-init backend-build
	@echo ""
	@echo "Done."
	@echo "Next: env __MAKE_CONF=/dev/null make run"

help:
	@echo "Targets:"
	@echo "  make                -> env + start minio + init db + build jar"
	@echo "  make run            -> run backend (foreground)"
	@echo "  make minio-stop     -> stop minio"
	@echo "  make db-init        -> apply db/init/*.sql to Postgres"
	@echo ""
	@echo "Tip: on helios run with: env __MAKE_CONF=/dev/null make ..."

env:
	@if [ ! -f "$(ENV_FILE)" ]; then \
		if [ -f .env.helios ]; then \
			cp .env.helios "$(ENV_FILE)"; \
			echo "Created $(ENV_FILE) from .env.helios"; \
		else \
			echo "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/studs?sslmode=require" > "$(ENV_FILE)"; \
			echo "SPRING_DATASOURCE_USERNAME=$(PGUSER)" >> "$(ENV_FILE)"; \
			echo "SPRING_DATASOURCE_PASSWORD=" >> "$(ENV_FILE)"; \
			echo "APP_S3_ENDPOINT=http://127.0.0.1:9100" >> "$(ENV_FILE)"; \
			echo "APP_S3_ACCESS_KEY=$(MINIO_ACCESS_KEY)" >> "$(ENV_FILE)"; \
			echo "APP_S3_SECRET_KEY=$(MINIO_SECRET_KEY)" >> "$(ENV_FILE)"; \
			echo "APP_S3_BUCKET=$(S3_BUCKET)" >> "$(ENV_FILE)"; \
			echo "APP_S3_REGION=us-east-1" >> "$(ENV_FILE)"; \
			echo "APP_S3_PUBLIC_URL=$(S3_PUBLIC_URL)" >> "$(ENV_FILE)"; \
			echo "Created $(ENV_FILE)"; \
		fi; \
		echo "Edit $(ENV_FILE): set SPRING_DATASOURCE_PASSWORD and (optionally) APP_S3_PUBLIC_URL"; \
	fi

check-tools:
	@command -v java >/dev/null 2>&1 || { echo "java not found"; exit 1; }
	@command -v psql >/dev/null 2>&1 || { echo "psql not found"; exit 1; }
	@if ! command -v mvn >/dev/null 2>&1; then \
		echo "ERROR: mvn (Maven) not found in PATH."; \
		echo "On helios it is usually installed. If not, you can add Maven locally and rerun."; \
		exit 1; \
	fi
	@if [ ! -x "$(MINIO_BIN)" ]; then \
		echo "ERROR: MinIO binary not found/executable at: $(MINIO_BIN)"; \
		echo "You already downloaded it earlier; set MINIO_BIN=... or re-download."; \
		exit 1; \
	fi
	@command -v proccontrol >/dev/null 2>&1 || { echo "proccontrol not found"; exit 1; }

minio-start:
	@mkdir -p "$(MINIO_DATA)"
	@if pgrep -f "$(MINIO_BIN)" >/dev/null 2>&1; then \
		echo "MinIO already running."; \
	else \
		echo "Starting MinIO on $(MINIO_ADDR) ..."; \
		MINIO_ACCESS_KEY="$(MINIO_ACCESS_KEY)" MINIO_SECRET_KEY="$(MINIO_SECRET_KEY)" \
		nohup proccontrol -m aslr -s disable "$(MINIO_BIN)" \
			server --address "$(MINIO_ADDR)" "$(MINIO_DATA)" \
			> "$(HOME)/minio.log" 2>&1 & \
		echo "MinIO log: $(HOME)/minio.log"; \
	fi

minio-stop:
	@pkill -f "$(MINIO_BIN)" 2>/dev/null || true
	@echo "Stopped MinIO (if it was running)."


bucket-hint:
	@echo ""
	@echo "MinIO bucket:"
	@echo "  - Ensure bucket '$(S3_BUCKET)' exists"
	@echo "  - Set bucket policy to READ ONLY (public download), иначе будет AccessDenied"
	@echo "  - Browser: http://127.0.0.1:9100 (через ssh -L 9100:localhost:9100)"
	@echo ""

db-init:
	@echo "Initializing DB schema + seed in Postgres ($(PGDATABASE))..."
	@set -e; \
	if [ -f db/init/01_schema.sql ]; then \
		PGPASSWORD="$(PGPASSWORD)" $(PSQL) -f db/init/01_schema.sql; \
	else \
		echo "Missing db/init/01_schema.sql"; exit 1; \
	fi; \
	if [ -f db/init/02_seed.sql ]; then \
		PGPASSWORD="$(PGPASSWORD)" $(PSQL) -f db/init/02_seed.sql; \
	fi
	@echo "DB init done."

backend-build:
	@echo "Building Spring Boot backend..."
	@mkdir -p "$(DIST_DIR)"
	@cd "$(BACKEND_DIR)" && mvn -DskipTests package
	@jar=`ls -1 "$(BACKEND_DIR)/target"/*.jar 2>/dev/null | grep -vE '(sources|javadoc)\.jar$$' | head -n 1`; \
	if [ -z "$$jar" ]; then echo "Jar not found in $(BACKEND_DIR)/target"; exit 1; fi; \
	cp "$$jar" "$(DIST_DIR)/app.jar"; \
	echo "Built: $(DIST_DIR)/app.jar"

run: env
	@if [ ! -f "$(DIST_DIR)/app.jar" ]; then \
		echo "Jar not found. Run 'make backend-build' first."; \
		exit 1; \
	fi
	@echo "Starting backend on port 8080 (default Spring Boot)."
	@echo "Tip: ssh -L 8080:localhost:8080 s368748@helios"
	@set -a; . "./$(ENV_FILE)"; set +a; \
	java -jar "$(DIST_DIR)/app.jar"
