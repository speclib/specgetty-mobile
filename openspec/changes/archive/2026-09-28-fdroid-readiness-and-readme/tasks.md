## 1. The audit

- [x] 1.1 Licences read from published POMs, group by group
- [x] 1.2 A dependency with no licence block named as such
- [x] 1.3 The proprietary-service check, with the command that repeats it
- [x] 1.4 Both permissions, with the cost of refusing each
- [x] 1.5 The intent filter claimed, and the one deliberately not
- [x] 1.6 The wrapper jar named, with its checksum and the reason
- [x] 1.7 Commands to re-run the whole audit

## 2. Store metadata

- [x] 2.1 `title.txt`, `short_description.txt`, `full_description.txt`
- [x] 2.2 `changelogs/1.txt`
- [x] 2.3 The screenshots directory, honestly empty
- [x] 2.4 The description says the app reads and does not edit
- [x] 2.5 The description says it is not affiliated with OpenSpec

## 3. README

- [x] 3.1 What the app is, and the not-affiliated statement
- [x] 3.2 Building, with nix and without
- [x] 3.3 Installing the APK on a phone
- [x] 3.4 Adding a repository, and what happens to a page URL
- [x] 3.5 The token, and why it is typed rather than captured
- [x] 3.6 The gate and its floors
- [x] 3.7 Where the grammar comes from
- [x] 3.8 The Phase 2 roadmap

## 4. Proof

- [x] 4.1 The dependency check returns zero
- [x] 4.2 `git ls-files` finds only the wrapper jar
- [x] 4.3 The gate passes
