## 1. The emulator

- [x] 1.1 `emulator.sh` creating an API 26 AVD inside the repository
- [x] 1.2 Start headless, wait for boot, stop
- [x] 1.3 Find our own serial by AVD name, leaving another project's alone
- [x] 1.4 `.avd/` ignored by git

## 2. The suite

- [x] 2.1 A git repository served over HTTP on loopback, in the instrumented
      source set
- [x] 2.2 A fixture the test writes itself, so its numbers are its own
- [x] 2.3 Add a repository, and its statistics
- [x] 2.4 Browse the changes, and open one
- [x] 2.5 The tasks tab, with its figure and its boxes
- [x] 2.6 Open a delta, and its difference against the main spec
- [x] 2.7 Both sides of the difference on screen
- [x] 2.8 Open a spec, and a scenario's card
- [x] 2.9 No OpenSpec project, an unreachable host, and an SSH URL

## 3. Running it

- [x] 3.1 `e2e.sh` starting an emulator only if ours is not up
- [x] 3.2 Targeting by serial rather than assuming one device
- [x] 3.3 Content descriptions the tests and a screen reader both need

## 4. Proof

- [x] 4.1 Every test passes on an API 26 emulator
- [x] 4.2 The gate passes
