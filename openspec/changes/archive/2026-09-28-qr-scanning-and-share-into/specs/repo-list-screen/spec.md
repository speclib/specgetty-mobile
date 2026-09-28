## MODIFIED Requirements

### Requirement: A repository is added by URL, with an optional token

The screen SHALL take a clone URL and an optional access token, SHALL validate
the URL before anything is attempted, and SHALL NOT add anything until the
person confirms. The URL MAY be typed, pasted, scanned from a QR code or shared
in from another application, and every one of those SHALL end at the same form,
filled in and editable.

#### Scenario: Adding a public repository

- **WHEN** an HTTPS URL is entered and confirmed
- **THEN** the repository is added and cloned

#### Scenario: Adding a private repository

- **WHEN** a URL and a token are entered and confirmed
- **THEN** the token is used for the clone and stored

#### Scenario: An invalid URL

- **WHEN** the URL is empty, an SSH address, or not a web address
- **THEN** the reason is shown and nothing is added

#### Scenario: Nothing happens before confirming

- **WHEN** a URL has been typed but not confirmed
- **THEN** no repository has been added and nothing has been cloned

#### Scenario: The form can be opened already filled in

- **WHEN** the form is opened with a URL supplied from outside the screen
- **THEN** that URL is in the field, editable, and nothing has been added

#### Scenario: A scanned code

- **WHEN** a QR code holding a repository URL is scanned
- **THEN** the add form opens with that URL filled in
- **AND** no repository has been added

#### Scenario: A shared URL

- **WHEN** a URL is shared into the app from another application
- **THEN** the add form opens with that URL filled in
- **AND** no repository has been added

#### Scenario: Correcting a captured URL

- **WHEN** the normaliser produced the wrong URL
- **THEN** the field can be edited before confirming

#### Scenario: The form is cleared after a successful add

- **WHEN** a repository has been added
- **THEN** the form is empty the next time it is opened

#### Scenario: A token is never shown back

- **WHEN** the form is reopened for a repository that has a token
- **THEN** the token field is empty rather than showing the stored token

#### Scenario: A token is never taken from a capture

- **WHEN** a captured URL carries embedded credentials or a token parameter
- **THEN** the token field stays empty and the credentials are not stored
