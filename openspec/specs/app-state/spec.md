# app-state Specification

## Purpose
What the app is currently showing, what it is doing, and what went wrong. This
capability owns the transitions between those, so that a screen reads a state
rather than deducing one from a handful of flags.

## Requirements

### Requirement: The state of a repository is one of five things

The system SHALL represent a repository's project as exactly one of: not loaded,
loading, loaded, no OpenSpec project here, or failed with a reason.

#### Scenario: Before anything happens

- **WHEN** a repository has been added but not yet cloned
- **THEN** its state is not loaded

#### Scenario: While cloning

- **WHEN** a clone or a refresh is in flight
- **THEN** the state is loading

#### Scenario: After a successful load

- **WHEN** a clone succeeds and the project is read
- **THEN** the state is loaded and carries the index

#### Scenario: A repository with no project

- **WHEN** a clone succeeds but the repository has no `openspec/` at its root
- **THEN** the state says there is no OpenSpec project here

#### Scenario: A failure

- **WHEN** a clone or refresh fails
- **THEN** the state is failed and carries the error that caused it

#### Scenario: An empty project is loaded, not absent

- **WHEN** a repository holds an `openspec/` directory with nothing in it
- **THEN** the state is loaded, with counts of zero

### Requirement: The three failures are told apart

The system SHALL carry an authentication failure, a network failure and a
repository that is not an OpenSpec project as distinct states, so a screen can
say which happened.

#### Scenario: A rejected token

- **WHEN** the remote refuses the credentials
- **THEN** the state is failed with an authentication error

#### Scenario: An unreachable host

- **WHEN** the host cannot be reached
- **THEN** the state is failed with a network error

#### Scenario: Not an OpenSpec project

- **WHEN** the clone succeeds and there is no `openspec/`
- **THEN** the state is not a failure but its own outcome

### Requirement: Adding a repository clones it and loads the project

The system SHALL add a repository to the list, clone it, and load its project,
reporting the state at each step.

#### Scenario: Adding

- **WHEN** a repository is added
- **THEN** it appears in the list, is cloned, and its project is loaded

#### Scenario: The state while it happens

- **WHEN** an add is in flight
- **THEN** the state for that repository is loading

#### Scenario: Adding with a token

- **WHEN** a repository is added with a token
- **THEN** the token is used for the clone and stored for later refreshes

#### Scenario: A clone that fails

- **WHEN** the clone fails
- **THEN** the repository stays in the list, in a failed state, so it can be
  retried or removed

### Requirement: Refreshing reloads the project

The system SHALL refresh a repository by fetching and resetting to the remote,
then loading the project into a new index.

#### Scenario: A refresh

- **WHEN** a repository is refreshed
- **THEN** its working copy matches the remote and its index is rebuilt

#### Scenario: A refresh that changes the project

- **WHEN** the remote has gained a spec and a refresh runs
- **THEN** the new index reports the extra spec

#### Scenario: A refresh that fails

- **WHEN** a refresh fails
- **THEN** the state is failed and the previously loaded index is not shown as
  current

#### Scenario: Refreshing uses the stored token

- **WHEN** a private repository is refreshed
- **THEN** the token stored when it was added is used

### Requirement: Removing a repository forgets everything about it

The system SHALL delete a removed repository's working copy, its entry in the
list, its token and its state.

#### Scenario: Removing

- **WHEN** a repository is removed
- **THEN** its working copy is gone, it is out of the list, and it has no state

#### Scenario: Removing the active repository

- **WHEN** the active repository is removed and another remains
- **THEN** another becomes active

### Requirement: Switching changes what is shown

The system SHALL switch the active repository, and SHALL load its project if it
has not been loaded in this session.

#### Scenario: Switching to a loaded repository

- **WHEN** the active repository is switched to one already loaded
- **THEN** its index is shown without cloning again

#### Scenario: Switching to one not yet loaded

- **WHEN** the active repository is switched to one cloned in an earlier session
- **THEN** its project is loaded from the working copy on disk

### Requirement: A spec is parsed when it is opened, and then kept

The system SHALL NOT parse a spec or a delta until it is asked for, and SHALL
keep the result so that reopening it does not parse it again.

#### Scenario: Loading a project parses nothing

- **WHEN** a project of 28 specs is loaded
- **THEN** no spec has been parsed

#### Scenario: Opening a spec parses it

- **WHEN** a spec is opened
- **THEN** it is parsed once

#### Scenario: Reopening it

- **WHEN** the same spec is opened again
- **THEN** it is not parsed a second time

#### Scenario: A file that changed

- **WHEN** a refresh rewrote a spec and it is opened again
- **THEN** it is parsed again, the cached result being for the earlier content

#### Scenario: A file that did not change

- **WHEN** a refresh left a spec alone and it is opened again
- **THEN** the cached result is used
