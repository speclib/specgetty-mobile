---
# specgetty-mobile-ikzj
title: QR scanning and share-into
status: todo
type: epic
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T21:10:47Z
parent: specgetty-mobile-acg7
blocked_by:
    - specgetty-mobile-em4u
---

Three transports, one pipeline, and nothing acted on until the person confirms.

- [ ] CameraX pinned stable, core, camera2, lifecycle and view
- [ ] com.google.zxing:core as the decoder, pure Java, no ML Kit and no prebuilt natives
- [ ] FrameConverter packs the luminance plane, row stride handled, tested off device
- [ ] CAMERA requested on first scan, uses-feature required false
- [ ] ACTION_SEND with text/plain only, launchMode singleTask, handled in onCreate and onNewIntent
- [ ] A capture fills the add form, editable, and clones nothing
- [ ] The token field is never populated from a capture
- [ ] Text with no URL says so and leaves the form alone
- [ ] A live camera decode is not provable on an emulator: state that rather than fake it
