## Why

Bean `specgetty-mobile-03ra`, milestone `specgetty-mobile-pddz`.

`BRIEFING.md` calls the F-Droid requirements hard, and the app now has enough in
it that they can be checked rather than promised. It has also gained a second
permission since the briefing was written, which is precisely the kind of claim
that rots if nobody writes it down.

The audit is worth doing as an audit rather than an assertion. Every licence in
`docs/fdroid.md` was read out of the POM the artifact publishes, not recalled,
and the one dependency whose POM carries no licence block is named as such
instead of being quietly filled in.

There is one deviation from "no prebuilt binaries or jars": the Gradle wrapper
jar. Stating it plainly, with its checksum and the reason, is better than either
removing the wrapper or hoping nobody greps for `.jar`.

## What Changes

- `docs/fdroid.md`: the audit. Dependencies by group with their published
  licences, the two permissions and what refusing each costs, the single intent
  filter claimed and the one deliberately not, the wrapper jar deviation, and
  the commands to re-run the audit.
- `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt`,
  `full_description.txt` and `changelogs/1.txt`, with the screenshots directory
  present and honestly empty.
- `README.md`: what the app is, how to build and install it, how to add a
  repository and a token, how the gate works, and the Phase 2 roadmap.

## Capabilities

### New Capabilities

- `fdroid-readiness`: the claims this project makes about what it ships, and how
  each is checked.

## Impact

- No code changes and no new dependencies.
- The screenshots directory holds a note rather than images. A screenshot of an
  app nobody has run would misrepresent it; `scripts/screenshots.sh` takes real
  ones once a device is attached.
