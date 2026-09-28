## 1. View model

- [x] 1.1 The project for one repository, from the repository's state
- [x] 1.2 The selected tab
- [x] 1.3 The search query, its results and the files that matched
- [x] 1.4 Sigil insertion that rewrites the query rather than changing the grammar
- [x] 1.5 A state for a repository that failed or holds no project

## 2. Screen

- [x] 2.1 Header with the name and the statistics, visible on every tab
- [x] 2.2 Overview: counts, active changes, recently archived with dates
- [x] 2.3 Changes: grouped list, rows with progress, spec count and date
- [x] 2.4 Changes: search field, sigil control, empty result message
- [x] 2.5 Specs: the capability list
- [x] 2.6 Properties: project.md as Markdown else the configuration, and the
      schema rows
- [x] 2.7 Empty states for each tab

## 3. Navigation

- [x] 3.1 Opening a repository from the list
- [x] 3.2 Back to the list
- [x] 3.3 Rows for changes and specs navigate to the screens built next

## 4. Yaml

- [x] 4.1 A composable showing YAML monospaced with comments and keys apart

## 5. Proof

- [x] 5.1 View model tests for every scenario in this delta
- [x] 5.2 A test that opening the Specs tab parses nothing
- [x] 5.3 The gate passes
