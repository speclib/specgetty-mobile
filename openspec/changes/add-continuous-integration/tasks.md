## 1. The build gains a version to publish and a key to sign with

- [x] 1.1 Add a gradle task that prints `versionCode` and `versionName` as the
      build actually resolves them, and verify `./gradlew -q printVersion` prints
      `1 0.1.0`
- [x] 1.2 Add a `signingConfigs` block built only when the environment holds the
      keystore path, store password, alias and key password, and verify
      `./gradlew assembleRelease` with none of them set still succeeds and
      produces an unsigned APK
- [x] 1.3 Point the release build type at that config when it exists, and verify
      with a throwaway keystore in the environment that `apksigner verify` reports
      the release APK as signed by it
- [x] 1.4 Verify the throwaway keystore is gone from the tree and that
      `git status` is clean, and that no keystore or password is matched by a
      search of the repository

## 2. The coverage number becomes readable

- [x] 2.1 Add a script that reads the instruction coverage ratio out of the XML
      `jacocoTestReport` writes and prints one `TOTAL <n>%` line, and verify it
      against a report produced by `./gradlew jacocoTestReport`
- [x] 2.2 Verify the script fails loudly when the report is absent rather than
      printing a zero that would publish as a badge
- [x] 2.3 Verify `scripts/gate.sh` still passes and is unchanged, since the
      number is read in CI rather than by the gate

## 3. Check

- [x] 3.1 Add `.github/workflows/check.yml` running `scripts/gate.sh` on every
      push and pull request, with `contents: read` and a concurrency group that
      cancels a superseded run, and verify a push turns the commit green
- [x] 3.2 Add the Gradle cache action and verify a second run on the same branch
      resolves dependencies from the cache rather than downloading them again
- [x] 3.3 Keep the debug APK as an artifact named so that it reads as a debug
      build, and verify the artifact appears on a finished run under that name
- [x] 3.4 Run the coverage script on the default branch after the gate passes and
      keep the figure for the badge job, and verify the run log shows the same
      percentage the coverage floor measured
- [x] 3.5 Verify a deliberately failing commit turns the run red, and that a
      failure in either half of the gate does it

## 4. Badges

- [x] 4.1 Add `.github/workflows/badges.yml` triggered by a successful Check on
      the default branch, with `contents: write`, and verify it does not run for
      a pull request from a fork
- [x] 4.2 Publish the OpenSpec metrics and verify `gh-pages` is created on the
      first run and holds the four badge files
- [x] 4.3 Publish the coverage figure Check measured, and verify the published
      JSON carries that number rather than one measured again
- [ ] 4.4 Verify a red Check publishes nothing and the previous badges stay

## 5. The key

- [x] 5.1 Generate the release keystore outside the repository, and verify
      `keytool -list` reads it back with the alias the build expects
- [ ] 5.2 Back it up, and verify the backup opens with the same passwords from a
      second machine or medium
- [x] 5.3 Load the keystore and its three passwords into repository secrets, and
      verify the secrets are listed under the names the workflow reads
- [x] 5.4 Record in `docs/fdroid.md` which key signs the published APK, where the
      keystore and its backup live, what losing it ends, what F-Droid inclusion
      would do to the signature and what a user would lose by it, and why
      reproducible builds were not taken instead

## 6. Release

- [ ] 6.1 Add `.github/workflows/release.yml` triggered by a `v*` tag with
      `contents: write`, asserting the tag against the printed `versionName`, and
      verify a tag that disagrees is refused with both values named
- [ ] 6.2 Assert the printed `versionCode` exceeds the one the previous tag
      published, and verify the check passes when there is no previous tag and
      fails when the number did not move
- [ ] 6.3 Assert `CHANGELOG.md` holds an entry for the version and extract it as
      the release notes, and verify a version with no entry is refused
- [ ] 6.4 Build, sign and upload an APK whose file name carries the version, and
      verify nothing is published when any assertion fails
- [ ] 6.5 Verify a missing signing secret fails the workflow rather than
      publishing an unsigned APK

## 7. First release

- [x] 7.1 Move the `[Unreleased]` entries in `CHANGELOG.md` under `0.1.0`, and
      verify the extraction step reads that entry back
- [ ] 7.2 Tag `v0.1.0`, and verify the release carries a signed APK that installs
      on a device with `adb install` and opens
- [ ] 7.3 Verify the coverage and OpenSpec badges render from `gh-pages`

## 8. Verification

- [x] 8.1 `scripts/gate.sh` passes
- [x] 8.2 `openspec validate add-continuous-integration --strict` passes

## 9. Not in this change

- [x] 9.1 The README's badges and its install link. They are
      `specgetty-mobile-4l3a`, which this change unblocks.
- [x] 9.2 R8. `isMinifyEnabled` stays false. JGit and SnakeYAML dispatch
      reflectively, so shrinking needs rules and a test that runs the shrunk APK,
      which is its own bean.
- [x] 9.3 A nix binary cache for the Android SDK closure. Deferred until a
      measurement says the SDK build is what makes a run slow, and complicated by
      the SDK being unfree.
- [x] 9.4 Reproducible builds and an F-Droid submission. Both follow from the
      signature decision and neither is needed to publish an APK.
