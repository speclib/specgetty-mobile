#!/usr/bin/env bash
# e2e.sh
#
# The instrumented suite on an API 26 emulator: the minimum Android the app
# supports, and the version a regression would show up on first.
#
# Starts an emulator if ours is not already running, and targets it by serial
# rather than assuming a single device, because another project's emulator may
# be attached to the same adb server.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

SERIAL="$(./scripts/emulator.sh serial 2>/dev/null || true)"
STARTED_IT="no"

if [[ -z "$SERIAL" ]]; then
  echo "==> e2e: no emulator of ours is running, starting one"
  ./scripts/emulator.sh start
  SERIAL="$(./scripts/emulator.sh serial)"
  STARTED_IT="yes"
fi

echo "==> e2e: running on $SERIAL"
ANDROID_SERIAL="$SERIAL" ./gradlew connectedDebugAndroidTest

if [[ "$STARTED_IT" == "yes" ]]; then
  echo "==> e2e: stopping the emulator we started"
  ./scripts/emulator.sh kill
fi

echo "==> e2e passed"
