#!/usr/bin/env bash
# emulator.sh [start|create|wait|serial|kill] [api]
#
# An emulator for the instrumented tests and for the published imagery.
#
# The API level is an argument because the two want different answers. The tests
# run on 26, the minimum the app supports and the version a regression shows up
# on first. The screenshots and the recording are taken on a newer Android,
# because what the app should look like in a photograph has nothing to do with
# the oldest version it still runs on.
#
# Each AVD keeps its own log, pid and serial, so that starting one does not make
# the other unkillable. Ours are told apart from a foreign emulator, and from
# each other, by the AVD name the device reports.
#
# The AVDs live in .avd inside the repository rather than in the user's home, so
# a machine that has never run this project is not left with them afterwards.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

API="${2:-${EMULATOR_API:-26}}"
AVD_NAME="${AVD_NAME:-specgetty-api$API}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ROOT/.avd}"
IMAGE="system-images;android-$API;default;x86_64"

LOG="$ROOT/.avd/$AVD_NAME.log"
PIDFILE="$ROOT/.avd/$AVD_NAME.pid"
SERIALFILE="$ROOT/.avd/$AVD_NAME.serial"

create() {
  mkdir -p "$ANDROID_AVD_HOME"
  if [[ -d "$ANDROID_AVD_HOME/$AVD_NAME.avd" ]]; then
    echo "emulator: $AVD_NAME already exists"
    return 0
  fi
  echo "emulator: creating $AVD_NAME from $IMAGE"
  echo "no" | avdmanager create avd \
    --name "$AVD_NAME" \
    --package "$IMAGE" \
    --device "pixel_2" \
    --force
}

start() {
  create
  echo "emulator: starting $AVD_NAME"
  emulator -avd "$AVD_NAME" \
    -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
    -no-snapshot -wipe-data \
    >"$LOG" 2>&1 &
  echo "$!" >"$PIDFILE"
}

# Another project's emulator may be attached to the same adb server, and so may
# our own other one, so this finds the serial whose AVD name is the one asked
# for rather than assuming a single device or a single one of ours.
serial_of_ours() {
  local d name
  for d in $(adb devices | awk '/\tdevice$/ {print $1}'); do
    # A physical device answers this with an error, and under `set -e` that
    # error would end the search at whatever is plugged in rather than at the
    # emulator being looked for.
    name="$(adb -s "$d" emu avd name 2>/dev/null | head -1 | tr -d '\r' || true)"
    if [[ "$name" == "$AVD_NAME" ]]; then
      echo "$d"
      return 0
    fi
  done
  return 1
}

wait_for_boot() {
  echo "emulator: waiting for $AVD_NAME to boot"
  local booted="" serial=""
  for _ in $(seq 1 180); do
    serial="$(serial_of_ours || true)"
    if [[ -n "$serial" ]]; then
      booted="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
      if [[ "$booted" == "1" ]]; then
        adb -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
        echo "emulator: booted as $serial"
        echo "$serial" >"$SERIALFILE"
        return 0
      fi
    fi
    sleep 2
  done
  echo "emulator: $AVD_NAME did not boot within six minutes" >&2
  tail -20 "$LOG" >&2 || true
  return 1
}

kill_emulator() {
  local serial
  serial="$(serial_of_ours || true)"
  if [[ -n "$serial" ]]; then
    adb -s "$serial" emu kill >/dev/null 2>&1 || true
  fi
  if [[ -f "$PIDFILE" ]]; then
    kill "$(cat "$PIDFILE")" >/dev/null 2>&1 || true
    rm -f "$PIDFILE"
  fi
  rm -f "$SERIALFILE"
  echo "emulator: stopped $AVD_NAME"
}

case "${1:-start}" in
  create) create ;;
  start) start && wait_for_boot ;;
  wait) wait_for_boot ;;
  serial) serial_of_ours ;;
  kill) kill_emulator ;;
  *)
    echo "usage: emulator.sh [start|create|wait|serial|kill] [api]" >&2
    exit 1
    ;;
esac
