#!/usr/bin/env bash
# screenshots.sh
#
# Takes the Fastlane screenshots by running the app on an emulator and
# photographing what it actually draws, then pulls them into the metadata.
#
# Taken on API 26, the same emulator the tests run on. Photographing a newer
# Android would be better, and is blocked: the instrumented suite does not run
# on API 36 at all. That is specgetty-mobile-ityo. The API level is named here
# rather than assumed, so moving it is one line once that is fixed.
#
# The two-pane shot is taken in a separate run with the device turned, because
# that layout appears only when the window is not compact, and the test rule's
# host activity is created before a test body could turn anything.
#
# The project photographed is invented in the test, not somebody's real
# repository: a screenshot is published, and a real project's contents are not
# this project's to publish.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

API=26
DEST="$ROOT/fastlane/metadata/android/en-US/images/phoneScreenshots"
CLASS=io.github.mipmip.specgettyondroid.ScreenshotTest

SERIAL="$(./scripts/emulator.sh serial "$API" 2>/dev/null || true)"
if [[ -z "$SERIAL" ]]; then
  echo "screenshots: no API $API emulator of ours is running;" >&2
  echo "             start one with scripts/emulator.sh start $API" >&2
  exit 1
fi
export ANDROID_SERIAL="$SERIAL"

rotate() {
  adb -s "$SERIAL" shell settings put system accelerometer_rotation 0 </dev/null
  adb -s "$SERIAL" shell settings put system user_rotation "$1" </dev/null
  sleep 2
}

run_tests() {
  ./gradlew connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="$1"
}

echo "==> screenshots: upright shots on $SERIAL"
rotate 0
run_tests "$CLASS#takeTheScreenshots,$CLASS#takeTheSpecCardScreenshot"

echo "==> screenshots: the two-pane shot, turned"
rotate 1
run_tests "$CLASS#takeTheWideScreenshot"

rotate 0

# The test writes through `screencap`, which runs as the shell user, because
# Gradle uninstalls both APKs when the suite finishes and anything in the app's
# own storage would go with them.
REMOTE="/data/local/tmp"
echo "==> screenshots: pulling from $REMOTE"
mkdir -p "$DEST"
# `adb` reads stdin, so it would swallow the rest of a piped list after the
# first iteration. The names are collected first, and every adb call gets
# /dev/null on its input.
mapfile -t names < <(
  adb -s "$SERIAL" shell "ls $REMOTE" </dev/null | tr -d '\r' | grep '^specgetty-.*\.png$'
)

for name in "${names[@]}"; do
  local_name="${name#specgetty-}"
  adb -s "$SERIAL" pull "$REMOTE/$name" "$DEST/$local_name" </dev/null >/dev/null
  adb -s "$SERIAL" shell "rm -f $REMOTE/$name" </dev/null
  echo "    $local_name ($(stat -c%s "$DEST/$local_name") bytes)"
done

echo "==> screenshots: written to $DEST"
