## MODIFIED Requirements

### Requirement: The suite runs on the minimum supported Android

The system SHALL run its instrumented tests on an API 26 emulator, that being
the minimum version the app supports.

The project can now start an emulator at any API level its SDK holds, so "its
own" is no longer enough to identify the right one. The suite SHALL target the
API 26 emulator specifically rather than whichever of the project's emulators it
finds first.

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

#### Scenario: Another of this project's emulators is attached

- **WHEN** one of the project's emulators at a different API level is running
  and the suite is run
- **THEN** the suite targets the API 26 one and leaves the other alone, rather
  than taking whichever of ours it finds first

#### Scenario: An emulator already running

- **WHEN** the suite is run and an emulator of ours is already up
- **THEN** it is used, and it is not stopped afterwards
