## MODIFIED Requirements

### Requirement: The project is the one at the recorded path

The system SHALL load the project at `openspec/` beneath the path recorded for
the entry, which is the repository root when that path is empty, and SHALL NOT
scan for another while loading.

Which projects a repository holds is settled when it is added, by
`project-discovery`. Loading follows the path that survey recorded, so that a
project moved or removed in the remote is reported as gone rather than silently
replaced by another project in the same repository.

#### Scenario: A repository with a project at its root

- **WHEN** an entry recording the empty path is loaded and the repository holds
  `openspec/` at its root
- **THEN** that project is loaded

#### Scenario: A project inside the repository

- **WHEN** an entry recording the path `nivis` is loaded
- **THEN** the project at `nivis/openspec/` is loaded

#### Scenario: A repository without one

- **WHEN** an entry recording the empty path is loaded and the repository has no
  `openspec/` at its root
- **THEN** loading reports that there is no OpenSpec project here

#### Scenario: The recorded path no longer holds a project

- **WHEN** an entry records a path whose `openspec/` is gone after a refresh
- **THEN** loading reports that there is no OpenSpec project here, at that path

#### Scenario: Another project in the same repository is not substituted

- **WHEN** an entry's recorded path holds no project and a sibling directory
  does
- **THEN** the sibling is not loaded in its place

#### Scenario: Two entries on one repository

- **WHEN** two entries record two different paths within one repository
- **THEN** each loads the project at its own path, independently of the other
