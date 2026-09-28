# delta-view Specification

## Purpose
Reading the spec deltas of one change: what each requirement does to the
capability it belongs to, and how a modified requirement differs from the one it
modifies.

## Requirements

### Requirement: The outline covers every delta file of the change

The screen SHALL show one section per capability the change touches, and under
each the requirements of that delta with their scenarios.

#### Scenario: A change touching two capabilities

- **WHEN** a change holds deltas for two capabilities
- **THEN** both appear, each with its own requirements

#### Scenario: The order

- **WHEN** the outline is drawn
- **THEN** capabilities are in name order and requirements in file order

#### Scenario: A delta that does not fit the grammar

- **WHEN** a delta file cannot be structured
- **THEN** its section says why, with the line for each reason, rather than
  being omitted

#### Scenario: A change with no deltas

- **WHEN** the change touches no capability
- **THEN** the screen says so

### Requirement: Every requirement carries its operation

The screen SHALL mark each requirement with the operation it sits under, and
SHALL show an operation outside the four OpenSpec applies as written.

#### Scenario: The four operations

- **WHEN** requirements under ADDED, MODIFIED, REMOVED and RENAMED are drawn
- **THEN** each carries its own mark

#### Scenario: An unrecognised operation

- **WHEN** a requirement sits under a heading none of the four match
- **THEN** it is marked with what the heading said

### Requirement: A node opens its card

The screen SHALL show the selected node's detail: a requirement's prose, or a
scenario's clauses laid out where they are recognised and its raw text where
they are not.

#### Scenario: A requirement

- **WHEN** a requirement node is selected
- **THEN** the card shows its prose and not its scenarios

#### Scenario: A scenario

- **WHEN** a scenario node is selected
- **THEN** the card shows its clauses, each keyword with its text

#### Scenario: A scenario written as prose

- **WHEN** a scenario's content carries no recognised clause
- **THEN** the card shows the text as it is written

#### Scenario: The Purpose of a new capability

- **WHEN** a delta carries a Purpose and it is selected
- **THEN** the card shows it

### Requirement: A modified requirement can be compared with its original

For an active change, the system SHALL find the requirement of the same name in
`openspec/specs/<capability>/spec.md` and SHALL offer a difference, the original
and the proposed, opening on the difference.

#### Scenario: A modified requirement with an original

- **WHEN** a MODIFIED requirement's name matches one in the main spec
- **THEN** a comparison is offered, showing the difference first

#### Scenario: The three views

- **WHEN** a comparison is offered
- **THEN** the difference, the original and the proposed are each reachable

#### Scenario: The difference

- **WHEN** the difference is shown
- **THEN** lines only in the original and lines only in the proposed are
  distinguishable from lines in both

#### Scenario: An added requirement

- **WHEN** an ADDED requirement is selected
- **THEN** no comparison is offered, there being nothing to compare with

#### Scenario: A requirement whose original cannot be found

- **WHEN** a MODIFIED requirement names one the main spec does not hold
- **THEN** no comparison is offered rather than an empty one

#### Scenario: A capability the main spec does not have yet

- **WHEN** the change introduces the capability
- **THEN** no comparison is offered

### Requirement: An archived change offers no comparison

The system SHALL NOT offer a comparison for a requirement of an archived change.

#### Scenario: An archived modified requirement

- **WHEN** a MODIFIED requirement of an archived change is selected
- **THEN** no comparison is offered

#### Scenario: Why it is not offered

- **WHEN** an archived change is read
- **THEN** the main spec already holds the proposed text, so a difference would
  be empty and would imply the change did nothing

### Requirement: A requirement's text is sliced by the same grammar

The system SHALL take a requirement's source text from its heading up to the
next requirement or section heading, reading headings by the same rules the
parsers use.

#### Scenario: The slice

- **WHEN** a requirement's text is taken
- **THEN** it runs from its own heading to the next requirement or section
  heading

#### Scenario: A heading inside a fenced block

- **WHEN** a requirement's text contains a fenced block holding a line that
  looks like a heading
- **THEN** the slice does not end there

#### Scenario: The last requirement

- **WHEN** the requirement is the last in the file
- **THEN** the slice runs to the end of the file

#### Scenario: A name that is not there

- **WHEN** no requirement of that name is in the file
- **THEN** nothing is returned
