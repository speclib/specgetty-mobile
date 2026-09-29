## 1. Two emulators, told apart

- [x] 1.1 Give `scripts/emulator.sh` an API level argument, defaulting to 26, and
      verify it creates and starts `specgetty-api36` without disturbing
      `specgetty-api26`
- [x] 1.2 Pin the instrumented suite to the API 26 emulator by name, and verify
      `scripts/e2e.sh` still targets it with an API 36 emulator of ours also
      attached to the same adb server
- [x] 1.3 Verify a foreign emulator attached alongside both of ours is still left
      alone, which is the behaviour that already existed
- [x] 1.4 Put `ffmpeg` in the emulator devShell, and verify `ffmpeg -version`
      resolves inside `nix develop .#emulator` and not from the host

## 2. The screenshots the set was missing

- [x] 2.1 Add a spec card shot to the screenshot test, and verify it lands as a
      new file showing a requirement's card rather than the outline
- [x] 2.2 Add an adaptive two-pane shot, and verify it shows the outline and a
      card together rather than a narrow layout stretched
- [x] 2.3 Retake all seven, and verify every file in `phoneScreenshots/` was
      written by that run rather than surviving from the previous set

## 3. The hero

- [x] 3.1 Add a test that opens a spec, opens a card and steps through
      requirement and scenarios, holding on each long enough to read, and verify
      it passes on the API 26 emulator
- [x] 3.2 Add `scripts/recording.sh` that runs it under `screenrecord`, pulls the
      result and encodes a GIF, and verify one command produces the file from a
      clean state
- [x] 3.3 Verify the GIF is at or under 800 KB, and record which of the fallbacks
      in `design.md` was needed if any. 174,677 bytes at 360x640, 12 fps, 12
      seconds. None of the fallbacks was needed. What brought it there was not
      encoder tuning but filming less: the first cut ran 25.5 seconds and opened
      on the launcher, because the camera was started before the app was worth
      watching.
- [x] 3.4 Verify the GIF loops and is legible at the width the README renders it,
      by opening it rather than by trusting the encoder
- [x] 3.5 Verify the recording shows the project the test wrote, not a real one

## 4. The README

- [x] 4.1 Add the badge block, and verify all six resolve from a browser rather
      than only from `curl`
- [x] 4.2 Put the hero under the opening paragraph, and verify the first screen
      of the rendered page shows what the app is and what it looks like
- [x] 4.3 Add a short why, and verify it says why specifications get read on a
      phone before the feature list says what the app does
- [x] 4.4 Rewrite installation to offer the release first and the build command
      second, and verify both the download and the build command are present, as
      `fdroid-readiness` requires
- [x] 4.5 Add a screenshot strip with constrained widths, and verify the rendered
      page does not scroll sideways and the images do not fill the viewport
- [x] 4.6 Tighten the gate to about 8 lines, several projects to about 12, the
      store edge case to about 5, and how it is built to about 5, and verify
      nothing removed was a claim `fdroid-readiness` makes
- [x] 4.7 Add contributing and related sections, and verify every link resolves
- [x] 4.8 Verify all six README scenarios in `fdroid-readiness` are still true of
      the finished file, by reading the requirement and checking each against it
- [x] 4.9 Verify the file is about where it started in length, and say what it
      actually came to. **240 lines, up from 211.** The projection of roughly
      flat was wrong by 29. The gate and the reference sections did shrink as
      planned, and two things ate the saving that the estimate had not costed: a
      screenshot table is six lines of HTML per row rather than one of markdown,
      and installation grew from 15 lines to 34 because there is now a download,
      a signature to check and a build, where before there was only a build.

## 5. The audit

- [x] 5.1 Record in `docs/fdroid.md` which Android version the imagery is taken
      on, and that moving it to a newer one is blocked by
      `specgetty-mobile-ityo`
- [x] 5.2 Record how the recording is produced and how to produce it again, beside
      the screenshot instructions it sits next to

## 6. Verification

- [x] 6.1 `scripts/gate.sh` passes
- [x] 6.2 `openspec validate polish-the-readme --strict` passes
- [x] 6.3 The instrumented suite passes on API 26, unaffected by everything above

## 7. Not in this change

- [x] 7.1 A `docs/` split. Declined deliberately: six scenarios in
      `fdroid-readiness` say "WHEN the README is read", so a split is a spec
      change. When the README next outgrows itself, the spec is reworded first.
- [x] 7.2 An F-Droid submission or an F-Droid badge. There is no listing yet.
- [x] 7.3 Regenerating imagery automatically. Both are commands, run when the app
      looks different, not on a schedule and not in the gate.
