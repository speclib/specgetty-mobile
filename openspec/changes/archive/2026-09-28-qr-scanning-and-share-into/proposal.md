## Why

Bean `specgetty-mobile-ikzj`, milestone `specgetty-mobile-acg7`.

The extraction and normalisation half landed in milestone 03. This is the half
that needs a screen to land on, which is why it waited until the repository list
existed.

`BRIEFING.md` says scan, paste and share-into work as in beans-on-droid, and its
design records the reasoning that transfers unchanged. The short version:

- ML Kit is Play Services and is out. `zxing-android-embedded` wraps an older
  camera stack and owns its own activity; `zxing-cpp` ships prebuilt native
  libraries per ABI, which is more to audit for an F-Droid build. CameraX with
  `com.google.zxing:core` keeps the decode step pure Java and testable off a
  device, and the glue small and ours.
- Only `ACTION_SEND` with `text/plain` is claimed. Claiming `ACTION_VIEW` for
  `https` would put this app in the chooser for every link tapped on the phone.
- A share that arrives while the app is open reaches `onNewIntent`, so the
  activity is `singleTask`. Handling only the cold start would drop the share in
  what is probably the common case.

The permission is the real cost, and the mitigations are the same: `CAMERA` is
asked for when scanning is first chosen, `uses-feature` is marked not required
so the app still installs on a device without one, and refusing leaves
everything else working.

## What Changes

- `capture/QrDecoder` and `capture/FrameConverter`, ported from beans-on-droid.
  Both are pure and have no Android in them.
- `ui/screen/ScannerScreen`: a CameraX preview with an image analyser, the
  permission request, and the refusal states.
- The `CAMERA` permission, the `uses-feature`, the `ACTION_SEND` intent filter
  and `launchMode="singleTask"`.
- `MainActivity` hands a shared text to the repository list's capture, which
  already fills the add form and clones nothing.

## Capabilities

### Modified Capabilities

- `repo-url-capture`: gains the scan and the share as transports, and the rule
  that neither acts by itself.
- `repo-list-screen`: the add form gains a scan action and can be opened from
  outside the app.

## Impact

- New dependencies, both Apache-2.0: CameraX 1.6.2 and `com.google.zxing:core`.
- New permission: `CAMERA`. `docs/fdroid.md` in milestone 09 has to account for
  it rather than quietly claiming the app declares one permission.
- A decode from a live camera is not provable on an emulator. beans-on-droid
  established that and the reasoning is unchanged; the chain is tested link by
  link and the join between a real lens and the decoder is a manual check.
