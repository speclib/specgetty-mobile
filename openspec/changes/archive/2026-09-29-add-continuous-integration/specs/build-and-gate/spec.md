## MODIFIED Requirements

### Requirement: The published identity is fixed

The application SHALL be built with the identity given in `BRIEFING.md`, and
those values SHALL NOT be changed by a later change without the briefing
changing first.

`versionCode` is the exception to "fixed": it is pinned for the first release
and SHALL increase for every release after it, because Android refuses an update
whose `versionCode` did not.

#### Scenario: Application ID

- **WHEN** the debug or release APK is built
- **THEN** its application ID is `io.github.mipmip.specgettyondroid`

#### Scenario: Minimum supported Android

- **WHEN** the manifest is merged
- **THEN** `minSdk` is 26

#### Scenario: Version at first release

- **WHEN** the first release is built
- **THEN** `versionCode` is 1 and `versionName` is `0.1.0`

#### Scenario: A later release

- **WHEN** a release after the first is built
- **THEN** its `versionCode` is greater than the one the previous release
  published, so that it installs over its predecessor

## ADDED Requirements

### Requirement: The release build is signed when the environment holds a key

The release build SHALL be signed with a keystore taken from the environment,
and SHALL fall through to producing an unsigned APK when the environment holds
no keystore, rather than failing.

A build that demanded a keystore would break every build that cannot have one:
F-Droid builds from source and signs with its own key, and a contributor
building the release variant locally has no key at all. Neither is an error.

The keystore SHALL NOT be committed to the repository.

#### Scenario: A key is present

- **WHEN** the release variant is built and the environment holds a keystore and
  its passwords
- **THEN** the APK produced is signed with it

#### Scenario: No key is present

- **WHEN** the release variant is built and the environment holds no keystore
- **THEN** the build succeeds and produces an unsigned APK

#### Scenario: A build from source by somebody else

- **WHEN** the release variant is built from a clean checkout with no secrets
- **THEN** it behaves as the previous scenario, so that building from source is
  not something only this project can do

#### Scenario: The key is not in the tree

- **WHEN** the repository is searched for a keystore
- **THEN** none is found
