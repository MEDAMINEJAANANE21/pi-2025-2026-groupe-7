#!/usr/bin/env bash
set -euo pipefail

APP_NAME="pi-app"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_SRC="${SCRIPT_DIR}/${APP_NAME}.service"
TIMER_SRC="${SCRIPT_DIR}/${APP_NAME}.timer"
SCRIPT_SRC="${SCRIPT_DIR}/${APP_NAME}.sh"

SERVICE_DST="/etc/systemd/system/${APP_NAME}.service"
TIMER_DST="/etc/systemd/system/${APP_NAME}.timer"
SCRIPT_DST="/opt/${APP_NAME}.sh"

if [[ $EUID -ne 0 ]]; then
  echo "Run as root: sudo $0"
  exit 1
fi

if [[ ! -f "$SERVICE_SRC" ]]; then
  echo "Missing service file: $SERVICE_SRC"
  exit 1
fi

if [[ ! -f "$TIMER_SRC" ]]; then
  echo "Missing timer file: $TIMER_SRC"
  exit 1
fi

ln -sfn "$SERVICE_SRC" "$SERVICE_DST"
ln -sfn "$TIMER_SRC" "$TIMER_DST"
ln -sfn "$SCRIPT_SRC" "$SCRIPT_DST"

systemctl daemon-reload
systemctl enable --now "${APP_NAME}.timer"

echo "Installed and started ${APP_NAME}.timer"
systemctl status "${APP_NAME}.timer" --no-pager
