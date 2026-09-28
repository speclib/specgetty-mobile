#!/usr/bin/env bash
# emulator.sh [start|create|kill|wait]
#
# An API 26 emulator for the instrumented tests. API 26 because that is the
# minimum the app supports, and the version a regression would show up on first.
#
# The AVD lives in .avd inside the repository rather than in the user's home, so
# a machine that has never run this project is not left with one afterwards.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

AVD_NAME="${AVD_NAME:-specgetty-api26}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ROOT/.avd}"
IMAGE="system-images;android-26;default;x86_64"

create() {
  mkdir -p "$ANDROID_AVD_HOME"
  if [[ -d "$ANDROID_AVD_HOME/$AVD_NAME.avd" ]]; then
    echo "emulator: $AVD_NAME already exists"
    return 0
  fi
  echo "emulator: creating $AVD_NAME"
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
    >"$ROOT/.avd/emulator.log" 2>&1 &
  echo "$!" >"$ROOT/.avd/emulator.pid"
}

# Another project's emulator may be attached to the same adb server, so this
# finds the serial whose AVD name is ours rather than assuming a single device.
serial_of_ours() {
  local d name
  for d in $(adb devices | awk '/\tdevice$/ {print $1}'); do
    name="$(adb -s "$d" emu avd name 2>/dev/null | head -1 | tr -d '\r')"
    if [[ "$name" == "$AVD_NAME" ]]; then
      echo "$d"
      return 0
    fi
  done
  return 1
}

wait_for_boot() {
  echo "emulator: waiting for boot"
  local booted="" serial=""
  for _ in $(seq 1 180); do
    serial="$(serial_of_ours || true)"
    if [[ -n "$serial" ]]; then
      booted="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
      if [[ "$booted" == "1" ]]; then
        adb -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
        echo "emulator: booted as $serial"
        echo "$serial" >"$ROOT/.avd/serial"
        return 0
      fi
    fi
    sleep 2
  done
  echo "emulator: did not boot within six minutes" >&2
  tail -20 "$ROOT/.avd/emulator.log" >&2 || true
  return 1
}

kill_emulator() {
  local serial
  serial="$(serial_of_ours || true)"
  if [[ -n "$serial" ]]; then
    adb -s "$serial" emu kill >/dev/null 2>&1 || true
  fi
  if [[ -f "$ROOT/.avd/emulator.pid" ]]; then
    kill "$(cat "$ROOT/.avd/emulator.pid")" >/dev/null 2>&1 || true
    rm -f "$ROOT/.avd/emulator.pid"
  fi
  echo "emulator: stopped"
}

case "${1:-start}" in
  create) create ;;
  start) start && wait_for_boot ;;
  wait) wait_for_boot ;;
  serial) serial_of_ours ;;
  kill) kill_emulator ;;
  *)
    echo "usage: emulator.sh [start|create|wait|serial|kill]" >&2
    exit 1
    ;;
esac
