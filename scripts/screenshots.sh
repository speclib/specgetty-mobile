#!/usr/bin/env bash
# screenshots.sh
#
# Takes the Fastlane screenshots by running the app on an emulator and
# photographing what it actually draws, then pulls them into the metadata.
#
# The project photographed is invented in the test, not somebody's real
# repository: a screenshot is published, and a real project's contents are not
# this project's to publish.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

DEST="$ROOT/fastlane/metadata/android/en-US/images/phoneScreenshots"

SERIAL="$(./scripts/emulator.sh serial 2>/dev/null || true)"
if [[ -z "$SERIAL" ]]; then
  echo "screenshots: no emulator of ours is running; start one with scripts/emulator.sh start" >&2
  exit 1
fi
export ANDROID_SERIAL="$SERIAL"

echo "==> screenshots: running the screenshot test on $SERIAL"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.mipmip.specgettyondroid.ScreenshotTest

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
