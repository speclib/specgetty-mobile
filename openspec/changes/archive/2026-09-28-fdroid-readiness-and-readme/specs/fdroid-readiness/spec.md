## Purpose

What this project claims about what it ships, and how each claim is checked.
F-Droid inclusion depends on these being true rather than intended.

## ADDED Requirements

### Requirement: Every dependency is FOSS and is recorded

The project SHALL record the licence of every group on the release runtime
classpath, taken from what the artifact publishes, and SHALL name any group
whose licence could not be read that way rather than filling one in.

#### Scenario: The record exists

- **WHEN** `docs/fdroid.md` is read
- **THEN** every group on the release runtime classpath is listed with its
  licence

#### Scenario: A licence that cannot be read from the artifact

- **WHEN** a dependency publishes no licence in its POM
- **THEN** that is stated, rather than a licence being assumed silently

#### Scenario: No proprietary services

- **WHEN** the release runtime classpath is searched for Play Services,
  Firebase, Crashlytics, analytics or ad SDKs
- **THEN** none is present

#### Scenario: The check can be repeated

- **WHEN** someone wants to verify the claim
- **THEN** the document gives the command that produces the answer

### Requirement: Every permission is justified and its cost stated

The project SHALL list each permission the app declares, why it is needed, and
what refusing it costs.

#### Scenario: The permissions are listed

- **WHEN** the audit is read
- **THEN** `INTERNET` and `CAMERA` are each listed with a reason

#### Scenario: What refusing costs

- **WHEN** camera access is refused
- **THEN** the audit says scanning stops working and everything else does not

#### Scenario: A permission added later

- **WHEN** the app gains a permission
- **THEN** the audit changes with it rather than continuing to claim the old set

### Requirement: The intents claimed are stated, including one not claimed

The project SHALL record which intent filters the app registers, and SHALL state
that `ACTION_VIEW` for `https` is deliberately not among them.

#### Scenario: The filter that is claimed

- **WHEN** the audit is read
- **THEN** `ACTION_SEND` with `text/plain` is named

#### Scenario: The filter that is not

- **WHEN** the audit is read
- **THEN** it says `ACTION_VIEW` for `https` is not claimed, and why

### Requirement: Committed binaries are enumerated and justified

The project SHALL commit no prebuilt binary or jar except ones it names, and
SHALL record for each the reason and a checksum.

#### Scenario: The only binary

- **WHEN** the repository is searched for `.jar`, `.so`, `.aar`, `.dex` or
  `.apk` files
- **THEN** only the Gradle wrapper jar is found

#### Scenario: It is accounted for

- **WHEN** the audit is read
- **THEN** the wrapper jar is named as a deliberate deviation, with its checksum
  and the reason

#### Scenario: The distribution it fetches is pinned

- **WHEN** the wrapper downloads a Gradle distribution
- **THEN** that download is verified against a checksum in
  `gradle-wrapper.properties`

### Requirement: The store listing exists and does not overstate

The project SHALL provide Fastlane metadata for the default locale, and SHALL
NOT ship a screenshot of something that has not been run.

#### Scenario: The metadata

- **WHEN** `fastlane/metadata/android/en-US/` is read
- **THEN** it holds a title, a short description, a full description and a
  changelog for version 1

#### Scenario: Screenshots

- **WHEN** no screenshot has been taken from a device
- **THEN** the directory is present and says so, rather than holding a mock-up

#### Scenario: What the description claims

- **WHEN** the full description is read
- **THEN** it says this version reads and does not edit

### Requirement: The app says it is not affiliated with OpenSpec

The README and the store description SHALL both state that this app is not
affiliated with the OpenSpec project.

#### Scenario: The README

- **WHEN** the README is read
- **THEN** it says the app is not affiliated with the OpenSpec project

#### Scenario: The store description

- **WHEN** the full description is read
- **THEN** it says the same

### Requirement: The README tells a newcomer what to do

The README SHALL say what the app is, how to build it, how to install it, how to
add a repository and a token, and what is planned next.

#### Scenario: Building

- **WHEN** the README is read
- **THEN** it gives the command that produces an APK

#### Scenario: Installing

- **WHEN** the README is read
- **THEN** it says how to get that APK onto a phone

#### Scenario: Adding a repository

- **WHEN** the README is read
- **THEN** it says what a URL may look like and that a page URL is normalised

#### Scenario: The token

- **WHEN** the README is read
- **THEN** it says a token is typed rather than captured, and how it is kept

#### Scenario: What is not in this version

- **WHEN** the README is read
- **THEN** it says this version reads and names what Phase 2 adds
