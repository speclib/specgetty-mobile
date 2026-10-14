## ADDED Requirements

### Requirement: The project view can be pulled to refresh

The screen SHALL refresh the repository it is reading on a pull-to-refresh
gesture, so that a reader does not have to return to the list to fetch what they
are looking at.

The screen SHALL keep its tab, its search and its place in the list while the
refresh runs, and SHALL say that it is happening without putting anything else
in the place of the project.

The gesture SHALL be available on every tab, including the ones with nothing in
them to scroll, because a tab that is empty is one of the reasons to want a
fetch.

#### Scenario: Pulling to refresh

- **WHEN** the person pulls the project view down
- **THEN** the repository is fetched and reset to the remote and the project is
  loaded again

#### Scenario: While it runs

- **WHEN** a refresh is in flight
- **THEN** the screen says so, and goes on showing the project, the tab that was
  open and the place in it

#### Scenario: A refresh that changed the project

- **WHEN** a refresh lands and the remote had gained a change
- **THEN** the counts in the header and the lists in the tabs show it, with the
  open tab still open

#### Scenario: A refresh that failed

- **WHEN** a refresh fails
- **THEN** the project stays on the screen and the reason is told alongside it

#### Scenario: A failure worth acting on

- **WHEN** the refresh failed because the repository could not be reached
- **THEN** what is told offers to try again

#### Scenario: A failure that needs authorizing

- **WHEN** the refresh failed because the authorization does not cover the
  repository
- **THEN** what is told offers to authorize it

#### Scenario: Pulling an empty tab

- **WHEN** the person pulls down a tab that has nothing in it to scroll
- **THEN** the refresh happens all the same

#### Scenario: Pulling a screen with no project

- **WHEN** the screen is saying why there is no project to show and it is pulled
  down
- **THEN** the refresh happens, which is how such a repository is retried from
  here
