# repo-store Specification

## Purpose
Getting a repository onto the device and keeping it current. This capability
owns cloning, refreshing and deleting, the rule that only HTTPS is spoken, and
the vocabulary the rest of the app uses to say what went wrong.

## Requirements

### Requirement: JGit runs inside app-private storage

The system SHALL configure JGit so that it reads no configuration from outside
the application's own storage, and SHALL do so before the first repository
operation.

#### Scenario: No user configuration on the device

- **WHEN** JGit resolves the user, system or JGit configuration
- **THEN** it reads files under an app-private directory
- **AND** it does not read the device user's home directory

#### Scenario: Hostname

- **WHEN** JGit asks the platform for a hostname
- **THEN** a fixed value is returned rather than a lookup that may fail

#### Scenario: Installing twice

- **WHEN** installation is requested more than once
- **THEN** the second request is ignored rather than replacing a live reader

### Requirement: A repository is cloned shallowly

The system SHALL clone a repository at depth 1 into app-private storage, and
SHALL fetch only the branch it is cloning.

#### Scenario: A clone

- **WHEN** a repository is cloned
- **THEN** its working copy exists under the store's root directory
- **AND** exactly one commit is present in its history

#### Scenario: Two repositories

- **WHEN** two repositories are cloned
- **THEN** each has its own working directory and neither sees the other's files

#### Scenario: Cloning over an existing directory

- **WHEN** a clone is made for an id that already has a working directory
- **THEN** the old directory is removed first

#### Scenario: A clone that fails

- **WHEN** a clone fails for any reason
- **THEN** no partial working directory is left behind

### Requirement: A refresh takes the remote's version

The system SHALL refresh a repository by fetching the remote and then resetting
hard to the remote branch, so that the working copy matches the remote exactly
rather than merging with it.

#### Scenario: A new commit upstream

- **WHEN** the remote has gained a commit and a refresh runs
- **THEN** the working copy contains the new commit's files

#### Scenario: A file deleted upstream

- **WHEN** a file is removed on the remote and a refresh runs
- **THEN** the file is gone from the working copy

#### Scenario: Local modification

- **WHEN** the working copy has been modified and a refresh runs
- **THEN** the modification is discarded in favour of the remote's version

#### Scenario: Refreshing something never cloned

- **WHEN** a refresh is asked for an id with no working copy
- **THEN** it fails rather than cloning silently

### Requirement: A repository can be deleted

The system SHALL delete a repository's working copy on request and SHALL report
whether there was anything to delete.

#### Scenario: Deleting a cloned repository

- **WHEN** a cloned repository is deleted
- **THEN** its working directory no longer exists
- **AND** the operation reports that it removed something

#### Scenario: Deleting one that is not there

- **WHEN** a repository that was never cloned is deleted
- **THEN** the operation reports that there was nothing to remove and does not fail

### Requirement: Only HTTPS is spoken, with an optional token

The system SHALL authenticate over HTTPS using a personal access token when one
is supplied, and SHALL NOT support SSH.

#### Scenario: A public repository

- **WHEN** a repository needs no credentials
- **THEN** it clones with no token supplied

#### Scenario: A private repository

- **WHEN** a token is supplied
- **THEN** it is presented as the HTTPS username with an empty password

#### Scenario: A blank token

- **WHEN** the token is empty or whitespace
- **THEN** no credentials are presented at all

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

### Requirement: An OpenSpec project sits at the repository root

The system SHALL look for the project at `openspec/` in the repository root and
SHALL NOT scan for one anywhere else.

#### Scenario: A repository with a project

- **WHEN** the working copy has an `openspec/` directory at its root
- **THEN** that directory is the project

#### Scenario: A repository without one

- **WHEN** the working copy has no `openspec/` at its root
- **THEN** the result says there is no OpenSpec project here

#### Scenario: A project nested deeper

- **WHEN** an `openspec/` directory exists only in a subdirectory
- **THEN** it is not found, because nested directories are not scanned

#### Scenario: An empty project

- **WHEN** `openspec/` exists but contains no specs and no changes
- **THEN** the project is found, and being empty is not the same as being absent

### Requirement: The paths inside a project are named in one place

The system SHALL resolve the specs directory, the active changes directory, the
archive directory, the configuration file and the project description from the
project directory, and SHALL accept either `config.yaml` or `config.yml`.

#### Scenario: Standard layout

- **WHEN** the project directory is resolved
- **THEN** `specs/`, `changes/` and `changes/archive/` are derived from it

#### Scenario: Either spelling of the configuration

- **WHEN** the project has `config.yml` rather than `config.yaml`
- **THEN** it is found

#### Scenario: No configuration at all

- **WHEN** the project has neither spelling
- **THEN** the result is absent rather than an error
