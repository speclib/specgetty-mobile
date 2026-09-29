#!/usr/bin/env bash
# release-guard.sh
#
# Refuses a release whose tag disagrees with the tree it would be built from.
#
#   scripts/release-guard.sh v0.1.0
#
# Three disagreements are worth catching before anything is published:
#
#   versionName   a tag that names a version the build does not
#   versionCode   a number that did not increase, which Android refuses to
#                 install over its predecessor, found by the person it fails
#   CHANGELOG     a release with no notes
#
# The two version values are read from the build rather than from the file they
# are written in, because everything else in that block has already moved into
# the version catalogue and grepping for the two that have not is a guess with
# a date on it.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "release-guard: usage: release-guard.sh <tag>" >&2
  exit 2
fi

tag="$1"
version="${tag#v}"
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

read_version() {
  ./gradlew -q :app:printVersion 2>/dev/null | tail -1
}

echo "==> release-guard: reading the version this tree builds"
current="$(read_version)"
code="${current%% *}"
name="${current##* }"

if [[ -z "$code" || -z "$name" ]]; then
  echo "release-guard: could not read a versionCode and versionName from the build" >&2
  exit 1
fi

echo "    tag $tag wants $version; the build says versionName $name, versionCode $code"

if [[ "$name" != "$version" ]]; then
  echo "release-guard: the tag says $version and the build says $name" >&2
  exit 1
fi

previous="$(git tag --list 'v*' --sort=-version:refname \
  | grep -v "^${tag}\$" \
  | head -1 || true)"

if [[ -z "$previous" ]]; then
  echo "    no previous tag, so there is nothing for the version code to exceed"
else
  echo "==> release-guard: reading the version $previous published"
  worktree="$(mktemp -d)"
  trap 'git worktree remove --force "$worktree" >/dev/null 2>&1 || true; rm -rf "$worktree"' EXIT
  git worktree add --detach "$worktree" "$previous" >/dev/null
  previous_code="$(cd "$worktree" && ./gradlew -q :app:printVersion 2>/dev/null | tail -1 | cut -d' ' -f1)"

  echo "    $previous published versionCode $previous_code"

  if [[ -z "$previous_code" ]]; then
    echo "release-guard: could not read the versionCode $previous published" >&2
    exit 1
  fi

  if [[ "$code" -le "$previous_code" ]]; then
    echo "release-guard: versionCode $code does not exceed the $previous_code that $previous published;" >&2
    echo "               Android refuses an update whose versionCode did not increase" >&2
    exit 1
  fi
fi

echo "==> release-guard: looking for the changelog entry for $version"
scripts/changelog-entry.sh "$version" >/dev/null

echo "==> release-guard: $tag agrees with the tree"
