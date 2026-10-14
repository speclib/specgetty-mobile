## ADDED Requirements

### Requirement: The delta screen can be pulled to refresh

The screen SHALL refresh the repository it is reading on a pull-to-refresh
gesture, from the outline and from an open comparison alike, and SHALL keep what
is open while the refresh runs.

#### Scenario: Pulling the outline

- **WHEN** the person pulls the delta outline down
- **THEN** the repository is fetched and reset to the remote and the project is
  loaded again

#### Scenario: Pulling a comparison

- **WHEN** the person pulls an open comparison down
- **THEN** the refresh happens, the comparison stays open, and the side being
  shown stays the side being shown

#### Scenario: While it runs

- **WHEN** a refresh is in flight
- **THEN** the screen says so, and goes on showing the outline or the comparison

#### Scenario: A refresh that failed

- **WHEN** a refresh fails
- **THEN** what was open stays open and the reason is told alongside it

#### Scenario: A refresh that rewrote a delta

- **WHEN** a refresh lands and the remote had rewritten one of the change's
  delta files
- **THEN** that section of the outline is the new one

#### Scenario: A refresh that rewrote the main spec

- **WHEN** a comparison is open and a refresh changes the main spec it compares
  against
- **THEN** the comparison is against the main spec as it is now

#### Scenario: A requirement the refresh removed

- **WHEN** a comparison is open on a requirement a refresh removed from the
  delta
- **THEN** the screen says so rather than comparing text that is no longer in
  the file

#### Scenario: The change itself is gone

- **WHEN** a refresh lands and the change is no longer in the project
- **THEN** the screen says so rather than showing what it last read
