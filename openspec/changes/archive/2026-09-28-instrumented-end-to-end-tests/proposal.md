## Why

Bean `specgetty-mobile-2b6s`, milestone `specgetty-mobile-pddz`.

`BRIEFING.md` names four journeys that have to work on an API 26 emulator: add a
repo, browse changes, open a delta diff, and open a spec. Four hundred unit
tests say the pieces are right; none of them says the app runs.

They cannot say it either. Every unit test here replaces something: the
dispatcher, the preferences, the vault, sometimes the file reader. That is what
makes them fast and precise, and it is also why they would keep passing if the
activity crashed on launch, if a navigation route were unregistered, or if JGit
failed on the one Android version the app claims to support.

API 26 specifically. It is the minimum the app supports, and the version where
the things that differ actually differ: `java.nio.file` and `java.time` exist
there only through desugaring, which milestone 01 enabled and nothing has
exercised on a device since.

## What Changes

- `EndToEndTest`: the four journeys plus the states that have to be readable,
  driven through the real navigation graph against a repository served over HTTP
  on loopback, so the clone is a real shallow clone.
- `scripts/emulator.sh`: create, start, wait for and stop an API 26 AVD, kept
  inside the repository rather than in the user's home.
- `scripts/e2e.sh`: run the suite, starting an emulator if ours is not already
  up and stopping only one it started.
- Content descriptions on the add form's fields and its confirm button, which
  the tests need and a screen reader wanted anyway.

## Capabilities

### New Capabilities

- `end-to-end`: the journeys that have to work on a device, and how they are
  run.

## Impact

- No production behaviour changes. The only production edit is semantics.
- The emulator scripts find their own AVD by name rather than assuming a single
  attached device, because a developer may have another project's emulator
  running. That is not hypothetical: one was, on the machine this was written
  on.
- The instrumented suite is not part of `scripts/gate.sh`. It needs an emulator,
  which a gate that has to run on every change should not require. It is run by
  `scripts/e2e.sh` before a release.
