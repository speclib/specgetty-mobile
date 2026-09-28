## Purpose

What this project builds into, which of its settings are fixed for the life of
the application, and what has to pass before a change may be shipped.

## ADDED Requirements

### Requirement: The published identity is fixed

The application SHALL be built with the identity given in `BRIEFING.md`, and
those values SHALL NOT be changed by a later change without the briefing
changing first.

#### Scenario: Application ID

- **WHEN** the debug or release APK is built
- **THEN** its application ID is `io.github.mipmip.specgettyondroid`

#### Scenario: Minimum supported Android

- **WHEN** the manifest is merged
- **THEN** `minSdk` is 26

#### Scenario: Version at first release

- **WHEN** the first release is built
- **THEN** `versionCode` is 1 and `versionName` is `0.1.0`

### Requirement: The app is a single activity hosting Compose

The application SHALL present its whole interface from one activity, through
Jetpack Compose with Material 3, with navigation between screens handled by
Navigation Compose.

#### Scenario: Launching

- **WHEN** the launcher icon is tapped
- **THEN** the single activity starts and renders a Compose destination

#### Scenario: A second screen

- **WHEN** a screen navigates to another
- **THEN** it does so within the same activity, through the navigation graph

### Requirement: Java time and NIO are available on API 26

The build SHALL enable core library desugaring with `desugar_jdk_libs_nio`, so
that the `java.nio.file` and `java.time` APIs JGit relies on are present on the
minimum supported Android version.

#### Scenario: Running on the minimum version

- **WHEN** the app runs on an API 26 device
- **THEN** code paths using `java.nio.file` and `java.time` execute rather than
  raising `NoClassDefFoundError`

### Requirement: A change is gated before it is shipped

The project SHALL provide one command that decides whether a change may ship,
and that command SHALL fail rather than warn when any part of it fails.

#### Scenario: The gate

- **WHEN** `scripts/gate.sh` is run
- **THEN** it runs `nix flake check`, and then a debug assemble, the unit tests,
  Android lint and the coverage verification
- **AND** it exits non-zero if any of them fails

#### Scenario: Before the build exists

- **WHEN** the gate runs in a tree with no `./gradlew`
- **THEN** it reports that the gradle half was skipped and passes on the flake
  half alone

#### Scenario: Lint

- **WHEN** Android lint reports an error
- **THEN** the gate fails

### Requirement: Coverage floors are enforced by the build

The build SHALL fail when instruction coverage over the bundle falls below 70
percent, or when it falls below 80 percent in a package the briefing names as a
parser or index package.

#### Scenario: Overall floor

- **WHEN** coverage over the whole bundle is below 70 percent
- **THEN** `jacocoCoverageVerification` fails

#### Scenario: Core package floor

- **WHEN** coverage in a parser or index package is below 80 percent
- **THEN** `jacocoCoverageVerification` fails

#### Scenario: A core package that does not exist yet

- **WHEN** a named parser or index package has no classes in the build
- **THEN** the rule has nothing to measure and does not fail the build

#### Scenario: Code that a unit test cannot reach

- **WHEN** a class is Android framework glue that unit tests cannot instantiate
- **THEN** it is named in the coverage exclusions rather than left to drag the
  overall figure down silently
