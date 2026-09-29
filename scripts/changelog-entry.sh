#!/usr/bin/env bash
# changelog-entry.sh
#
# Prints the CHANGELOG section for one version, which is what a release uses as
# its notes. Exits non-zero when there is no section for it: a release whose
# notes do not exist is a release nobody can read, and finding that out at the
# tag is cheaper than finding it out on the release page.
#
#   scripts/changelog-entry.sh 0.1.0
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "changelog-entry: usage: changelog-entry.sh <version> [changelog]" >&2
  exit 2
fi

version="$1"
ROOT="$(git rev-parse --show-toplevel)"
changelog="${2:-$ROOT/CHANGELOG.md}"

if [[ ! -f "$changelog" ]]; then
  echo "changelog-entry: no changelog at $changelog" >&2
  exit 1
fi

entry="$(awk -v want="$version" '
  /^## / {
    if (inside) exit
    line = $0
    sub(/^## +/, "", line)
    sub(/^\[/, "", line)
    sub(/\].*$/, "", line)
    if (line == want) { inside = 1; next }
    next
  }
  inside { print }
' "$changelog")"

entry="$(printf '%s\n' "$entry" | sed -e '/./,$!d' | tac | sed -e '/./,$!d' | tac)"

if [[ -z "$entry" ]]; then
  echo "changelog-entry: $changelog has no entry for $version" >&2
  exit 1
fi

printf '%s\n' "$entry"
