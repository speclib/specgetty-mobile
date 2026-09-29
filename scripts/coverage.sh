#!/usr/bin/env bash
# coverage.sh
#
# Prints the instruction coverage the jacoco report measured, as one line:
#
#   TOTAL 83.4%
#
# The figure is read from the XML report rather than from gradle's console
# output, because the report is a published artifact of the jacoco plugin and
# the console format is a contract nobody made. It is read rather than measured
# again, because a second measurement can disagree with the one that decides
# whether a change may ship, and a badge that disagrees with the gate is worse
# than no badge.
#
# Run `./gradlew jacocoTestReport` first. This script does not run it: the gate
# decides, and making every local gate run a report only CI publishes would slow
# it down for nothing.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
REPORT="${1:-$ROOT/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml}"

if [[ ! -f "$REPORT" ]]; then
  echo "coverage: no report at $REPORT; run ./gradlew jacocoTestReport first" >&2
  exit 1
fi

# The report element's own counters come after every package, so the last
# INSTRUCTION counter in the file is the one for the whole bundle.
counter="$(grep -o '<counter type="INSTRUCTION"[^>]*>' "$REPORT" | tail -1 || true)"

if [[ -z "$counter" ]]; then
  echo "coverage: $REPORT has no INSTRUCTION counter" >&2
  exit 1
fi

missed="$(printf '%s' "$counter" | sed -n 's/.*missed="\([0-9]*\)".*/\1/p')"
covered="$(printf '%s' "$counter" | sed -n 's/.*covered="\([0-9]*\)".*/\1/p')"

if [[ -z "$missed" || -z "$covered" ]]; then
  echo "coverage: could not read missed and covered from $counter" >&2
  exit 1
fi

total=$((missed + covered))

if [[ "$total" -eq 0 ]]; then
  echo "coverage: the report counts no instructions at all" >&2
  exit 1
fi

printf 'TOTAL %s%%\n' "$(awk -v c="$covered" -v t="$total" 'BEGIN { printf "%.1f", (c / t) * 100 }')"
