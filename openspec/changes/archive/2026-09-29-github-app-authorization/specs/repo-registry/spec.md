## MODIFIED Requirements

### Requirement: A token is encrypted and kept apart from the list

The system SHALL encrypt an access token with a key held in the Android
Keystore, SHALL store it separately from the repository list, and SHALL record
in the list only whether a token exists.

A credential obtained by authorizing MAY carry an expiry and the material needed
to renew it. All of it SHALL be encrypted and stored the same way, and SHALL be
removed with the repository.

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

#### Scenario: A credential that can be renewed

- **WHEN** a repository holds a credential obtained by authorizing
- **THEN** its expiry and its renewal material are encrypted and stored with it

#### Scenario: Renewal material is a secret too

- **WHEN** the stored renewal material is read without the Keystore key
- **THEN** it does not reveal anything usable

#### Scenario: Removing a repository that was authorized

- **WHEN** such a repository is removed
- **THEN** its credential and its renewal material are both gone

#### Scenario: A typed token is unchanged by any of this

- **WHEN** a repository holds a token that was typed
- **THEN** it has no expiry and no renewal material, and works as before

#### Scenario: What the list records

- **WHEN** the repository list is read
- **THEN** it records that a credential exists and how it was obtained, and never
  the credential itself
