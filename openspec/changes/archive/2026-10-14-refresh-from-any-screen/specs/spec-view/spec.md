## ADDED Requirements

### Requirement: The spec screen can be pulled to refresh

The screen SHALL refresh the repository it is reading on a pull-to-refresh
gesture, from the outline and from an open card alike, and SHALL keep what is
open while the refresh runs.

#### Scenario: Pulling the outline

- **WHEN** the person pulls the outline down
- **THEN** the repository is fetched and reset to the remote and the project is
  loaded again

#### Scenario: Pulling a card

- **WHEN** the person pulls an open card down
- **THEN** the refresh happens and the card stays open

#### Scenario: While it runs

- **WHEN** a refresh is in flight
- **THEN** the screen says so, and goes on showing the outline or the card

#### Scenario: A refresh that failed

- **WHEN** a refresh fails
- **THEN** the spec stays on the screen and the reason is told alongside it

#### Scenario: A refresh that rewrote the spec

- **WHEN** a refresh lands and the remote had rewritten this spec
- **THEN** the outline is the new one, and the card open on a node still in it
  shows that node's new text

#### Scenario: A card whose node the refresh removed

- **WHEN** a card is open on a node and a refresh removes that node from the
  spec
- **THEN** the screen says so rather than showing text that is no longer in the
  file

#### Scenario: A capability the refresh removed

- **WHEN** a refresh lands and the capability this screen is for is no longer in
  the project
- **THEN** the screen says the capability is not in this project
