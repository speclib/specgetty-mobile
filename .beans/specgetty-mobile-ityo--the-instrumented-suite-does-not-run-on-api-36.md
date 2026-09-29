---
# specgetty-mobile-ityo
title: The instrumented suite does not run on API 36
status: todo
type: bug
priority: normal
created_at: 2026-09-29T18:52:36Z
updated_at: 2026-09-29T18:52:36Z
---

Every instrumented test that adds a repository fails on an API 36 emulator. The
same tests pass on API 26. Found while trying to move the published imagery to a
newer Android in `polish-the-readme`, which was abandoned because of this.

```
FAILED  addARepository                          FAILED  openASpec
FAILED  browseTheChanges                        FAILED  openADeltaDiff
FAILED  aChangeShowsItsTasksAsBoxes             FAILED  anUnreachableHostSaysSo
FAILED  aRepositoryWithNoOpenSpecProjectSaysSo  FAILED  aSpecCardStepsToTheNext...
```

Each is a `ComposeTimeoutException` after 60 seconds, waiting for the repository
statistics to appear.

What has been ruled out:

- Not caused by any change in `polish-the-readme`. Verified against an unmodified
  `addARepository`.
- Not cleartext HTTP being blocked. `logcat` holds no cleartext error, and the
  app's target SDK would make that the obvious suspect.
- Not an app crash. There is no exception from the app at all in `logcat`.

`anUnreachableHostSaysSo` failing is the useful clue. That test needs no
successful clone, only the app to try one and report that it failed. If even the
error never appears, the add path is stalling rather than the test's local git
server being unreachable.

Worth knowing regardless of screenshots: API 36 is close to what a current phone
runs, and the app is not known to work there.

To reproduce:

```bash
nix develop .#emulator --command ./scripts/emulator.sh start 36
nix develop .#emulator --command bash -c \
  'ANDROID_SERIAL=$(./scripts/emulator.sh serial 36) ./gradlew connectedDebugAndroidTest'
```
