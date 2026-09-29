## Purpose
What runs when a commit is pushed and when a version is tagged: which checks a
change has to survive in public, what the repository publishes about itself, and
what a release has to agree with before it is allowed to exist.

## ADDED Requirements

### Requirement: Every push is gated where everyone can see it

The repository SHALL run the same gate `scripts/gate.sh` runs, on every push and
on every pull request, and SHALL fail rather than warn when any part of it
fails.

The gate is in two halves because gradle needs network access that a sandboxed
derivation does not get. Both halves SHALL run, and a run that skips the gradle
half SHALL NOT be reported as a pass.

Running the gate rather than a set of steps that resemble it is deliberate: two
gates that can disagree are worse than one, because the disagreement is found
when a change is already believed to be finished.

#### Scenario: A push

- **WHEN** a commit is pushed to any branch
- **THEN** the flake half and the gradle half both run
- **AND** the run fails if either fails

#### Scenario: A pull request from a fork

- **WHEN** a pull request is opened from a fork
- **THEN** the gate runs on it and can report a pass
- **AND** it needs no permission the fork cannot be granted

#### Scenario: A branch pushed twice

- **WHEN** a second commit is pushed to a branch whose run has not finished
- **THEN** the older run is cancelled, because the newer commit is the question
  worth answering

#### Scenario: Android lint

- **WHEN** Android lint reports an error
- **THEN** the run fails, as the gate does

### Requirement: The build kept from a check is a debug build and says so

The check SHALL keep the debug APK it built, and SHALL name and describe it as a
debug build rather than as a way to install the app.

The debug APK carries the Android SDK's debug key. That key is public, and it is
not the key a release carries, so a device holding one refuses the other and the
refusal costs the repository list and its credentials. The build is kept because
putting a pull request on a phone is worth doing; it is labelled because doing
that unknowingly is not.

#### Scenario: The artifact exists

- **WHEN** a check finishes successfully
- **THEN** the debug APK it built is retrievable from that run

#### Scenario: What it is called

- **WHEN** the artifact is listed
- **THEN** its name says it is a debug build

#### Scenario: What the README offers

- **WHEN** the README's installation section is read
- **THEN** it does not offer the check artifact as a way to get the app

### Requirement: Badges describe a commit that passed the gate

The repository SHALL publish badges from a workflow separate from the gate,
running only after a successful gate on the default branch, so that no badge
describes a commit the gate rejected.

Publishing needs write access to the repository, which a pull request from a
fork can never be granted. Keeping it separate SHALL mean that an outside
contributor's pull request cannot fail because of it.

#### Scenario: After a pass

- **WHEN** the gate passes on the default branch
- **THEN** the badges are published for that commit

#### Scenario: After a failure

- **WHEN** the gate fails
- **THEN** nothing is published, and the badges continue to describe the last
  commit that passed

#### Scenario: A fork's pull request

- **WHEN** a pull request from a fork is pushed
- **THEN** the badge workflow does not run against it, and its absence does not
  fail the pull request

#### Scenario: What is published

- **WHEN** the badges are published
- **THEN** they include the project's OpenSpec metrics and its coverage

### Requirement: The coverage badge shows the number the gate measured

The coverage published SHALL be the figure produced by the same run that decided
whether the tree passes, carried from it, rather than a second measurement.

A second measurement can disagree with the one that decides whether a change may
ship, and a badge that disagrees with the gate is worse than no badge.

#### Scenario: The number is carried

- **WHEN** the badge is published
- **THEN** its figure came from the run that gated that commit

#### Scenario: No number to carry

- **WHEN** the gate did not pass, so no figure was produced
- **THEN** no coverage badge is published for that commit

### Requirement: A release is refused when the tag disagrees with the tree

A release SHALL be built only from a version tag, and SHALL be refused unless
the tree agrees with the tag in every respect the release depends on.

#### Scenario: The version name

- **WHEN** a tag `vX.Y.Z` is pushed and the build's `versionName` is not `X.Y.Z`
- **THEN** the release is refused and says which two values disagree

#### Scenario: The version code

- **WHEN** a tag is pushed and the build's `versionCode` is not greater than the
  one the previous tag published
- **THEN** the release is refused, because Android will not install an update
  whose `versionCode` did not increase

#### Scenario: The first release

- **WHEN** a tag is pushed and there is no previous tag to compare against
- **THEN** there is nothing for the version code to exceed and that check passes

#### Scenario: The release notes

- **WHEN** a tag is pushed and `CHANGELOG.md` has no entry for that version
- **THEN** the release is refused, because a release whose notes do not exist is
  a release nobody can read

#### Scenario: Nothing is published by a refusal

- **WHEN** a release is refused for any of these reasons
- **THEN** no tag assets and no release exist for that version

### Requirement: A published release carries an APK that installs

The release SHALL carry a signed APK, named so that its version is readable from
the file name, and SHALL take its notes from the changelog entry for that
version.

#### Scenario: The asset

- **WHEN** a release is published
- **THEN** it carries one APK, signed with the project's release key

#### Scenario: The name

- **WHEN** the asset is downloaded
- **THEN** its file name says which version it is

#### Scenario: What it says

- **WHEN** the release page is read
- **THEN** its notes are the changelog entry for that version
