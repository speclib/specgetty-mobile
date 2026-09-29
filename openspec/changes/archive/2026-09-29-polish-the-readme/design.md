## Context

See `proposal.md` for why. What shapes the approach is what already exists.

`ScreenshotTest.kt` drives the real app through the Compose test rule and writes
five PNGs via `screencap`, which `scripts/screenshots.sh` pulls into the Fastlane
metadata. It photographs a project the test invents, because a real project's
contents are not this project's to publish. That machinery is the model for
everything here.

`scripts/emulator.sh` creates and starts one AVD, hard-coded to
`system-images;android-26;default;x86_64`, kept in `.avd` inside the repository
so a machine that has never run this project is not left with one afterwards. It
already handles a foreign emulator on the same adb server by targeting its own
by name.

`system-images;android-36;default;x86_64` is present in the SDK the flake
composes, so no flake change is needed for the image. `ffmpeg` is not in either
devShell.

`screenrecord` on the device is v1.2 and takes `--size`, `--bit-rate` and
`--time-limit`, with a 180 second ceiling.

The README is 211 lines, and `fdroid-readiness` asserts six things about its
contents.

## Goals / Non-Goals

**Goals:**

- A landing page a stranger can read, without losing a single claim the spec
  makes about the README.
- Imagery that looks like the present, produced by one command each.
- A recording anyone can regenerate, not only the machine it was first made on.

**Non-Goals:**

- Splitting the README. Deliberate, and the proposal says why.
- Changing anything the app does. No file under `app/src/main/` is touched.
- Making the recording part of the gate. It needs an emulator, and so does the
  instrumented suite, which is already out for that reason.

## Decisions

### The recording is driven by a test, not by tap coordinates

The obvious cheap route is `adb shell input tap x y` in a loop. It is rejected:
coordinates encode a layout, the layout they would encode is the one
`spec-card-next-and-previous` shipped days ago, and a recording that silently
starts tapping the wrong thing produces a hero that is wrong rather than a build
that is red.

So the taps come from the Compose test rule, addressed the way
`ScreenshotTest.kt` addresses them. `screenrecord` runs beside it, started by the
script and stopped when the test finishes, which is the same division of labour
`scripts/screenshots.sh` already uses between a test that drives and a script
that collects.

### The recording holds still on purpose

A recording made at the speed a test runs is unreadable: the cards change faster
than they can be read, and the result looks like a glitch rather than a feature.
The test therefore waits about a second and a half on each card.

That is a deliberate inefficiency in a test, which is worth stating because it
looks like a mistake. It is also why this lives in its own test rather than in
`ScreenshotTest`: the screenshot run should stay as fast as it is.

### Everything the recording needs comes from the flake

`ffmpeg` exists on the machine this was explored on and that is not a reason to
depend on it. It goes into the emulator devShell, beside the SDK, so that
`scripts/recording.sh` behaves the way `scripts/screenshots.sh` does: run it in
the shell the project defines and it works.

### The API level is an argument, and imagery uses 26 anyway

`scripts/emulator.sh` takes the API level rather than hard-coding one, and each
AVD keeps its own log, pid and serial so that starting one does not make another
unkillable. The suite names the API 26 emulator rather than taking whichever of
ours is up, which is what forces the `end-to-end` delta.

Imagery was meant to use API 36. It does not, because the instrumented suite
does not run there: every test that adds a repository times out, the failure
predates this change, and there is no app-side exception or cleartext error to
explain it. That is `specgetty-mobile-ityo`, and it is a bigger finding than the
screenshots that uncovered it.

So the screenshots and the recording are taken on API 26, the same emulator the
tests use. The argument stays because the investigation will need it, and
because hard-coding a version into a script is what hid this for as long as it
was hidden.

Two bugs surfaced on the way and are fixed here, because the API argument is
unusable with either in place. `serial_of_ours` ran under `set -e` and asked
every attached device for an AVD name; a physical device answers with an error,
which ended the search at whatever was plugged in rather than at the emulator.
And the log, pid and serial files were shared between AVDs.

### GIF, with a size budget and a fallback

The hero is a GIF. It animates inline on GitHub from a relative path, which mp4
does not, and this content is the good case for it: the frames between taps are
identical, so the encoder has almost nothing to store.

The budget is about 800 KB. specgetty restructured its README partly because 2.5
MB of recordings all loaded on the landing page, and repeating that while citing
it would be poor. If the first encode overruns, the order of retreat is fewer
frames per second, then smaller dimensions, then animated WebP, which GitHub
renders and which is several times smaller. WebP is last because it is the only
one of the three that changes what some readers can see.

### The README's shape

```
  title, badges, one paragraph, not affiliated
  the hero
  Why                     new, short
  What it does            tightened
  Install                 download first, build second
  Adding a repository     unchanged in substance
  A private repository    unchanged in substance
  Screenshots             new, a strip
  Building and the gate   26 lines to 8
  Roadmap
  Contributing            new
  Related                 new
  Licence
```

Six scenarios in `fdroid-readiness` have to stay true of this file. Building,
downloading, installing, the URL, the token, the roadmap and the affiliation line
all keep a home in the README itself, which is the whole reason the split was
declined.

Screenshots are constrained in width rather than dropped in raw. A 1080 by 1920
image in a README renders as a full-width tower and pushes everything below it
off the first screen.

## Risks / Trade-offs

**The GIF overruns its budget** → The retreat order is decided above rather than
improvised at the time. The content is the favourable case, so this is unlikely
before the second fallback.

**A recording is not reproducible byte for byte** → It is not meant to be.
Timing varies per run, so the command produces an equivalent recording rather
than an identical file. The spec asks that it can be made again, not that it
comes out the same.

**Two AVDs cost disk** → They live in `.avd` in the repository, and only the one
being used is started. The imagery AVD is needed when imagery is regenerated,
which is rarely.

**API 36 shows something the app does not handle** → It did, and that is why
imagery stayed on 26. Recorded as `specgetty-mobile-ityo` rather than absorbed
here.

**The screenshots and the recording drift apart** → They are taken by two
commands on two runs and could disagree about what the app looks like. Both read
the same invented project from the same test fixtures and both name API 26, so a
drift would have to come from one of the two scripts being changed alone.

## Migration Plan

1. `ffmpeg` into the emulator shell, and `scripts/emulator.sh` takes an API
   level. Nothing observable changes yet.
2. The suite is pinned to the API 26 emulator by name, before a second one
   exists to confuse it.
3. The screenshot test gains the spec card and the adaptive layout. The five
   existing shots are retaken in the same run.
4. The recording test and `scripts/recording.sh` land, and the hero is produced.
5. The README is rewritten around what now exists.
6. `docs/fdroid.md` records the Android version the imagery was taken on.

Rollback is per step, and the README is the last one, so a revert of it leaves
the imagery and the scripts intact.
