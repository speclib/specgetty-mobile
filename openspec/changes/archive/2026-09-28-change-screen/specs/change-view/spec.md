## Purpose

Reading one change: the artifact files it actually has, how far its tasks have
got, and which capabilities it touches.

## ADDED Requirements

### Requirement: There is a tab per artifact file, plus a Specs tab

The screen SHALL show one tab per markdown file in the change directory, in a
stable order, and one further tab for the capabilities the change touches.

#### Scenario: The usual change

- **WHEN** a change holds `proposal.md`, `design.md` and `tasks.md`
- **THEN** there are three artifact tabs and a Specs tab

#### Scenario: A change with one artifact

- **WHEN** a change holds only `proposal.md`
- **THEN** there is one artifact tab and a Specs tab

#### Scenario: An artifact nobody expected

- **WHEN** a change holds `notes.md`
- **THEN** it gets a tab like any other

#### Scenario: The tab order does not shift

- **WHEN** the change is opened twice
- **THEN** the tabs are in the same order both times

#### Scenario: A tab is named by its file

- **WHEN** an artifact tab is drawn
- **THEN** it is named after the file, without the `.md`

### Requirement: An artifact renders as Markdown

The screen SHALL render an artifact's text as Markdown, and SHALL read it when
its tab is opened rather than when the change is opened.

#### Scenario: Reading an artifact

- **WHEN** an artifact tab is selected
- **THEN** its file is read and rendered as Markdown

#### Scenario: Nothing is read up front

- **WHEN** the change is opened
- **THEN** no artifact beyond the first tab's has been read

#### Scenario: A file that cannot be read

- **WHEN** an artifact file cannot be read
- **THEN** the tab says so rather than showing an empty document

### Requirement: The tasks tab shows progress and draws boxes

The tasks tab SHALL show the change's completion as `done/total`, and SHALL draw
each task with a box that is filled when the task is done.

#### Scenario: The statistics

- **WHEN** a change's tasks are three done of five
- **THEN** the tasks tab says 3 of 5

#### Scenario: The boxes

- **WHEN** the tasks are drawn
- **THEN** each carries a box, filled for a done task and empty for one not done

#### Scenario: What counts as a task

- **WHEN** a line is an indented checkbox, or uses another list marker
- **THEN** it is not drawn as a task, because it is not counted as one either

#### Scenario: A task's continuation lines

- **WHEN** a task wraps onto indented lines
- **THEN** they are shown with it rather than as separate tasks

#### Scenario: The file's own headings

- **WHEN** the tasks file groups its tasks under headings
- **THEN** the headings are shown

#### Scenario: A change with no tasks file

- **WHEN** a change has no `tasks.md`
- **THEN** there is no tasks tab

#### Scenario: The boxes are not tappable

- **WHEN** a task's box is touched
- **THEN** nothing changes, in the file or on screen

### Requirement: The Specs tab lists the capabilities the change touches

The Specs tab SHALL list each capability the change has a delta for, and SHALL
open that delta.

#### Scenario: A change touching two capabilities

- **WHEN** the Specs tab is open for a change with two deltas
- **THEN** both capabilities are listed

#### Scenario: A change touching none

- **WHEN** the change has no `specs/` directory
- **THEN** the tab says the change touches no capability

#### Scenario: Opening a delta

- **WHEN** a capability is selected
- **THEN** the delta view for it opens

#### Scenario: Nothing is parsed by listing

- **WHEN** the Specs tab is opened
- **THEN** no delta file has been parsed

### Requirement: The change screen is reached and left

The screen SHALL be opened from a change row and SHALL return to the project
view.

#### Scenario: Opening from the Changes tab

- **WHEN** a change row is opened
- **THEN** that change's screen is shown, named after the change

#### Scenario: Going back

- **WHEN** the back action is used
- **THEN** the project view is shown again

#### Scenario: An archived change

- **WHEN** an archived change is opened
- **THEN** it is shown the same way, with its date
