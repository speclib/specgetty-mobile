## Context

See `proposal.md` for why. What shapes the approach is the shape of the existing
gate and the shape of the build.

`scripts/gate.sh` runs in two halves and cannot be collapsed into one.
`nix flake check` is sandboxed and covers shellcheck over `scripts/` and
`nixpkgs-fmt` over `flake.nix`. The gradle half runs inside `nix develop`,
because resolving dependencies needs network access a pure derivation does not
get. The flake fixes `android_sdk.accept_license = true` and `allowUnfree`, so a
runner needs no configuration of its own to build the SDK.

`app/build.gradle.kts` declares no `signingConfigs`. `assembleRelease` today
produces an unsigned APK. `isMinifyEnabled` is false and `proguard-rules.pro` is
empty. `jacocoTestReport` exists and emits XML; `jacocoCoverageVerification`
exists and only passes or fails, so it yields no number to publish.

`CHANGELOG.md` follows Keep a Changelog with an `[Unreleased]` section. The
repository is public, so Actions minutes are free and the cost of a slow run is
wall-clock rather than money.

specgetty solved the same problem and its three workflows are the reference. The
reasoning in its `check.yml` and `badges.yml` comments is adopted here rather
than rediscovered; where this project differs, the difference is below.

## Goals / Non-Goals

**Goals:**

- One gate, run in two places, never two gates that can disagree.
- A release that cannot be published in a state that does not install.
- A signing config that is invisible to every build that has no key.
- A coverage badge that cannot disagree with the coverage floor.

**Non-Goals:**

- Making the run fast. Correct first, measured second, optimised only where a
  measurement says it hurts.
- Any change to what the app does. No file under `app/src/` is touched.
- Publishing anything to F-Droid, or to any other store.

## Decisions

### Check runs `scripts/gate.sh`, not a transcription of it

specgetty's check workflow runs `nix flake check` and nothing else, on the
grounds that a workflow reproducing the steps would be a second opinion about
whether the tree is good. Here `nix flake check` is only half the gate, so the
equivalent is to invoke the script.

Alternative considered: spelling the gradle tasks out in the workflow. Rejected
for the reason above. The failure it invites is the one specgetty's comment
names: the two are edited apart over months, and the disagreement surfaces when
a change is already believed finished.

Consequence: `scripts/gate.sh` gains a caller it did not have. It already takes
no arguments and already exits non-zero on any failure, so it needs no change to
be called this way.

### The coverage number comes from a script the gate can also run

`jacocoCoverageVerification` decides but does not report. The number is read out
of the XML `jacocoTestReport` writes, by a script that prints one line.

Alternative considered: parsing gradle's console output. Rejected as a contract
with a format nobody promised. The XML report is a published artifact of the
jacoco plugin.

Alternative considered: having the badge workflow measure it. Rejected for the
reason the spec gives: two measurements can disagree, and the badge would be the
one people believe.

Where the script runs is the open part. It can be a step in Check after the gate
passes, which keeps `scripts/gate.sh` unchanged, or a line inside the gate,
which makes the local run and the CI run produce the same output. The first is
chosen: the gate's job is to decide, and adding a reporting step to it makes
every local run slower for a number only CI publishes.

### Signing is configured by absence, not by a flag

The signing config is built when the environment holds a keystore and is left
unbuilt when it does not, and the release build type takes whichever exists. No
property, no profile, no `-P` flag, no branch on a CI variable.

Alternative considered: a `signing.properties` file read when present. Equivalent
in effect and worse in CI, because the file has to be written from a secret
before gradle runs, which is a step that can be forgotten in a way an environment
variable is not.

Alternative considered: failing when the key is absent, with an opt-out flag.
Rejected: it inverts the default against every build that is not ours, which is
every build F-Droid does.

The keystore reaches the runner base64-encoded in a secret, is decoded to a path
outside the checkout, and is passed through four environment variables: the
path, the store password, the key alias and the key password.

### Release reads the version from gradle, not from the file

