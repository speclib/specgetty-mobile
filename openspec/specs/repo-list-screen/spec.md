# repo-list-screen Specification

## Purpose
The first screen: every repository the app knows about, what its OpenSpec
project contains, and the means to add one, remove one or switch to another.

## Requirements

### Requirement: Every repository shows its statistics

The screen SHALL show, for each repository, its label, its spec count, its
active and archived change counts, and its open tasks as `done/total`.

#### Scenario: A loaded repository

- **WHEN** a repository's project has loaded
- **THEN** its row shows the spec count, the active and archived change counts
  and the open tasks

#### Scenario: A repository with no open tasks

- **WHEN** a repository's active changes hold no tasks
- **THEN** the row shows no task figure rather than `0/0`

#### Scenario: A repository still loading

- **WHEN** a repository's project has not finished loading
- **THEN** its row says so in place of the statistics

### Requirement: A repository is added by URL, with an optional token

The screen SHALL take a clone URL and an optional access token, SHALL validate
the URL before anything is attempted, and SHALL NOT add anything until the
person confirms. The URL MAY be typed, pasted, scanned from a QR code or shared
in from another application, and every one of those SHALL end at the same form,
filled in and editable.

When the repository holds more than one project, confirming SHALL present those
projects for the person to choose from, and SHALL add only the chosen ones. When
it holds one, confirming SHALL add it with no further question.

#### Scenario: Adding a public repository

- **WHEN** an HTTPS URL is entered and confirmed
- **THEN** the repository is added and cloned

#### Scenario: Adding a private repository

- **WHEN** a URL and a token are entered and confirmed
- **THEN** the token is used for the clone and stored

#### Scenario: A repository holding one project

- **WHEN** a repository holding a single project is confirmed
- **THEN** it is added directly, with nothing further to choose

#### Scenario: A repository holding several projects

- **WHEN** a repository holding four projects is confirmed
- **THEN** the four are offered by name
- **AND** nothing has been added to the list yet

#### Scenario: Choosing some of them

- **WHEN** two of the offered projects are chosen and confirmed
- **THEN** the list holds two entries for that repository

#### Scenario: Choosing none of them

- **WHEN** the choice is abandoned
- **THEN** nothing is added to the list

#### Scenario: An invalid URL

- **WHEN** the URL is empty, an SSH address, or not a web address
- **THEN** the reason is shown and nothing is added

#### Scenario: Nothing happens before confirming

- **WHEN** a URL has been typed but not confirmed
- **THEN** no repository has been added and nothing has been cloned

#### Scenario: The form can be opened already filled in

- **WHEN** the form is opened with a URL supplied from outside the screen
- **THEN** that URL is in the field, editable, and nothing has been added

#### Scenario: A scanned code

- **WHEN** a QR code holding a repository URL is scanned
- **THEN** the add form opens with that URL filled in
- **AND** no repository has been added

#### Scenario: A shared URL

- **WHEN** a URL is shared into the app from another application
- **THEN** the add form opens with that URL filled in
- **AND** no repository has been added

#### Scenario: Correcting a captured URL

- **WHEN** the normaliser produced the wrong URL
- **THEN** the field can be edited before confirming

#### Scenario: The form is cleared after a successful add

- **WHEN** a repository has been added
- **THEN** the form is empty the next time it is opened

#### Scenario: A token is never shown back

- **WHEN** the form is reopened for a repository that has a token
- **THEN** the token field is empty rather than showing the stored token

#### Scenario: A token is never taken from a capture

- **WHEN** a captured URL carries embedded credentials or a token parameter
- **THEN** the token field stays empty and the credentials are not stored

#### Scenario: Authorizing is offered where it works

- **WHEN** the URL in the form is one the app can authorize for
- **THEN** the form offers to authorize as well as to type a token

#### Scenario: Authorizing is not offered where it does not

- **WHEN** the URL is for a host the app cannot authorize for
- **THEN** only typing is offered, with no mention of a feature that will not
  work here

#### Scenario: A credential that was authorized

- **WHEN** authorization succeeds for the URL in the form
- **THEN** the form shows that a credential is held, without showing it
- **AND** nothing is added until the person confirms

#### Scenario: Abandoning the authorization

- **WHEN** the person starts authorizing and backs out
- **THEN** the form is as it was, and nothing has been added or stored

#### Scenario: Neither a token nor an authorization

- **WHEN** a private repository is added with no credential at all
- **THEN** it fails as it does today, saying the authentication failed

### Requirement: The list can be refreshed

The screen SHALL refresh a repository on a pull-to-refresh gesture, fetching and
resetting to the remote and reloading the project.

#### Scenario: Pulling to refresh

- **WHEN** the person pulls the list down
- **THEN** the repositories are refreshed and their statistics are recomputed

#### Scenario: While refreshing

- **WHEN** a refresh is in flight
- **THEN** the screen shows that it is happening

### Requirement: A repository can be removed and switched to

The screen SHALL let a repository be removed, asking first, and SHALL let
another be made active.

#### Scenario: Removing

- **WHEN** a repository is removed and the removal is confirmed
- **THEN** it leaves the list and its working copy is deleted

#### Scenario: Removing is confirmed first

- **WHEN** removal is chosen but not confirmed
- **THEN** the repository is still there

#### Scenario: Switching

- **WHEN** another repository is chosen
- **THEN** it becomes the active one

#### Scenario: Opening a repository

- **WHEN** a loaded repository's row is opened
- **THEN** the project view for it is shown

### Requirement: Empty, loading and error states are readable

The screen SHALL distinguish having no repositories, loading one, a repository
whose clone failed, and a repository that holds no OpenSpec project.

#### Scenario: No repositories at all

- **WHEN** nothing has been added
- **THEN** the screen says so and offers to add one

#### Scenario: An authentication failure

- **WHEN** a clone failed because the token was refused
- **THEN** the row says the authentication failed

#### Scenario: A network failure

- **WHEN** a clone failed because the host could not be reached
- **THEN** the row says the repository could not be reached

#### Scenario: No OpenSpec project in the repository

- **WHEN** a clone succeeded and there is no `openspec/` at the root
- **THEN** the row says there is no OpenSpec project here

#### Scenario: An empty project is not an error

- **WHEN** a repository holds an `openspec/` directory with nothing in it
- **THEN** the row shows counts of zero rather than an error

#### Scenario: A failed repository can be retried

- **WHEN** a repository is in a failed state
- **THEN** it can be refreshed again without being removed and re-added

### Requirement: A row says which project in a repository it is

A row for a project held in a subdirectory SHALL show that directory's name and
the repository it came from. A row for a project at a repository root SHALL be
unchanged.

#### Scenario: A project in a subdirectory

- **WHEN** a row for the project at `nivis` within `nivis-openspec-stores` is
  shown
- **THEN** it names both

#### Scenario: A project at the root

- **WHEN** a row for a project at a repository root is shown
- **THEN** it is named as before, with no path

#### Scenario: Two rows on one repository

- **WHEN** two projects from one repository are in the list
- **THEN** each row carries its own statistics rather than the repository's
  combined total
