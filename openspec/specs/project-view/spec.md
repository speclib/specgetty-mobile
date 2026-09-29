# project-view Specification

## Purpose
Reading one repository's OpenSpec project: what it contains, what is being
worked on, what has been done, and how it is configured.

## Requirements

### Requirement: The header names the project and its size

The screen SHALL show the repository's name and its statistics above the tabs,
and SHALL keep them visible whichever tab is open.

#### Scenario: The header

- **WHEN** the project view is open
- **THEN** the repository name, the spec count, the active and archived change
  counts and the open tasks are shown

#### Scenario: Switching tabs

- **WHEN** another tab is selected
- **THEN** the header is unchanged

### Requirement: The Overview tab summarises the project

The Overview tab SHALL show the counts, the active changes by name, and the
recently archived changes with their dates.

#### Scenario: Active changes

- **WHEN** the Overview tab is open
- **THEN** each active change is listed by name

#### Scenario: Recently archived

- **WHEN** the Overview tab is open
- **THEN** the most recently archived changes are listed with their dates,
  newest first

#### Scenario: A project with nothing in it

- **WHEN** the project holds no specs and no changes
- **THEN** the tab says the project is empty rather than showing empty lists

### Requirement: The Changes tab is one grouped list

The Changes tab SHALL show one list grouped into Active and Archived, each row
carrying the change's name, its task progress, the number of specs it touches
and, for an archived change, its date.

#### Scenario: The groups

- **WHEN** the Changes tab is open
- **THEN** active changes are under an Active heading and archived ones under an
  Archived heading

#### Scenario: A row

- **WHEN** a change row is drawn
- **THEN** it shows the name, the task progress as `done/total`, and the number
  of specs the change touches

#### Scenario: An archived row

- **WHEN** an archived change row is drawn
- **THEN** it also shows the date it was archived

#### Scenario: A change with no tasks

- **WHEN** a change holds no tasks
- **THEN** its row shows no task figure rather than `0/0`

#### Scenario: Ordering

- **WHEN** the list is drawn
- **THEN** active changes are in name order and archived ones newest first

### Requirement: The Changes tab is searched by the grammar change-search defines

The Changes tab SHALL narrow its list by a query read with the same grammar as
specgetty: fuzzy on names by default, a literal name match after `'`, and a
literal match in artifact and spec text after `:`.

#### Scenario: Typing a name

- **WHEN** a query with no sigil is typed
- **THEN** the list narrows to changes whose names match it loosely, best first

#### Scenario: Searching the text

- **WHEN** a query beginning with `:` is typed
- **THEN** the list narrows to changes whose artifacts or specs contain it

#### Scenario: A row that matched on text

- **WHEN** a change matched through the text of its files
- **THEN** the row names the files it matched in

#### Scenario: The sigils can be inserted rather than typed

- **WHEN** the person uses the control beside the search field
- **THEN** the query is rewritten to carry the chosen sigil, the grammar being
  unchanged

#### Scenario: A query that matches nothing

- **WHEN** a query matches no change
- **THEN** the tab says so and repeats the query back

#### Scenario: Clearing the query

- **WHEN** the query is cleared
- **THEN** the full list returns in its normal order

### Requirement: The Specs tab lists the capabilities

The Specs tab SHALL list the project's capabilities by name, in name order.

#### Scenario: The list

- **WHEN** the Specs tab is open
- **THEN** every capability under `openspec/specs/` is listed by name

#### Scenario: No specs

- **WHEN** the project has no specs
- **THEN** the tab says so

#### Scenario: Nothing is parsed by listing

- **WHEN** the Specs tab is opened
- **THEN** no spec file has been parsed

### Requirement: The Properties tab shows the configuration

The Properties tab SHALL show `project.md` rendered as Markdown when the project
has one, and otherwise the configuration file with its YAML legible, and SHALL
list one row per workflow schema the changes use.

#### Scenario: A project description

- **WHEN** the project holds `project.md`
- **THEN** it is rendered as Markdown

#### Scenario: No description

- **WHEN** the project holds no `project.md`
- **THEN** the configuration file is shown instead

#### Scenario: Neither

- **WHEN** the project holds neither
- **THEN** the tab says there is no configuration rather than showing nothing

#### Scenario: The schemas in use

- **WHEN** the changes use `spec-driven` and `tinychange`
- **THEN** both are listed, once each

#### Scenario: A change with no schema

- **WHEN** a change has no `.openspec.yaml`
- **THEN** it contributes no schema row

### Requirement: The project view is reached from the list and left again

The screen SHALL be opened from a loaded repository's row and SHALL return to
the list.

#### Scenario: Opening

- **WHEN** a loaded repository is opened
- **THEN** its project view is shown

#### Scenario: Going back

- **WHEN** the back action is used
- **THEN** the repository list is shown again

#### Scenario: A repository that is not loaded

- **WHEN** a repository whose project failed to load is opened
- **THEN** the screen says why rather than showing an empty project

### Requirement: A tab label is legible whatever the screen

The system SHALL show each tab's label in full on one line, and SHALL NOT break
a label across lines or cut a word in half, whatever the width of the screen or
the size of the reader's type.

#### Scenario: A narrow screen

- **WHEN** the four tabs do not fit side by side at their natural widths
- **THEN** the row scrolls rather than squeezing a label into a column narrower
  than the word

#### Scenario: A label is never broken mid-word

- **WHEN** any tab is drawn
- **THEN** its label is on one line, and no word in it is split

#### Scenario: Large type

- **WHEN** the reader has set a larger system font size
- **THEN** the labels are still whole

#### Scenario: A wide screen

- **WHEN** the tabs fit comfortably
- **THEN** they are shown as they were
