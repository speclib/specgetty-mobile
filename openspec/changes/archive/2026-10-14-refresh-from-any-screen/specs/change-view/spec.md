## ADDED Requirements

### Requirement: The change screen can be pulled to refresh

The screen SHALL refresh the repository it is reading on a pull-to-refresh
gesture, and SHALL keep the open tab and the place in it while the refresh runs.

#### Scenario: Pulling to refresh

- **WHEN** the person pulls the change screen down
- **THEN** the repository is fetched and reset to the remote and the project is
  loaded again

#### Scenario: While it runs

- **WHEN** a refresh is in flight
- **THEN** the screen says so, and goes on showing the artifact that was open

#### Scenario: A refresh that failed

- **WHEN** a refresh fails
- **THEN** the artifact stays on the screen and the reason is told alongside it

#### Scenario: Pulling a tab with nothing to scroll

- **WHEN** a short artifact or an empty Specs tab is pulled down
- **THEN** the refresh happens all the same

### Requirement: An artifact is re-read when a refresh rewrites it

The screen SHALL show the artifact that is on disk now. An artifact read once
and kept SHALL NOT outlive a refresh of the repository it was read from, since
the refresh may have replaced every word of it.

An artifact left alone by a refresh SHALL NOT have to be read again, so that
refreshing a change with a long design document costs no more than it has to.

#### Scenario: An artifact the refresh rewrote

- **WHEN** a tab is open, a refresh lands, and the remote had rewritten that
  artifact
- **THEN** the tab shows the new text

#### Scenario: An artifact the refresh left alone

- **WHEN** a refresh lands and an artifact is unchanged
- **THEN** reopening its tab does not read the file again

#### Scenario: An artifact the refresh removed

- **WHEN** the open tab is for an artifact the refresh removed from the change
- **THEN** that tab is gone and another of the change's tabs is open in its
  place

#### Scenario: An artifact the refresh added

- **WHEN** a refresh lands and the change has gained an artifact file
- **THEN** it has a tab, in the order the tabs are given

#### Scenario: The change itself is gone

- **WHEN** a refresh lands and the change is no longer in the project, having
  been archived or deleted on the remote
- **THEN** the screen says the change is no longer in the project rather than
  showing what it last read