Asserting the tag against `versionName` and `versionCode` needs those two values.
Grepping `app/build.gradle.kts` works today and breaks the first time either
moves into a version catalog or a computed expression, which is exactly what
happened to `compileSdk`, `minSdk` and `buildToolsVersion` in this file already.

So the build prints them. A task that writes the two values is a few lines, is
testable locally, and cannot disagree with what the build actually uses.

The previous release's `versionCode` comes from the previous tag, read by
checking out that tag and asking the same task. The first release has no previous
tag, and the spec says that check passes rather than failing on the absence.

### The three workflows are three files, not three jobs in one

Check needs `contents: read`. Badges needs `contents: write`, which a fork's pull
request can never be granted. Release needs `contents: write` and runs on a tag
rather than on a push. Three permission sets and three triggers.

Alternative considered: one workflow with conditional jobs. Rejected because the
permission block is per workflow in the simple form, and because a fork's pull
request would then contain a job that cannot succeed, which reads to the
contributor as their failure.

### Caching is added where it is cheap, and nowhere else

`gradle/actions/setup-gradle` caches the wrapper distribution and the Maven
dependencies, which is one line for the two largest downloads.

The Android SDK closure is not cached in the first pass. Caching it means a nix
binary cache, and the SDK is unfree: pushing Google's artefacts to a public cache
is redistribution under a licence that does not clearly permit it. A private
cache avoids the question and costs a subscription and a key. Since the
repository is public, the runner is free, and the only cost is waiting, this is
deferred until a measurement says it is what hurts.

specgetty's check workflow carries a comment about removing a cache action that
made runs slower. That was a Go project whose dependencies are small. The
conclusion does not transfer, and the difference is recorded here rather than
letting the comment be read as a rule.

### R8 stays off

Stated in the proposal as out of scope; repeated here because the temptation is
local to this change. Turning `isMinifyEnabled` on is one line in the file this
change already edits. JGit and SnakeYAML both dispatch reflectively, so the APK
would build, install, and fail at the first clone, in a release build that the
unit tests never exercise. Turning it on needs a test that runs the shrunk APK,
which this change does not add.

## Risks / Trade-offs

**A cold run takes ten to twenty minutes** → Accepted. The repository is public
so the minutes are free, `cancel-in-progress` stops a superseded run, and the
Gradle cache covers the two largest downloads. Revisited if the SDK build turns
out to dominate.

**The keystore is a single point of failure** → Its custody and backup are a
requirement in `fdroid-readiness`, and the audit names where the backup is. The
project cannot recover from losing it, so the mitigation is that losing it is
made hard to do by accident rather than survivable.

**Four secrets have to exist before the first tag** → The Release workflow fails
on a missing secret rather than publishing an unsigned APK. Failing on the first
tag is recoverable; publishing an APK nobody can install is the failure that
reaches a user.

**The debug artifact is installed by someone who then cannot update** → Named and
described as a debug build, and the README does not offer it. This is a
mitigation, not a fix: the only fix is not publishing it, and putting a pull
request on a phone is worth more than the residue.

**`android_sdk.accept_license = true` accepts Google's terms on a runner** →
Already true of every local `nix develop`. The flake states it; CI does not
change what is being accepted, only where.

**The badge action is a third-party action on `@main`** → It runs only after the
gate has passed and only with `contents: write` on the badge branch, so the worst
it reaches is `gh-pages`. Pinning it to a tag or a SHA is worth doing when it
publishes something that matters more than badges.

## Migration Plan

1. The signing config lands first and changes nothing observable, because no
   environment holds a keystore yet.
2. Check lands second and starts reporting on pushes. Until Badges exists, its
   result is visible on the commit and nowhere else.
3. Badges lands third and creates `gh-pages` on its first successful run. The
   README's badges are `specgetty-mobile-4l3a`, so nothing points at the branch
   until then.
4. The keystore is generated, backed up and loaded into repository secrets.
5. Release lands last. It does nothing until a tag exists.
6. `v0.1.0` is tagged, and its release is the first thing the README can link.

Rollback is per step. Deleting a workflow file stops it. The signing config falls
through to unsigned the moment the environment stops holding a keystore, which is
what every build outside the Release workflow already does.
