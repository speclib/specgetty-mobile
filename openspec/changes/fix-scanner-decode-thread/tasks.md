## 1. Implementation

- [ ] 1.1 Hold the decoded text in state the analyser may write from any thread,
      and act on it from a `LaunchedEffect`, so navigation and the Compose state
      are touched on the main thread
- [ ] 1.2 Keep the once-only guard set until the scanner has acted, rather than
      clearing it in the same breath as setting it

## 2. Verification

- [ ] 2.1 An instrumented test that delivers a decode from a background thread
      and asserts the add form opens with the URL, rather than the app dying
- [ ] 2.2 `scripts/gate.sh` passes
- [ ] 2.3 Scan a real code on the Fairphone, which is the one link a test cannot
      stand in for
