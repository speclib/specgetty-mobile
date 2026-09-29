#!/usr/bin/env bash
# e2e.sh
#
# The instrumented suite on an API 26 emulator: the minimum Android the app
# supports, and the version a regression would show up on first.
#
# Starts one if the API 26 emulator is not already running, and targets it by
# serial rather than assuming a single device. Another project's emulator may be
# attached to the same adb server, and so may this project's own newer one that
# the imagery is taken on, so the API level is named here rather than left to a
# default that a later edit could move.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

API=26

SERIAL="$(./scripts/emulator.sh serial "$API" 2>/dev/null || true)"
STARTED_IT="no"

if [[ -z "$SERIAL" ]]; then
  echo "==> e2e: no API $API emulator of ours is running, starting one"
  ./scripts/emulator.sh start "$API"
  SERIAL="$(./scripts/emulator.sh serial "$API")"
  STARTED_IT="yes"
fi

echo "==> e2e: running on $SERIAL"
ANDROID_SERIAL="$SERIAL" ./gradlew connectedDebugAndroidTest

if [[ "$STARTED_IT" == "yes" ]]; then
  echo "==> e2e: stopping the emulator we started"
  ./scripts/emulator.sh kill "$API"
fi

echo "==> e2e passed"
