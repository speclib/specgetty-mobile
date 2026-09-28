## 1. Fuzzy matching

- [x] 1.1 Subsequence match reporting the indexes it matched
- [x] 1.2 sahilm/fuzzy's scoring: first character, separator, camel case,
      adjacency, and the leading-character penalty
- [x] 1.3 The unmatched-character penalty over the whole candidate
- [x] 1.4 A stable sort, strongest first

## 2. Query

- [x] 2.1 The sigil grammar: none, `'` and `:`
- [x] 2.2 A sigil with nothing after it is an empty query
- [x] 2.3 Smart case, applied to both the literal and the fuzzy matcher

## 3. Index

- [x] 3.1 Counts: specs, active, archived, open tasks
- [x] 3.2 Active in name order, archive newest first, undated last
- [x] 3.3 Search returning rows with the files that matched
- [x] 3.4 A name query reads no file from disk
- [x] 3.5 An empty result carries the query that produced it

## 4. Proof

- [x] 4.1 Tests for every scenario in this delta
- [x] 4.2 A search over the vendored corpus, with its duration reported
- [x] 4.3 `index` package coverage at or above 80 percent
- [x] 4.4 The gate passes
