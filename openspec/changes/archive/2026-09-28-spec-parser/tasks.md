## 1. Shared markdown machinery

- [x] 1.1 Normalise line endings and strip a byte order mark
- [x] 1.2 Fence mask over ``` and ~~~, the fence lines included
- [x] 1.3 Two views of the file: the content, and the content with fenced lines
      blanked, which every heading test reads
- [x] 1.4 Heading level, section body up to the next heading at or above a level
- [x] 1.5 CommonMark thematic break test

## 2. The grammar

- [x] 2.1 The Purpose, Requirements, Requirement, scenario and delta patterns,
      case-insensitive where OpenSpec makes them so
- [x] 2.2 Scenario naming: strip the heading, a closing hash run and a
      `Scenario:` prefix
- [x] 2.3 Requirements read only inside the `## Requirements` section
- [x] 2.4 A scenario needs content to count

## 3. Scenario content

- [x] 3.1 `clauseOf`: a `- ` bullet opening with an upper-case keyword
- [x] 3.2 Ordered parts, clauses and prose interleaved as written
- [x] 3.3 A clause continues across source lines until a blank
- [x] 3.4 A horizontal rule ends a part and is not carried
- [x] 3.5 A prose line opening with a keyword starts its own part

## 4. Problems

- [x] 4.1 No Purpose, empty Purpose, delta header, no Requirements section,
      no requirements, requirements outside, requirement without scenario
- [x] 4.2 Each reason carries its line, or zero when it concerns absence
- [x] 4.3 A file with problems yields no outline

## 5. Proof

- [x] 5.1 specgetty's thirteen fixtures copied verbatim into test resources
- [x] 5.2 A table asserting each fixture lands on the same side of the grammar
- [x] 5.3 Tests for every scenario in this delta
- [x] 5.4 `spec` package coverage at or above 80 percent
- [x] 5.5 The gate passes
