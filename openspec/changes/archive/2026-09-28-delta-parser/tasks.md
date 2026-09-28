## 1. The delta grammar

- [x] 1.1 Any `## <word> Requirements` heading opens a section
- [x] 1.2 A requirement carries the operation of the nearest header above it
- [x] 1.3 An unrecognised operation is carried through as written
- [x] 1.4 A requirement's extent stops at the next requirement or the next
      operation heading, whichever comes first

## 2. Validity

- [x] 2.1 Report a `## Requirements` heading as the main spec form
- [x] 2.2 Report a requirement under no delta header, with its line
- [x] 2.3 Report a file with no requirements
- [x] 2.4 Report an empty `## Purpose`, and accept an absent one
- [x] 2.5 Require a scenario of ADDED and MODIFIED only

## 3. Nodes

- [x] 3.1 Every node carries its capability
- [x] 3.2 Paths prefixed with the capability
- [x] 3.3 Scenario content read by the shared reader

## 4. Corpus

- [x] 4.1 Vendor specgetty's `openspec/` tree into test resources
- [x] 4.2 A test that parses every delta file in it without raising
- [x] 4.3 A test that the corpus is present and of the expected size

## 5. Proof

- [x] 5.1 Tests for every scenario in this delta
- [x] 5.2 `spec` package coverage at or above 80 percent
- [x] 5.3 The gate passes
