# project-index Specification

## Purpose
The counts, grouping, ordering and search the screens read. This capability owns
how a change list is narrowed by typing, and it follows specgetty's
`change-search` so that the two tools agree about what a query means.

## Requirements

### Requirement: The project reports its counts

The system SHALL report the number of specs, the number of active changes, the
number of archived changes, and the open tasks across active changes.

#### Scenario: A project with content

- **WHEN** a project holds 28 specs, 1 active change and 57 archived changes
- **THEN** those are the counts reported

#### Scenario: Tasks are counted across active changes only

- **WHEN** an archived change holds completed tasks
- **THEN** they are not in the open task figure

#### Scenario: An empty project

- **WHEN** a project holds nothing
- **THEN** every count is zero

### Requirement: Changes are grouped and ordered

The system SHALL present active changes in name order and archived changes by
date with the newest first, ties broken by name.

#### Scenario: Active changes

- **WHEN** the active changes are listed
- **THEN** they are in name order

#### Scenario: Archived changes

- **WHEN** the archived changes are listed
- **THEN** they are newest first by archive date

#### Scenario: Two archived on the same day

- **WHEN** two changes were archived on the same date
- **THEN** they are ordered by name between themselves

#### Scenario: An archived change with no date

- **WHEN** an archived change carries no date
- **THEN** it is listed last rather than dropped

### Requirement: A query selects its matcher by a sigil

The system SHALL read a query's leading sigil: none for fuzzy matching on the
change name, `'` for a literal substring of the name, and `:` for a literal
substring of the change's artifact and spec text as well as its name.

#### Scenario: Fuzzy by default

- **WHEN** the query is `expzip`
- **THEN** a change named `export-change-as-zip` matches, its characters
  appearing in that order in the name

#### Scenario: Literal on the name

- **WHEN** the query begins with `'`
- **THEN** the rest is matched as a substring of the name and fuzzy matching is
  not used

#### Scenario: Literal in the text

- **WHEN** the query begins with `:`
- **THEN** the rest is matched as a substring of every artifact and spec file of
  each change, and of the change name

#### Scenario: A sigil with nothing after it

- **WHEN** the query is `'` or `:` alone
- **THEN** it is an empty query and filters nothing out

#### Scenario: An empty query

- **WHEN** the query is empty
- **THEN** the list keeps its normal ordering and nothing is filtered

### Requirement: Case is smart

The system SHALL match case-insensitively when a query carries no upper-case
character, and case-sensitively when it carries one.

#### Scenario: All lower case

- **WHEN** the query is `export`
- **THEN** a change named `Export-Change` matches

#### Scenario: One upper-case character

- **WHEN** the query is `Export`
- **THEN** a change named `export-change` does not match

#### Scenario: Smart case applies to the fuzzy matcher too

- **WHEN** an upper-case fuzzy query is run
- **THEN** every character it matched has to agree in case

### Requirement: Fuzzy results are ranked

The system SHALL order fuzzy results by match score, strongest first, scoring a
match at the start of the name, after a separator, or adjacent to a previous
match more highly than one in the middle of a word.

#### Scenario: Ranked results

- **WHEN** a fuzzy query matches more than one change
- **THEN** the strongest match is first

#### Scenario: A match at the start beats one in the middle

- **WHEN** the query matches the start of one name and the middle of another
- **THEN** the one matching at the start is first

#### Scenario: Adjacent characters beat scattered ones

- **WHEN** the query's characters are adjacent in one name and scattered in
  another
- **THEN** the adjacent one is first

#### Scenario: Literal results keep their order

- **WHEN** a `'` or `:` query matches several changes
- **THEN** they keep the order the list already had, there being no score

### Requirement: A text match says which files matched

The system SHALL report, for a change that matched a `:` query through its
text, which of its files the text was found in, and SHALL report none when the
change matched through its name alone.

#### Scenario: Matched in two artifacts

- **WHEN** a change matches through the text of `proposal.md` and `tasks.md`
- **THEN** both file names are reported for that row

#### Scenario: Matched in a delta

- **WHEN** a change matches through the text of one of its spec deltas
- **THEN** that capability's spec file is reported

#### Scenario: Matched on the name

- **WHEN** a change matches through its name
- **THEN** no file names are reported for it

#### Scenario: Matched on both

- **WHEN** a change matches on its name and in a file
- **THEN** the file is still reported

### Requirement: An empty result is distinguishable from an empty project

The system SHALL let a caller tell a query that matched nothing from a project
that holds nothing.

#### Scenario: A query that matched nothing

- **WHEN** a non-empty query matches no change
- **THEN** the result is empty and the query is available to echo back

#### Scenario: A project with no changes

- **WHEN** a project holds no changes and no query is applied
- **THEN** the result is empty for a different reason, and the two are
  distinguishable

### Requirement: Searching a real project stays quick

The system SHALL search the whole of a real project without reading a file it
does not need to.

#### Scenario: A name query reads no file

- **WHEN** a fuzzy or `'` query runs
- **THEN** no artifact or spec file is read

#### Scenario: A text query over a real project

- **WHEN** a `:` query runs over a project of about 30 specs and 57 archived
  changes
- **THEN** it completes, and the time it took is measured rather than assumed
