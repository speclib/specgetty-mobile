#!/usr/bin/env bash
# recording.sh
#
# Records the hero for the README: a spec opened as an outline, a card opened on
# it, and a reader stepping through requirement and scenarios. That is the thing
# a still cannot show.
#
# The taps come from a Compose test rather than from `input tap` at fixed
# coordinates, because coordinates encode a layout, and a recording that starts
# tapping the wrong thing produces a wrong hero rather than a failing run.
#
# Everything it needs comes from the emulator dev shell, ffmpeg included, so
# this is not a thing only one machine can produce.
#
# Recorded on API 26 for the same reason the screenshots are: the suite does not
# run on API 36. See specgetty-mobile-ityo.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

API=26
CLASS=io.github.mipmip.specgettyondroid.ScreenshotTest
REMOTE=/data/local/tmp/specgetty-hero.mp4
READY=/data/local/tmp/specgetty-recording-ready
DEST="$ROOT/docs/images"
GIF="$DEST/stepping-through-a-spec.gif"
BUDGET=$((800 * 1024))

# Half the device's 1080 wide, which is already more than the GIF keeps.
CAPTURE_SIZE=540x960
# What the README renders it at, give or take.
GIF_WIDTH=360
GIF_FPS=12

SERIAL="$(./scripts/emulator.sh serial "$API" 2>/dev/null || true)"
if [[ -z "$SERIAL" ]]; then
  echo "recording: no API $API emulator of ours is running;" >&2
  echo "           start one with scripts/emulator.sh start $API" >&2
  exit 1
fi
export ANDROID_SERIAL="$SERIAL"

adb -s "$SERIAL" shell settings put system accelerometer_rotation 0 </dev/null
adb -s "$SERIAL" shell settings put system user_rotation 0 </dev/null
adb -s "$SERIAL" shell "rm -f $REMOTE $READY" </dev/null

recorder=""

stop_recorder() {
  [[ -n "$recorder" ]] || return 0
  adb -s "$SERIAL" shell pkill -SIGINT screenrecord </dev/null >/dev/null 2>&1 || true
  wait "$recorder" 2>/dev/null || true
  # screenrecord needs a moment to finish writing the container after SIGINT.
  sleep 3
  recorder=""
}
trap stop_recorder EXIT

echo "==> recording: driving the app"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$CLASS#recordSteppingThroughASpec" \
  -Pandroid.testInstrumentationRunnerArguments.recording=true &
driver=$!

# A launcher, a form and a clone come before anything worth filming. The test
# touches a file once the spec is open, and the camera starts then, so the hero
# opens on the app rather than on a wallpaper.
echo "==> recording: waiting for the app to reach the spec"
for _ in $(seq 1 240); do
  if adb -s "$SERIAL" shell "ls $READY" </dev/null >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$driver" 2>/dev/null; then
    echo "recording: the driver finished without ever reaching the spec" >&2
    wait "$driver" || true
    exit 1
  fi
  sleep 1
done

if ! adb -s "$SERIAL" shell "ls $READY" </dev/null >/dev/null 2>&1; then
  echo "recording: the app never reached the spec" >&2
  exit 1
fi

echo "==> recording: starting screenrecord on $SERIAL"
adb -s "$SERIAL" shell screenrecord --size "$CAPTURE_SIZE" --bit-rate 6M \
  --time-limit 120 "$REMOTE" </dev/null &
recorder=$!

wait "$driver"

stop_recorder
trap - EXIT
adb -s "$SERIAL" shell "rm -f $READY" </dev/null

echo "==> recording: pulling the capture"
mkdir -p "$DEST"
mp4="$(mktemp -t specgetty-hero-XXXXXX.mp4)"
adb -s "$SERIAL" pull "$REMOTE" "$mp4" </dev/null >/dev/null
adb -s "$SERIAL" shell "rm -f $REMOTE" </dev/null

echo "==> recording: encoding the gif"
palette="$(mktemp -t specgetty-palette-XXXXXX.png)"
filters="fps=$GIF_FPS,scale=$GIF_WIDTH:-1:flags=lanczos"
ffmpeg -y -loglevel error -i "$mp4" -vf "$filters,palettegen=stats_mode=diff" "$palette"
ffmpeg -y -loglevel error -i "$mp4" -i "$palette" \
  -lavfi "$filters [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
  -loop 0 "$GIF"
rm -f "$mp4" "$palette"

size="$(stat -c%s "$GIF")"
echo "==> recording: $GIF ($size bytes)"

if [[ "$size" -gt "$BUDGET" ]]; then
  echo "recording: over the $BUDGET byte budget." >&2
  echo "           design.md gives the order of retreat: fewer frames per" >&2
  echo "           second, then smaller, then animated WebP last." >&2
  exit 1
fi
