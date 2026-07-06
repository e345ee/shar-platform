#!/usr/bin/env bash
set -euo pipefail


DOMAIN="${DOMAIN:?set DOMAIN (e.g. e345ee.ru)}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
WEBROOT_DIR="${WEBROOT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/www" && pwd)}"

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
  echo "ERROR: run as root (use sudo) so /etc/letsencrypt is writable." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker not found." >&2
  exit 1
fi

mkdir -p "${WEBROOT_DIR}"

email_args=()
if [[ -n "${CERTBOT_EMAIL}" ]]; then
  email_args+=("--agree-tos" "-m" "${CERTBOT_EMAIL}" "--non-interactive")
else
  email_args+=("--register-unsafely-without-email" "--agree-tos" "--non-interactive")
fi

set -x

docker run --rm \
  -v "${WEBROOT_DIR}:/var/www/certbot" \
  -v "/etc/letsencrypt:/etc/letsencrypt" \
  certbot/certbot:latest \
  certonly --webroot -w /var/www/certbot -d "${DOMAIN}" \
  "${email_args[@]}"
