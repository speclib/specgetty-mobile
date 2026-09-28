## 1. Decoding

- [x] 1.1 `QrDecoder` over `com.google.zxing:core`, pure Java, no Android
- [x] 1.2 `FrameConverter` packing a frame to width, row stride handled
- [x] 1.3 Unit tests for both, off device

## 2. Dependencies and manifest

- [x] 2.1 CameraX and `com.google.zxing:core` in the catalog and the module
- [x] 2.2 `CAMERA` declared, `uses-feature` camera not required
- [x] 2.3 `ACTION_SEND` with `text/plain`, and nothing else
- [x] 2.4 `launchMode="singleTask"`

## 3. Scanner

- [x] 3.1 Scanner screen with a CameraX preview bound to the lifecycle
- [x] 3.2 Image analyser turning a frame into a decode attempt
- [x] 3.3 A decoded address closes the scanner and fills the form
- [x] 3.4 A decoded non-address says so and keeps scanning
- [x] 3.5 The camera is released when the scanner leaves
- [x] 3.6 Camera access asked for on first scan, refusal explained

## 4. Share

- [x] 4.1 A share on a cold start reaches the form
- [x] 4.2 A share while running reaches it through `onNewIntent`
- [x] 4.3 The token field is never filled from a capture

## 5. Proof

- [x] 5.1 Decoder tests against generated QR codes
- [x] 5.2 Stride handling tested with padded frames
- [x] 5.3 Capture tests already cover the form behaviour
- [x] 5.4 The gate passes
