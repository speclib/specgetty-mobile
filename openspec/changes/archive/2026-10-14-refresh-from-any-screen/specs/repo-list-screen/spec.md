## MODIFIED Requirements

### Requirement: The list can be refreshed

The screen SHALL refresh a repository on a pull-to-refresh gesture, fetching and
resetting to the remote and reloading the project.

A row that already shows a project SHALL go on showing it while the refresh runs
and if the refresh fails. The failure SHALL be told without the row being
replaced by it, because the working copy the row was reading is still there.

#### Scenario: Pulling to refresh

- **WHEN** the person pulls the list down
- **THEN** the repositories are refreshed and their statistics are recomputed

#### Scenario: While refreshing

- **WHEN** a refresh is in flight
- **THEN** the screen shows that it is happening

#### Scenario: A loaded row through a refresh

- **WHEN** a row showing a project is refreshed
- **THEN** it goes on showing that project's statistics until the new ones
  replace them

#### Scenario: A loaded row whose refresh fails

- **WHEN** a refresh fails for a row that was showing a project
- **THEN** the row still shows that project, and the reason the refresh failed
  is told alongside the list

#### Scenario: A row that never loaded

- **WHEN** a refresh fails for a row that has no project to show
- **THEN** the row says what went wrong, as it does for a failed clone

#### Scenario: A refresh asked for elsewhere

- **WHEN** a refresh asked for from another screen is in flight
- **THEN** the list shows that it is happening for the rows on that working copy
