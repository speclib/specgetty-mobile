## Purpose

The journeys that have to work on a real device, and the states that have to be
readable there. This is the only place the app is exercised without anything
replaced.

## ADDED Requirements

### Requirement: The suite runs on the minimum supported Android

The system SHALL run its instrumented tests on an API 26 emulator, that being
the minimum version the app supports.

#### Scenario: The emulator

- **WHEN** the instrumented suite runs
- **THEN** it runs against API 26

#### Scenario: Desugared APIs

- **WHEN** the app clones a repository on API 26
- **THEN** the `java.nio.file` and `java.time` APIs JGit uses are present

#### Scenario: Another project's emulator is attached

- **WHEN** an emulator belonging to something else is attached to the same adb
  server
- **THEN** the suite targets its own by name and leaves the other alone

#### Scenario: An emulator already running

- **WHEN** the suite is run and an emulator of ours is already up
- **THEN** it is used, and it is not stopped afterwards

### Requirement: A repository can be added and its statistics appear

The system SHALL clone a repository entered into the add form and SHALL show
the project's statistics on its row.

#### Scenario: Adding

- **WHEN** an HTTPS URL is entered and confirmed
- **THEN** the repository is cloned and its row shows the spec count, the change
  counts and the task progress

#### Scenario: The clone is a real one

- **WHEN** the repository is served over HTTP on loopback
- **THEN** it is cloned over the git protocol rather than copied

### Requirement: The changes of a project can be browsed

The system SHALL show a project's changes and open one.

#### Scenario: The list

- **WHEN** the Changes tab is opened
- **THEN** each change is listed with its task progress

#### Scenario: Opening one

- **WHEN** a change is opened
- **THEN** its artifacts, its tasks and the capabilities it touches are reachable

#### Scenario: The tasks

- **WHEN** the tasks tab of a change is opened
- **THEN** the completion figure and each task are shown

### Requirement: A spec delta can be opened and compared

The system SHALL open a change's delta and, for a change not yet archived, show
the difference against the requirement it modifies.

#### Scenario: The delta outline

- **WHEN** a change's capability is opened
- **THEN** its requirements are listed with their operations

#### Scenario: The comparison

- **WHEN** a modified requirement is selected
- **THEN** the difference, the original and the proposed are each offered

#### Scenario: Both sides are present

- **WHEN** the difference is shown
- **THEN** the line only in the original and the line only in the proposed are
  both on screen

### Requirement: A spec can be opened and read

The system SHALL open a capability's spec and show its outline and its cards.

#### Scenario: The outline

- **WHEN** a capability is opened from the Specs tab
- **THEN** its Purpose and its requirements are listed

#### Scenario: A scenario's card

- **WHEN** a scenario is selected
- **THEN** its clauses are shown

### Requirement: The states that have to be readable are readable

The system SHALL show, on a device, the three outcomes `BRIEFING.md` requires to
be told apart.

#### Scenario: No OpenSpec project

- **WHEN** a repository with no `openspec/` at its root is added
- **THEN** the row says there is no OpenSpec project here

#### Scenario: A network failure

- **WHEN** a repository whose host cannot be reached is added
- **THEN** the row says it could not be reached

#### Scenario: A URL that will not do

- **WHEN** an SSH URL is entered and confirmed
- **THEN** it is refused with a reason, and nothing is cloned
