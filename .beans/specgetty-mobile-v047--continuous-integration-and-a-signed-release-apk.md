---
# specgetty-mobile-v047
title: Continuous integration and a signed release APK
status: in-progress
type: epic
priority: normal
created_at: 2026-09-29T16:21:08Z
updated_at: 2026-09-29T16:22:25Z
blocking:
    - specgetty-mobile-4l3a
---

`specgetty-mobile-4l3a` wants a polished README. Two of the things a polished
README needs cannot be written yet: badges, and an install line that is not
"clone the repository and run gradle". Both need automation that does not exist,
so they come first and 4l3a follows.

specgetty solved the same ordering with `add-continuous-integration` before
`restructure-the-readme`, so the badges landed once rather than twice.

## The signature decision

The app is signed with our own key and published on GitHub Releases. If it later
reaches F-Droid, F-Droid signs with its own key and a user cannot update across
the two. That break is accepted now, at versionCode 1 with no users, rather than
paid for with reproducible builds.

## Scope

- A signing config that reads a keystore from the environment and falls through
  to unsigned when it is absent, so F-Droid's source build and a local
  `assembleRelease` both keep working.
- Check: `nix flake check`, then build, test, lint and coverage inside
  `nix develop`, the same two halves `scripts/gate.sh` runs. A debug APK is kept
  as an artifact.
- Badges: a separate workflow, because publishing needs write access that a
  fork's pull request can never be granted. It writes the four OpenSpec metrics
  and the coverage number to `gh-pages`.
- Release: on a `v*` tag. Asserts the tag matches `versionName` and that
  `CHANGELOG.md` has an entry for it, then builds, signs and uploads the APK.
- A script that reads the coverage percentage out of the jacoco XML report, so
  the badge publishes the number the gate measured rather than a second
  measurement that can disagree with it.

## Not in scope

- R8. `isMinifyEnabled` stays false and `proguard-rules.pro` stays empty. JGit
  and SnakeYAML both work by reflection, so shrinking without rules for them
  gives an APK that installs and then crashes on the first clone. That is its
  own change with a test behind it.
- Reproducible builds, and any F-Droid submission.
- Instrumented tests. They need an emulator, and a gate that runs on every push
  should not.
- The README itself.
