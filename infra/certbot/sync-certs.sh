#!/usr/bin/env bash
set -euo pipefail


DOMAIN="${DOMAIN:?set DOMAIN (e.g. e345ee.ru)}"
TLS_CERTS_DIR="${TLS_CERTS_DIR:?set TLS_CERTS_DIR (directory for fullchain.pem/privkey.pem)}"

LE_LIVE_DIR="/etc/letsencrypt/live/${DOMAIN}"

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
  echo "ERROR: run as root (use sudo)." >&2
  exit 1
fi

if [[ ! -d "${LE_LIVE_DIR}" ]]; then
  echo "ERROR: ${LE_LIVE_DIR} not found. Did you run certbot for ${DOMAIN}?" >&2
  exit 1
fi

mkdir -p "${TLS_CERTS_DIR}"


install -m 0644 "${LE_LIVE_DIR}/fullchain.pem" "${TLS_CERTS_DIR}/fullchain.pem"
install -m 0600 "${LE_LIVE_DIR}/privkey.pem" "${TLS_CERTS_DIR}/privkey.pem"

echo "Synced certs to ${TLS_CERTS_DIR}"