# spec-parsing Specification

## Purpose
Reading a `spec.md` into the structure it already has, by the rules OpenSpec's
own parser uses, and reporting every reason a file does not fit them. This is
the only code in the app that knows the spec grammar.

## Requirements

### Requirement: The grammar is OpenSpec's, and nothing is added to it

The system SHALL read a main spec by these rules and SHALL NOT add a rule of its
own: requirements live under a `## Requirements` heading; a requirement is a
`### Requirement: <name>` heading inside that section; a scenario is any
level-four heading with content under it; a heading inside a fenced code block
is not a heading; and a delta header in a main spec is a fault.

#### Scenario: A heading inside a fenced code block

- **WHEN** a fenced block contains a line beginning with `#`
- **THEN** that line is content, not a heading, and does not end the node it
  sits in
- **AND** the fenced text is still part of the scenario's content

#### Scenario: A scenario heading without the Scenario prefix

- **WHEN** a level-four heading's text does not begin with `Scenario:`
- **THEN** it is a scenario, named by its heading text

#### Scenario: A scenario heading with nothing under it

- **WHEN** a level-four heading has no content before the next heading
- **THEN** it is not a scenario

#### Scenario: Headings are matched without regard to case

- **WHEN** the headings read `## purpose`, `## REQUIREMENTS` or
  `### requirement: x`
- **THEN** they are read as the grammar's headings

#### Scenario: A heading that merely resembles a requirement

- **WHEN** a heading reads `### Requirements of a kind`
- **THEN** it is not a requirement, because it is not `### Requirement:`

#### Scenario: A closing hash run in a heading

- **WHEN** a scenario heading is written `#### Edge case ####`
- **THEN** its name is `Edge case`

### Requirement: The outline is Purpose, requirements and their scenarios

The system SHALL produce an outline holding the Purpose first when there is one,
then each requirement in file order with its own scenarios under it in file
order.

#### Scenario: File order is kept

- **WHEN** a spec with several requirements is parsed
- **THEN** the outline lists Purpose, then the requirements in the order the
  file gives them, each followed by its scenarios in file order

#### Scenario: A requirement's own prose

- **WHEN** a requirement is parsed
- **THEN** its body is the text between its heading and its first scenario, and
  does not include its scenarios

#### Scenario: A node keeps its identity across a re-parse

- **WHEN** the same spec is parsed twice
- **THEN** each node carries the same path both times

#### Scenario: Two requirements naming the same scenario

- **WHEN** two requirements each hold a scenario of the same name
- **THEN** their paths differ, because a scenario's path carries its requirement

### Requirement: A scenario's content is never dropped

The system SHALL carry a scenario's content in full and in the order the file
gives it, and SHALL NOT discard a line for being written in a shape the clause
reader does not recognise.

#### Scenario: Clauses written without bullets

- **WHEN** a scenario's lines read `GIVEN ...`, `WHEN ...` and `THEN ...` with
  no list markers
- **THEN** every one of those lines is in the parsed content

#### Scenario: A scenario written as prose

- **WHEN** a scenario's content is a paragraph carrying no keyword
- **THEN** that paragraph is in the parsed content as a single prose part

#### Scenario: Clauses and prose together

- **WHEN** a scenario holds keyword clauses with a paragraph between them
- **THEN** all three are present, in the order the file gives them

#### Scenario: A separator is not content

- **WHEN** a scenario's content includes a horizontal rule
- **THEN** the rule is left out, being a separator rather than behaviour
- **AND** the requirement after it is still parsed

#### Scenario: A clause spanning several source lines

- **WHEN** a clause's text runs across more than one source line
- **THEN** those lines are joined into one clause

#### Scenario: No scenario is empty

- **WHEN** any scenario in a parseable spec is read
- **THEN** it has at least one part

### Requirement: A clause is a bulleted upper-case keyword

The system SHALL recognise a clause only as a `- ` bullet opening with `GIVEN`,
`WHEN`, `THEN` or `AND`, in upper case, with or without bold marks. Everything
else SHALL be carried as prose.

#### Scenario: The shapes that are clauses

- **WHEN** a line reads `- **WHEN** a thing` or `- WHEN a thing`
- **THEN** it is a clause whose keyword is `WHEN`

#### Scenario: The shapes that are not

- **WHEN** a line reads `- **When** a thing`, `**WHEN** a thing`, `WHEN a thing`
  or `- something else`
- **THEN** it is not a clause

#### Scenario: All four keywords

- **WHEN** a scenario holds a `GIVEN`, a `WHEN`, a `THEN` and an `AND` bullet
- **THEN** all four are read as clauses, each with its own keyword

#### Scenario: Title case bold clauses are prose

- **WHEN** a scenario's lines read `**Given** ...`, `**When** ...` and
  `**Then** ...`
- **THEN** they are carried as prose rather than laid out as clauses
- **AND** none of their text is lost

### Requirement: A file that does not fit is reported in full

The system SHALL report every reason a file is not a spec rather than the first,
SHALL give the line for a reason that concerns one, and SHALL produce no outline
for such a file.

#### Scenario: No Purpose

- **WHEN** a file has no `## Purpose` section
- **THEN** that is reported

#### Scenario: An empty Purpose

- **WHEN** a file's `## Purpose` section has no text under it
- **THEN** that is reported

#### Scenario: A delta header in a main spec

- **WHEN** a main spec's requirements sit under `## ADDED Requirements`, or
  `## MODIFIED`, `## REMOVED` or `## RENAMED`
- **THEN** it is reported as a delta header belonging in a change
- **AND** the reason names the line it is on

#### Scenario: No requirements section

- **WHEN** a file has no `## Requirements` heading
- **THEN** that is reported

#### Scenario: Requirements outside the section

- **WHEN** `### Requirement:` headings sit outside the `## Requirements` section
- **THEN** that is reported, with the lines they are on

#### Scenario: A requirement with no scenario

- **WHEN** a requirement has no level-four heading with content under it
- **THEN** that is reported, with the line the requirement is on

#### Scenario: No outline for a file that does not fit

- **WHEN** a file is reported as not a spec
- **THEN** no outline is produced for it

#### Scenario: Several faults at once

- **WHEN** a file has more than one fault
- **THEN** every fault is reported, not only the first
