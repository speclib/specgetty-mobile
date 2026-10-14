## MODIFIED Requirements

### Requirement: The state of a repository is one of five things

The system SHALL represent a repository's project as exactly one of: not loaded,
loading, loaded, no OpenSpec project here, or failed with a reason.

Loading and failed SHALL mean that there is nothing to show. A repository that
holds an index SHALL keep it across a refresh, whatever the refresh does, so
that the states a screen reads never ask it to unshow a project it is already
showing.

#### Scenario: Before anything happens

- **WHEN** a repository has been added but not yet cloned
- **THEN** its state is not loaded

#### Scenario: While cloning

- **WHEN** a clone is in flight, or a refresh of a repository that holds no
  index
- **THEN** the state is loading

#### Scenario: While refreshing something already loaded

- **WHEN** a refresh of a repository that holds an index is in flight
- **THEN** its state is still loaded and carries the index it already had

#### Scenario: After a successful load

- **WHEN** a clone succeeds and the project is read
- **THEN** the state is loaded and carries the index

#### Scenario: A repository with no project

- **WHEN** a clone succeeds but the repository has no `openspec/` at its root
- **THEN** the state says there is no OpenSpec project here

#### Scenario: A failure

- **WHEN** a clone fails, or a refresh of a repository that holds no index fails
- **THEN** the state is failed and carries the error that caused it

#### Scenario: An empty project is loaded, not absent

- **WHEN** a repository holds an `openspec/` directory with nothing in it
- **THEN** the state is loaded, with counts of zero

### Requirement: Refreshing reloads the project

The system SHALL refresh a repository by fetching and resetting to the remote,
then loading the project into a new index.

A refresh SHALL report its outcome separately from the state, so that a failure
can be told without a readable project being taken away to tell it.

#### Scenario: A refresh

- **WHEN** a repository is refreshed
- **THEN** its working copy matches the remote and its index is rebuilt

#### Scenario: A refresh that changes the project

- **WHEN** the remote has gained a spec and a refresh runs
- **THEN** the new index reports the extra spec

#### Scenario: A refresh that fails

- **WHEN** a refresh of a repository that holds no index fails
- **THEN** the state is failed and carries the error that caused it

#### Scenario: A refresh that fails with a project already loaded

- **WHEN** a refresh of a repository that holds an index fails
- **THEN** that index stays current, because fetching and resetting leaves the
  working copy untouched when the fetch is what failed
- **AND** the failure is reported as the outcome of the refresh

#### Scenario: A credential that cannot be renewed

- **WHEN** a refresh of a loaded repository cannot renew its credential, or
  cannot reach GitHub to try
- **THEN** the index stays current and the reason is reported as the outcome,
  the same way a failed fetch is

#### Scenario: Refreshing uses the stored token

- **WHEN** a private repository is refreshed
- **THEN** the token stored when it was added is used

## ADDED Requirements

### Requirement: A refresh belongs to the working copy, not to the screen

The system SHALL run a refresh for as long as it takes, whatever the reader does
in the meantime, and SHALL NOT abandon it because the screen that asked for it
was left. A reader who asks for a repository to be fetched and then navigates
away has not withdrawn the request.

A refresh SHALL be identified by the working copy it fetches rather than by the
repository list entry that asked, so that every entry reading that working copy
reports the same refresh and only one fetch runs for it.

#### Scenario: Leaving the screen that asked

- **WHEN** a refresh is asked for from a screen and that screen is left before
  it lands
- **THEN** the refresh runs to its end and its result is loaded

#### Scenario: Asking twice

- **WHEN** a refresh of a working copy is asked for while one is already in
  flight for it
- **THEN** nothing further is started and the one in flight stands

#### Scenario: Two entries on one working copy

- **WHEN** two repository list entries share a working copy and one of them is
  refreshed
- **THEN** both report the refresh as in flight, one fetch runs, and both are
  reloaded from the result

#### Scenario: What a refresh in flight is

- **WHEN** a refresh is in flight for a working copy
- **THEN** that is readable for every entry on it, separately from each entry's
  state
