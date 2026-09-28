## Why

Milestone `specgetty-mobile-pddz`. The Fastlane metadata shipped with an empty
screenshots directory and a note saying a screenshot of an app nobody has run
would misrepresent it. The app has now been run, on an emulator, by the
instrumented suite. So the note can be replaced with photographs.

Taking them by driving the real app rather than by composing them by hand is the
whole point: a store screenshot is a claim about what the app looks like, and
the only way to keep that claim true as the app changes is to regenerate it from
the app.

The project photographed is invented in the test rather than borrowed from a
real repository. A screenshot is published, and somebody else's specifications
are not this project's to publish.

## What Changes

- `ScreenshotTest`: drives the app through the repository list, the overview,
  the changes, a change's tasks and a spec delta's difference, photographing
  each.
- `scripts/screenshots.sh`: runs it and pulls the files into the metadata.
- Five screenshots in `fastlane/metadata/android/en-US/images/phoneScreenshots/`.

## Capabilities

### Modified Capabilities

- `fdroid-readiness`: the screenshots requirement changes from "present and
  honestly empty" to "taken from the running app".

## Impact

- No production code changes.
- Three things about a device had to be worked around, and each is recorded in
  the test where it bites: external storage may not exist, Gradle uninstalls the
  APKs when the suite ends, and there is no gentle way to dismiss the soft
  keyboard in an activity that is only Compose.
