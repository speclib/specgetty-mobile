## ADDED Requirements

### Requirement: The signature the published APK carries is stated

The project SHALL record which key signs the APK it publishes, and SHALL state
what F-Droid inclusion would do to that signature, rather than leaving it to be
discovered by a user whose update is refused.

The decision recorded is that the APK published on releases is signed with this
project's own key. F-Droid signs with its own key unless a build is reproducible,
and Android refuses an update across two different signatures. A user who
installed from a release and then installs from F-Droid has to uninstall first,
which takes the repository list and its credentials with it. That break is
accepted while `versionCode` is 1 and there are no users to break.

#### Scenario: The key is named

- **WHEN** `docs/fdroid.md` is read
- **THEN** it says the published APK is signed with the project's own key

#### Scenario: What F-Droid would change

- **WHEN** the audit is read
- **THEN** it says F-Droid would sign with its own key, that the two cannot
  update across each other, and what a user loses by uninstalling

#### Scenario: Why it was accepted rather than solved

- **WHEN** the audit is read
- **THEN** it says reproducible builds are the alternative and why they were not
  taken now

#### Scenario: The decision stops being cheap

- **WHEN** the app has users and F-Droid inclusion is pursued
- **THEN** the audit is what says what they are about to be asked to do

### Requirement: The signing key is kept, and where it is kept is written down

The project SHALL record where the release keystore is held and where its backup
is, and SHALL state what losing it costs.

Losing the keystore ends the ability to update anyone who installed from a
release. The only remedy is a new application ID, and `BRIEFING.md` says the
application ID is permanent once published, so there is no remedy.

#### Scenario: Where it lives

- **WHEN** the audit is read
- **THEN** it says where the keystore is held and where the backup is, without
  putting the key or any password in the repository

#### Scenario: What losing it costs

- **WHEN** the audit is read
- **THEN** it says that a lost keystore cannot be replaced and what that ends

#### Scenario: What the repository holds

- **WHEN** the repository is searched for a keystore or a signing password
- **THEN** neither is found, and the audit says the workflow takes them from
  repository secrets instead
