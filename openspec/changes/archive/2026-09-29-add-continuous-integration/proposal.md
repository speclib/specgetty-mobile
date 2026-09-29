## Why

Bean `specgetty-mobile-v047`, blocking `specgetty-mobile-4l3a`.

`specgetty-mobile-4l3a` asks for a polished README. Two of the things a polished
README needs cannot be written yet. There are no badges, because nothing
publishes any. And the install section offers one path, building the debug APK
yourself, which is the mobile equivalent of the failure specgetty named as one
of the three costliest in its own README: documenting the path fewest people
want.

Nothing runs on a push either. `scripts/gate.sh` decides whether a change may
ship, and it runs only where someone remembers to run it. A pull request from
outside has no way to show that it passes.

specgetty met the same ordering and split it: `add-continuous-integration`
shipped first, then `restructure-the-readme` gained its badges once rather than
twice. This change is the first half. The README is the second.

## What Changes

**The release build can be signed.** `assembleRelease` today produces an
unsigned APK, which Android refuses to install, because the build declares no
signing config. It gains one that reads a keystore out of the environment and
falls through to unsigned when the environment does not hold one, so F-Droid's
build from source and a local `assembleRelease` both keep working unchanged. The
keystore never enters the repository.

**The signature is decided rather than discovered.** The app is signed with this
project's own key and published on GitHub Releases. If it later reaches F-Droid,
F-Droid signs with its own key, and a user cannot update across the two: Android
refuses the install and the repository list and its credentials go with the
uninstall. That break is accepted now, at `versionCode` 1 with no users, rather
than paid for with reproducible builds. It is written down because the
alternative is a user discovering it.

**Three workflows, each with one job.**

| Workflow | Runs on | Does |
|----------|---------|------|
| Check    | every push and pull request | both halves of `scripts/gate.sh`, and keeps the debug build |
| Badges   | after a successful Check on `main` | publishes the OpenSpec metrics and the coverage number to `gh-pages` |
| Release  | a `v*` tag | checks the tag against the build, then builds, signs and uploads the APK |

Check runs the gate the project already has rather than a second opinion about
whether the tree is good. Two gates that can disagree are worse than one,
because the disagreement is found when a change is already believed finished.
The gate is in two halves here, `nix flake check` and then gradle inside
`nix develop`, because gradle needs network access that a sandboxed derivation
does not get, so Check is in two halves for the same reason.

Badges is separate from Check because publishing needs write access, and a pull
request from a fork can never be granted it. Combined, an outside contributor's
pull request would fail through no fault of theirs. It runs after Check rather
than beside it, so nothing describes a commit the gate rejected.

Release refuses to publish a tag that disagrees with the tree: `v0.1.0` requires
`versionName` to be `0.1.0` and `CHANGELOG.md` to have an entry for it. A
release whose notes do not exist is a release nobody can read. It also requires
`versionCode` to be greater than the one the previous tag published, because
Android refuses an update whose `versionCode` did not increase, and a release
that cannot be installed over its predecessor is found by the person it fails.

**The debug build Check keeps is not an install.** It carries the SDK's debug
key, which is public and is not the key a release carries, so installing it and
then installing a release means the same refusal the F-Droid decision above
accepts once. It stays because it is worth having a build of a pull request to
put on a phone, and it is named and described as what it is. The README does not
offer it as a way to get the app.

**The keystore has one copy that matters.** Losing it ends the ability to update
anyone who installed from a release, and the only remedy is a new application ID,
which `BRIEFING.md` says is permanent once published. Where the backup lives is
written down in the audit rather than remembered.

**The coverage number is taken, not measured again.** `jacocoCoverageVerification`
only passes or fails, so a script reads the percentage out of the jacoco XML
report and Check hands it to Badges. Measuring a second time gives a number that
can disagree with the one that decides whether a change may ship.

Not in scope, with the reason for each:

- **R8.** `isMinifyEnabled` stays false and `proguard-rules.pro` stays empty.
  JGit and SnakeYAML both work by reflection, so shrinking without rules for them
  produces an APK that builds, installs, and crashes on the first clone. Turning
  it on needs a test that runs the shrunk APK, which is its own change.
- **Reproducible builds, and any F-Droid submission.** Both follow from the
  signature decision above and neither is needed to publish an APK.
- **The instrumented tests.** They need an emulator, and a gate that runs on
  every push should not. `scripts/e2e.sh` stays a thing you run before a release.
- **The README.** `specgetty-mobile-4l3a`.

## Capabilities

### New Capabilities

- `continuous-integration`: what runs when a commit is pushed and when a version
  is tagged, how the three workflows divide the work and why they are not one,
  and what each publishes.

### Modified Capabilities

- `build-and-gate`: the release build gains a signing config, and what it
  produces now depends on whether the environment holds a keystore.
- `fdroid-readiness`: a new claim about the signature the published APK carries,
  and what F-Droid inclusion would do to it.

## Impact

- `.github/workflows/`: new, three files
- `app/build.gradle.kts`: a `signingConfigs` block and a release `signingConfig`
- `scripts/`: a script that reads the coverage percentage out of the jacoco XML
- `docs/fdroid.md`: the signature section
- `gh-pages`: new branch, created by the badge action on its first run
- Repository settings: four secrets holding the keystore and its passwords
- `specgetty-mobile-4l3a` unblocks once the first tag is published

## Rollback

Its own commit. Deleting the workflows stops everything this change starts, and
the signing config falls through to unsigned the moment the environment stops
holding a keystore, which is what every build outside the Release workflow
already does. Nothing in the app changes, so nothing in the app can break with
it.
