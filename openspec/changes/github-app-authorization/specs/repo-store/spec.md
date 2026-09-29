## MODIFIED Requirements

### Requirement: A failure says which kind of failure it is

The system SHALL classify a failure as authentication, no access to this
repository, network, not an OpenSpec project, or unknown, and SHALL return that
as a value rather than raising it.

A credential that the host accepts but that carries no grant on the repository
asked for SHALL NOT be reported as an authentication failure, because the
credential is not the thing that is wrong.

#### Scenario: A rejected token

- **WHEN** the remote refuses the credentials
- **THEN** the failure is an authentication failure

#### Scenario: A host that does not resolve

- **WHEN** the host cannot be reached
- **THEN** the failure is a network failure

#### Scenario: A URL that is not a repository

- **WHEN** the URL points at something that is not a git repository
- **THEN** the failure is reported rather than raised

#### Scenario: Nothing is thrown at the caller

- **WHEN** any repository operation fails
- **THEN** the caller receives a failure value and no exception escapes

#### Scenario: A good credential with no grant on this repository

- **WHEN** the host accepts the credential but refuses the repository
- **THEN** the failure says there is no access to this repository, and is
  distinguishable from an authentication failure

#### Scenario: What the person is told

- **WHEN** that failure is shown
- **THEN** it points at which repositories the credential reaches, and not at the
  credential

#### Scenario: A refusal that mentions writing

- **WHEN** the host refuses a read with a message about write access
- **THEN** it is still read as no access to this repository, the message being
  the host's wording rather than a description of what was attempted
