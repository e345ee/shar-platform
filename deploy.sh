#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ENV_FILE="${ENV_FILE:-${ROOT_DIR}/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-${ROOT_DIR}/compose.prod.yml}"

usage() {
	cat <<'EOF'
Usage:
	bash deploy.sh up
	bash deploy.sh issue-le DOMAIN=e345ee.ru [CERTBOT_EMAIL=you@example.com]
	bash deploy.sh sync-le DOMAIN=e345ee.ru
	bash deploy.sh restart

Defaults:
	ENV_FILE=.env.prod
	COMPOSE_FILE=compose.prod.yml

Notes:
	- Run on the SERVER.
	- Ensure ports 80/443 are open and DNS points domain to this server.
EOF
}

require_cmd() {
	local cmd="$1"
	if ! command -v "$cmd" >/dev/null 2>&1; then
		echo "ERROR: missing command: $cmd" >&2
		exit 1
	fi
}

load_env() {
	if [[ ! -f "${ENV_FILE}" ]]; then
		echo "ERROR: env file not found: ${ENV_FILE}" >&2
		echo "Create it from .env.prod.example (see README)." >&2
		exit 1
	fi


	set -a
	source "${ENV_FILE}"
	set +a
}

ensure_tls_files() {
	local tls_dir="${TLS_CERTS_DIR:?TLS_CERTS_DIR is required}"
	mkdir -p "$tls_dir"

	if [[ -f "$tls_dir/fullchain.pem" && -f "$tls_dir/privkey.pem" ]]; then
		return 0
	fi

	echo "TLS cert not found in $tls_dir; creating temporary self-signed (1 day)" >&2

	if command -v openssl >/dev/null 2>&1; then
		openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
			-keyout "$tls_dir/privkey.pem" \
			-out "$tls_dir/fullchain.pem" \
			-subj "/CN=${DOMAIN:-e345ee.ru}"
	else
		require_cmd docker
		docker run --rm -v "$tls_dir:/out" alpine:3.20 sh -euc '
			apk add --no-cache openssl >/dev/null
			openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
				-keyout /out/privkey.pem \
				-out /out/fullchain.pem \
				-subj "/CN=${DOMAIN:-e345ee.ru}"
		'
	fi

	chmod 600 "$tls_dir/privkey.pem" || true
}

compose() {
	require_cmd docker
	docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

cmd_up() {
	load_env
	ensure_tls_files
	compose up -d --build
	echo "OK: stack is up. Check: https://$(echo "${REACT_APP_API_URL:-}" | sed -E 's#^https?://##')/healthz" >&2
}

cmd_restart() {
	load_env
	compose restart gateway
}

cmd_issue_le() {
	load_env

	local domain="${DOMAIN:-}"
	if [[ -z "$domain" ]]; then

		domain="${REACT_APP_API_URL:-}"
		domain="${domain#http://}"
		domain="${domain#https://}"
		domain="${domain%%/*}"
	fi
	if [[ -z "$domain" ]]; then
		echo "ERROR: set DOMAIN (e.g. DOMAIN=e345ee.ru)" >&2
		exit 1
	fi

	require_cmd docker


	local webroot="${ROOT_DIR}/infra/certbot/www"
	mkdir -p "$webroot"

	local email_args
	if [[ -n "${CERTBOT_EMAIL:-}" ]]; then
		email_args=("--agree-tos" "-m" "${CERTBOT_EMAIL}" "--non-interactive")
	else
		email_args=("--register-unsafely-without-email" "--agree-tos" "--non-interactive")
	fi

	echo "Issuing Let's Encrypt cert for ${domain} via webroot..." >&2
	docker run --rm \
		-v "${webroot}:/var/www/certbot" \
		-v "/etc/letsencrypt:/etc/letsencrypt" \
		certbot/certbot:latest \
		certonly --webroot -w /var/www/certbot -d "${domain}" \
		"${email_args[@]}"

	DOMAIN="${domain}" TLS_CERTS_DIR="${TLS_CERTS_DIR}" bash "${ROOT_DIR}/infra/certbot/sync-certs.sh"
	compose restart gateway
	echo "OK: certificate issued and gateway restarted" >&2
}

cmd_sync_le() {
	load_env
	local domain="${DOMAIN:-e345ee.ru}"
	DOMAIN="$domain" TLS_CERTS_DIR="${TLS_CERTS_DIR}" bash "${ROOT_DIR}/infra/certbot/sync-certs.sh"
	compose restart gateway
}

main() {
	cd "$ROOT_DIR"

	local cmd="${1:-}"
	shift || true

	case "$cmd" in
		up) cmd_up "$@" ;;
		restart) cmd_restart "$@" ;;
		issue-le) cmd_issue_le "$@" ;;
		sync-le) cmd_sync_le "$@" ;;
		-h|--help|help|"") usage ;;
		*)
			echo "ERROR: unknown command: $cmd" >&2
			usage
			exit 1
			;;
	esac
}

main "$@"

