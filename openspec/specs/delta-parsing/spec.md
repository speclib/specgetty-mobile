# delta-parsing Specification

## Purpose
Reading one spec file of a change: its requirements, what the change does to
each of them, and what `openspec archive` would decline to apply. A delta
inverts most of the rules that make a main spec valid, which is why it is read
by its own grammar rather than by the main one with a flag.

## Requirements

### Requirement: A requirement carries the operation it sits under

The system SHALL give each requirement the operation of the nearest delta
header above it, and SHALL carry an operation outside the four OpenSpec applies
through as written rather than dropping the requirement.

#### Scenario: The four known operations

- **WHEN** a requirement sits under `## ADDED`, `## MODIFIED`, `## REMOVED` or
  `## RENAMED Requirements`
- **THEN** it carries that operation

#### Scenario: Several sections in one file

- **WHEN** a file holds an ADDED section followed by a MODIFIED section
- **THEN** each requirement carries the operation of the section it is in

#### Scenario: An operation heading nobody recognises

- **WHEN** a requirement sits under `## DEPRECATED Requirements`
- **THEN** it is read, and carries `DEPRECATED` as written

#### Scenario: A section header does not leak upward

- **WHEN** a requirement is the last of its section
- **THEN** its body stops at the next operation heading rather than swallowing it

### Requirement: A requirement under no delta header is reported

The system SHALL report a requirement that sits above every delta header,
because `openspec` applies a requirement only under one.

#### Scenario: A requirement before any header

- **WHEN** a `### Requirement:` heading appears before the first delta header
- **THEN** it is reported, with the line it is on
- **AND** the reason names the four headings that would apply it

#### Scenario: A file with no requirements at all

- **WHEN** a delta holds no `### Requirement:` heading
- **THEN** that is reported

### Requirement: A main spec heading in a change is reported

The system SHALL report a `## Requirements` heading in a change, because
OpenSpec reads only delta headers there and nothing under it would be applied.

#### Scenario: The main spec heading

- **WHEN** a change's spec file carries `## Requirements`
- **THEN** it is reported as the heading a main spec uses
- **AND** the reason says nothing under it will be applied when the change is
  archived

### Requirement: Purpose is optional in a delta

The system SHALL accept a delta with no `## Purpose`, that being the ordinary
case, and SHALL report one whose Purpose heading has nothing under it.

#### Scenario: No Purpose

- **WHEN** a delta has no `## Purpose` section
- **THEN** that is not a fault

#### Scenario: A Purpose for a new capability

- **WHEN** a delta carries a Purpose with text
- **THEN** it becomes the first node of the outline

#### Scenario: An empty Purpose heading

- **WHEN** a delta carries a `## Purpose` heading with nothing under it
- **THEN** that is reported, because archive copies it into the new spec

### Requirement: Only an addition or a modification needs a scenario

The system SHALL require at least one scenario of an ADDED or MODIFIED
requirement, and SHALL NOT require one of a REMOVED or RENAMED requirement,
which names a reason or two names instead.

#### Scenario: An added requirement with no scenario

- **WHEN** a requirement under `## ADDED Requirements` has no scenario
- **THEN** that is reported

#### Scenario: A removed requirement with no scenario

- **WHEN** a requirement under `## REMOVED Requirements` has no scenario
- **THEN** that is not a fault

#### Scenario: A renamed requirement with no scenario

- **WHEN** a requirement under `## RENAMED Requirements` has no scenario
- **THEN** that is not a fault

### Requirement: A node knows which delta file it came from

The system SHALL record the capability each node came from and SHALL prefix its
path with it, so that two deltas of one change may touch requirements of the
same name.

#### Scenario: The capability is recorded

- **WHEN** a delta for capability `spec-parsing` is parsed
- **THEN** every node it produced names that capability

#### Scenario: Two deltas naming the same requirement

- **WHEN** two deltas of one change each hold a requirement of the same name
- **THEN** the two requirements have different paths

### Requirement: Scenario content is read as it is in a main spec

The system SHALL read a delta's scenarios by the same rules as a main spec's:
any level-four heading with content under it, fences masked, and content
carried in full and in order.

#### Scenario: The same content rules

- **WHEN** a delta scenario holds clauses, prose and a horizontal rule
- **THEN** they are read exactly as they would be in a main spec

#### Scenario: A scenario heading with nothing under it

- **WHEN** a level-four heading in a delta has no content under it
- **THEN** it is not a scenario

### Requirement: Every real delta in the corpus is readable

The system SHALL parse the delta files of the vendored specgetty project, and
SHALL report a fault only where `openspec` would.

#### Scenario: The corpus parses

- **WHEN** every delta file in the vendored archive is parsed
- **THEN** the great majority parse without a fault, and none raises

#### Scenario: Nothing raises

- **WHEN** any file in the corpus is parsed
- **THEN** no exception escapes the parser
