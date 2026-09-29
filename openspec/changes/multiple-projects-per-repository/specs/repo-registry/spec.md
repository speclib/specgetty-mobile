## MODIFIED Requirements

### Requirement: A repository id is derived from its URL and its path

The system SHALL derive a list entry's id from its URL together with the path
within the repository, so that one repository may hold more than one entry. It
SHALL derive the clone's id from the URL alone, so that entries sharing a URL
share one working copy and one credential. Both ids SHALL be stable across
launches and safe to use as a directory name.

#### Scenario: The same URL and path twice

- **WHEN** an entry id is derived from the same URL and path on two occasions
- **THEN** the two ids are equal

#### Scenario: One URL, two paths

- **WHEN** entry ids are derived from one URL with two different paths
- **THEN** the two ids differ

#### Scenario: Different URLs

- **WHEN** entry ids are derived from two different URLs
- **THEN** the two ids differ

#### Scenario: The clone id ignores the path

- **WHEN** clone ids are derived from one URL with two different paths
- **THEN** the two clone ids are equal

#### Scenario: The root path and no path are the same thing

- **WHEN** an entry id is derived from a URL with the empty path
- **THEN** it equals the id derived from that URL before paths existed

#### Scenario: Surrounding whitespace and case

- **WHEN** the URL differs only by surrounding whitespace or letter case
- **THEN** the ids are the same

#### Scenario: Safe as a file name

- **WHEN** either id is derived from any URL and path
- **THEN** it contains no path separator and no character a file name forbids

### Requirement: The list holds each project once

The system SHALL keep at most one entry per URL and path together, and SHALL
treat adding one that is already present as an update to it. It SHALL allow
several entries that share a URL when their paths differ.

#### Scenario: Adding a new entry

- **WHEN** an entry not in the list is added
- **THEN** it appears in the list

#### Scenario: Adding the same project again

- **WHEN** an entry already in the list is added with a different label
- **THEN** the list still holds one entry for it, carrying the new label

#### Scenario: Two projects from one repository

- **WHEN** two projects at different paths in one repository are added
- **THEN** the list holds both

#### Scenario: The first entry becomes active

- **WHEN** the first entry is added to an empty list
- **THEN** it becomes the active entry

#### Scenario: A later entry does not steal focus

- **WHEN** a second entry is added
- **THEN** the active entry is unchanged

### Requirement: Removing an entry keeps what another entry still uses

The system SHALL remove an entry from the list, and SHALL move the active
selection to another entry when the removed one was active. It SHALL delete the
working copy and the credential only when no remaining entry shares the removed
entry's URL.

#### Scenario: Removing the active entry

- **WHEN** the active entry is removed and others remain
- **THEN** one of the remaining entries becomes active

#### Scenario: Removing an entry that is not active

- **WHEN** an entry that is not active is removed
- **THEN** the active entry is unchanged

#### Scenario: Removing the last entry

- **WHEN** the only entry is removed
- **THEN** the list is empty and no entry is active

#### Scenario: Removing one that is not there

- **WHEN** an id not in the list is removed
- **THEN** the list is unchanged

#### Scenario: One of several entries on a repository

- **WHEN** one of two entries sharing a URL is removed
- **THEN** the working copy and the credential are kept
- **AND** the remaining entry still loads

#### Scenario: The last entry on a repository

- **WHEN** the last entry for a URL is removed
- **THEN** the working copy and the credential are deleted

### Requirement: A token is encrypted and kept apart from the list

The system SHALL encrypt an access token with a key held in the Android
Keystore, SHALL store it separately from the repository list under the clone's
id, and SHALL record in the list only whether a token exists.

#### Scenario: Storing a token

- **WHEN** a repository is added with a token
- **THEN** the token is encrypted before it is written
- **AND** the repository list records only that a token exists

#### Scenario: Reading a token back

- **WHEN** the token for an entry is asked for
- **THEN** the decrypted token is returned

#### Scenario: A token shared by entries on one repository

- **WHEN** a second project from a repository that has a token is added
- **THEN** the same token is used, and no second copy is stored

#### Scenario: No token

- **WHEN** a repository has no token
- **THEN** asking for its token yields nothing rather than an empty string

#### Scenario: A blank token is no token

- **WHEN** a repository is added with an empty or whitespace token
- **THEN** no token is stored and the list records that there is none

#### Scenario: Removing a repository

- **WHEN** the last entry for a URL is removed
- **THEN** its token is removed with it

#### Scenario: Removing one of several entries on a repository

- **WHEN** one of two entries sharing a URL is removed
- **THEN** the token is kept, because the remaining entry still needs it

#### Scenario: Stored material is unreadable on its own

- **WHEN** the stored token material is read without the Keystore key
- **THEN** it does not reveal the token

## ADDED Requirements

### Requirement: A list written before paths existed still reads

The system SHALL read a stored list whose entries carry no path, treating each
such entry as recording the repository root, and SHALL keep its ids unchanged so
that the working copy already on the device is still the one it uses.

#### Scenario: A list from an earlier version

- **WHEN** a list stored before entries carried a path is read
- **THEN** every entry loads the project at its repository root

#### Scenario: The clone already on the device

- **WHEN** such an entry is loaded
- **THEN** it uses the working copy already cloned rather than cloning again
