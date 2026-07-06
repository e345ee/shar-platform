#!/usr/bin/env bash
set -euo pipefail


SSH_PORT="${SSH_PORT:-22}"

if [[ ${EUID:-$(id -u)} -ne 0 ]]; then
  echo "ERROR: run as root (use sudo)." >&2
  exit 1
fi

if ! command -v ufw >/dev/null 2>&1; then
  echo "ERROR: ufw is not installed. Install it first (e.g. apt-get install ufw)." >&2
  exit 1
fi

echo "Configuring UFW..."


ufw default deny incoming
ufw default allow outgoing


ufw allow "${SSH_PORT}/tcp" comment "SSH"
ufw allow 80/tcp comment "HTTP (Let's Encrypt HTTP-01)"
ufw allow 443/tcp comment "HTTPS"


ufw --force enable
ufw status verbose

echo "Done. If you're running Docker, verify published ports match 80/443 only."