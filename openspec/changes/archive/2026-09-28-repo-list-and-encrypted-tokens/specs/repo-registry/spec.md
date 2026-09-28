## Purpose

Which repositories the app knows about, which one is being looked at, and how
the access token for a private repository is kept. This capability owns the
repository id, because that id names the directory a clone lands in.

## ADDED Requirements

### Requirement: A repository id is derived from its URL

The system SHALL derive a repository's id from its URL, so that the id is stable
across launches and safe to use as a directory name.

#### Scenario: The same URL twice

- **WHEN** an id is derived from the same URL on two occasions
- **THEN** the two ids are equal

#### Scenario: Different URLs

- **WHEN** ids are derived from two different URLs
- **THEN** the two ids differ

#### Scenario: Surrounding whitespace and case

- **WHEN** the URL differs only by surrounding whitespace or letter case
- **THEN** the id is the same

#### Scenario: Safe as a file name

- **WHEN** an id is derived from any URL
- **THEN** it contains no path separator and no character a file name forbids

### Requirement: A repository carries a readable label

The system SHALL give a repository a label, defaulting to the last segment of
its URL with any `.git` suffix removed, and SHALL use a label the person typed
in preference to the default.

#### Scenario: Default from the URL

- **WHEN** no label is given for `https://github.com/speclib/specgetty.git`
- **THEN** the label is `specgetty`

#### Scenario: A trailing slash

- **WHEN** the URL ends with a slash
- **THEN** the trailing slash does not become part of the label

#### Scenario: A label that was typed

- **WHEN** a label is given
- **THEN** it is used as it is

### Requirement: The list holds each repository once

The system SHALL keep at most one entry per repository URL, and SHALL treat
adding a repository that is already present as an update to it.

#### Scenario: Adding a new repository

- **WHEN** a repository not in the list is added
- **THEN** it appears in the list

#### Scenario: Adding the same repository again

- **WHEN** a repository already in the list is added with a different label
- **THEN** the list still holds one entry for it, carrying the new label

#### Scenario: The first repository becomes active

- **WHEN** the first repository is added to an empty list
- **THEN** it becomes the active repository

#### Scenario: A later repository does not steal focus

- **WHEN** a second repository is added
- **THEN** the active repository is unchanged

### Requirement: Removing a repository chooses a new active one

The system SHALL remove a repository from the list, and SHALL move the active
selection to another repository when the removed one was active.

#### Scenario: Removing the active repository

- **WHEN** the active repository is removed and others remain
- **THEN** one of the remaining repositories becomes active

#### Scenario: Removing a repository that is not active

- **WHEN** a repository that is not active is removed
- **THEN** the active repository is unchanged

#### Scenario: Removing the last repository

- **WHEN** the only repository is removed
- **THEN** the list is empty and no repository is active

#### Scenario: Removing one that is not there

- **WHEN** an id not in the list is removed
- **THEN** the list is unchanged

### Requirement: The active repository can be switched

The system SHALL switch the active repository on request, and SHALL ignore a
request to activate a repository it does not hold.

#### Scenario: Switching

- **WHEN** another repository in the list is activated
- **THEN** it becomes the active repository

#### Scenario: Activating something unknown

- **WHEN** an id not in the list is activated
- **THEN** the active repository is unchanged

### Requirement: A URL is validated before anything is attempted

The system SHALL reject a URL it cannot clone before a clone is attempted, and
SHALL say why in terms the person can act on.

#### Scenario: Empty

- **WHEN** the URL is empty or only whitespace
- **THEN** the person is asked for the repository's clone URL

#### Scenario: An SSH URL

- **WHEN** the URL is an `ssh://` or `git@` address
- **THEN** the person is told SSH is not supported and to use the HTTPS URL

#### Scenario: Not a web address

- **WHEN** the URL uses neither `http://` nor `https://`
- **THEN** the person is told it must start with `https://`

#### Scenario: A scheme with no host

- **WHEN** the URL is a scheme and nothing else
- **THEN** the person is told it has no host

#### Scenario: A URL that will do

- **WHEN** the URL is an HTTPS address with a host
- **THEN** validation passes with nothing to report

### Requirement: A token is encrypted and kept apart from the list

The system SHALL encrypt an access token with a key held in the Android
Keystore, SHALL store it separately from the repository list, and SHALL record
in the list only whether a token exists.

#### Scenario: Storing a token

- **WHEN** a repository is added with a token
- **THEN** the token is encrypted before it is written
- **AND** the repository list records only that a token exists

#### Scenario: Reading a token back

- **WHEN** the token for a repository is asked for
- **THEN** the decrypted token is returned

#### Scenario: No token

- **WHEN** a repository has no token
- **THEN** asking for its token yields nothing rather than an empty string

#### Scenario: A blank token is no token

- **WHEN** a repository is added with an empty or whitespace token
- **THEN** no token is stored and the list records that there is none

#### Scenario: Removing a repository

- **WHEN** a repository is removed
- **THEN** its token is removed with it

#### Scenario: Stored material is unreadable on its own

- **WHEN** the stored token material is read without the Keystore key
- **THEN** it does not reveal the token

### Requirement: The list survives a restart

The system SHALL persist the repository list, and SHALL recover rather than
crash when what was persisted cannot be read.

#### Scenario: Reopening the app

- **WHEN** repositories were added and the app is started again
- **THEN** the same repositories and the same active selection are present

#### Scenario: Unreadable stored data

- **WHEN** the persisted list cannot be decoded
- **THEN** an empty list is used rather than the app failing to start

#### Scenario: A field the app does not know

- **WHEN** the persisted list carries a field this version does not know
- **THEN** it is ignored and the rest is read
