# spec-view Specification

## Purpose
Reading one capability's specification: its Purpose, its requirements and their
scenarios, and what is shown when the file does not fit the grammar.

## Requirements

### Requirement: The outline is what the spec contains

The screen SHALL list the spec's Purpose, then each requirement in file order
with its scenarios under it in file order, and SHALL distinguish a requirement
from a scenario by how it is drawn rather than by indentation alone.

#### Scenario: The order

- **WHEN** a spec with several requirements is opened
- **THEN** Purpose comes first, then the requirements in file order, each
  followed by its own scenarios

#### Scenario: The levels

- **WHEN** the outline is drawn
- **THEN** a requirement is drawn differently from a scenario

#### Scenario: A spec with no Purpose section

- **WHEN** a spec cannot be structured because it has no Purpose
- **THEN** the report is shown instead of an outline

### Requirement: Every node has a card

The screen SHALL show the selected node, and what it shows SHALL depend on the
kind of node.

#### Scenario: Purpose

- **WHEN** Purpose is selected
- **THEN** the card shows the purpose text

#### Scenario: A requirement

- **WHEN** a requirement is selected
- **THEN** the card shows the text between its heading and its first scenario,
  and not its scenarios

#### Scenario: A scenario

- **WHEN** a scenario is selected
- **THEN** the card shows its content

### Requirement: A scenario's clauses are laid out where they are recognised

The screen SHALL put a recognised clause's keyword above its text, and SHALL
show content it does not recognise as the prose it is, in the place the file
puts it.

#### Scenario: A recognised clause

- **WHEN** a scenario holds `- **WHEN** something happens`
- **THEN** the keyword is shown above the clause text

#### Scenario: All four keywords

- **WHEN** a scenario holds GIVEN, WHEN, THEN and AND clauses
- **THEN** each is laid out with its own keyword

#### Scenario: Content in a shape the card does not lay out

- **WHEN** a scenario's content is a paragraph, or clauses written without
  bullets
- **THEN** it is shown as prose rather than omitted

#### Scenario: Clauses and prose together

- **WHEN** a scenario holds both
- **THEN** both are shown, in the order the file gives them

### Requirement: A file that does not fit the grammar still opens

The screen SHALL open whether or not the file can be structured. When it cannot,
the screen SHALL report every reason it found, each with its line, and SHALL
keep the whole file reachable as Markdown.

#### Scenario: Opening a file that does not fit

- **WHEN** a spec that cannot be structured is opened
- **THEN** the screen opens and says why

#### Scenario: Every reason is given

- **WHEN** a file has more than one fault
- **THEN** each is listed, not only the first

#### Scenario: A reason names its line

- **WHEN** a reason concerns a particular line
- **THEN** the line number is shown

#### Scenario: The file is still readable

- **WHEN** a spec cannot be structured
- **THEN** its whole text is still reachable as Markdown from the same screen

#### Scenario: A file that cannot be read at all

- **WHEN** the spec file cannot be read from disk
- **THEN** the screen says so rather than showing an empty outline

### Requirement: The spec is parsed once per reading

The system SHALL parse the spec when the screen opens and SHALL reuse that
result while the file is unchanged.

#### Scenario: Opening

- **WHEN** the spec screen opens
- **THEN** the spec is parsed

#### Scenario: Returning to it

- **WHEN** the screen is left and the same spec opened again
- **THEN** it is not parsed a second time

### Requirement: The spec screen is reached and left

The screen SHALL be opened from the project's Specs tab and SHALL return to it,
and selecting a node SHALL be a level the back action returns from.

#### Scenario: Opening a spec

- **WHEN** a capability is opened from the Specs tab
- **THEN** its spec screen is shown, named after the capability

#### Scenario: Going back from a card

- **WHEN** a node's card is showing and the back action is used
- **THEN** the outline is shown again

#### Scenario: Going back from the outline

- **WHEN** the outline is showing and the back action is used
- **THEN** the project view is shown again
