## 1. Implementation

- [ ] 1.1 Give the project view's tab row the same treatment the change screen
      already has, so each tab takes the width its label needs and the row
      scrolls when they do not all fit
- [ ] 1.2 Draw a tab label on a single line, so a width nobody anticipated
      cannot split a word

## 2. Verification

- [ ] 2.1 An instrumented test at a narrow width and a large font scale,
      asserting each of the four labels is present in full
- [ ] 2.2 `scripts/gate.sh` passes
- [ ] 2.3 Look at it on the Fairphone, which is where it was seen
