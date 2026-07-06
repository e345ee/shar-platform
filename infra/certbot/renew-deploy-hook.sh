#!/usr/bin/env bash
set -euo pipefail


DOMAIN="${DOMAIN:-e345ee.ru}"
TLS_CERTS_DIR="${TLS_CERTS_DIR:-/opt/shar-platform/certs}"
REPO_ROOT="${REPO_ROOT:-/opt/shar-platform}"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/compose.prod.yml}"

renewed_domains="${RENEWED_DOMAINS:-}"
renewed_lineage="${RENEWED_LINEAGE:-}"

contains_domain() {
  local needle="$1"
  local haystack="$2"

  [[ " ${haystack} " == *" ${needle} "* ]]
}

if [[ -z "${renewed_domains}" || -z "${renewed_lineage}" ]]; then
  echo "certbot hook: missing RENEWED_DOMAINS/RENEWED_LINEAGE; nothing to do" >&2
  exit 0
fi

if ! contains_domain "${DOMAIN}" "${renewed_domains}"; then
  echo "certbot hook: ${DOMAIN} not in renewed domains (${renewed_domains}); skip" >&2
  exit 0
fi

if [[ ! -f "${renewed_lineage}/fullchain.pem" || ! -f "${renewed_lineage}/privkey.pem" ]]; then
  echo "certbot hook: cert files not found in ${renewed_lineage}; skip" >&2
  exit 0
fi

mkdir -p "${TLS_CERTS_DIR}"
install -m 0644 "${renewed_lineage}/fullchain.pem" "${TLS_CERTS_DIR}/fullchain.pem"
install -m 0600 "${renewed_lineage}/privkey.pem" "${TLS_CERTS_DIR}/privkey.pem"

echo "certbot hook: synced certs to ${TLS_CERTS_DIR}"

if ! command -v docker >/dev/null 2>&1; then
  echo "certbot hook: docker not found; cannot restart gateway" >&2
  exit 0
fi

if [[ ! -d "${REPO_ROOT}" ]]; then
  echo "certbot hook: REPO_ROOT not found: ${REPO_ROOT}; cannot restart gateway" >&2
  exit 0
fi

cd "${REPO_ROOT}"


docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" restart gateway

echo "certbot hook: restarted gateway"