## 1. Requirement source

- [x] 1.1 Slice one requirement's text out of a file by its name
- [x] 1.2 Headings read through the fence mask
- [x] 1.3 The slice ends at the next requirement or section heading, or the end
- [x] 1.4 An absent name yields nothing

## 2. Comparison

- [x] 2.1 Find a delta requirement's original in the capability's main spec
- [x] 2.2 No comparison for an ADDED requirement, an absent original, or a
      capability the main spec does not have
- [x] 2.3 No comparison for an archived change
- [x] 2.4 A unified difference with lines marked as only-original, only-proposed
      or common

## 3. View model

- [x] 3.1 Parse each delta through the repository's cache
- [x] 3.2 An outline of capability sections, requirements and scenarios
- [x] 3.3 Problems reported per delta file rather than the file being dropped
- [x] 3.4 The selected node, and its comparison when there is one
- [x] 3.5 The comparison opens on the difference

## 4. Screen

- [x] 4.1 Outline with operation marks
- [x] 4.2 Card for a requirement, a scenario and a Purpose
- [x] 4.3 Difference, original and proposed as three views
- [x] 4.4 Back to the change

## 5. Proof

- [x] 5.1 Tests for every scenario in this delta
- [x] 5.2 A test over a real delta from the vendored corpus
- [x] 5.3 The gate passes
