# project-loading Specification

## Purpose
Walking an OpenSpec project into the specs, changes and configuration the
screens show. This capability owns where things are looked for and what is read
eagerly, but not how a spec's text is understood.

## Requirements

### Requirement: The project is the one at the repository root

The system SHALL load the project at `openspec/` in the repository root and
SHALL NOT scan for another.

#### Scenario: A repository with a project

- **WHEN** a repository holding `openspec/` at its root is loaded
- **THEN** that project is loaded

#### Scenario: A repository without one

- **WHEN** a repository has no `openspec/` at its root
- **THEN** loading reports that there is no OpenSpec project here

#### Scenario: A project nested deeper

- **WHEN** an `openspec/` directory exists only in a subdirectory
- **THEN** it is not loaded

### Requirement: Capabilities are the directories under specs

The system SHALL treat each directory under `openspec/specs/` holding a
`spec.md` as one capability, named by the directory.

#### Scenario: Capabilities are listed

- **WHEN** a project holds `specs/change-search/spec.md` and
  `specs/overview-tab/spec.md`
- **THEN** both capabilities are listed, named by their directories

#### Scenario: Ordering

- **WHEN** the capabilities are listed
- **THEN** they are in name order, so the list does not move between loads

#### Scenario: A directory with no spec file

- **WHEN** a directory under `specs/` holds no `spec.md`
- **THEN** it is not a capability

#### Scenario: No specs at all

- **WHEN** a project has no `specs/` directory
- **THEN** the capability list is empty rather than an error

#### Scenario: The spec is not read yet

- **WHEN** the project is loaded
- **THEN** no `spec.md` has been parsed

### Requirement: Changes are active or archived

The system SHALL treat each directory directly under `openspec/changes/` as an
active change, except `archive`, and each directory under
`openspec/changes/archive/` as an archived change.

#### Scenario: An active change

- **WHEN** `changes/add-a-thing/` exists
- **THEN** it is an active change named `add-a-thing`

#### Scenario: An archived change

- **WHEN** `changes/archive/2026-09-21-add-a-thing/` exists
- **THEN** it is an archived change

#### Scenario: The archive directory is not a change

- **WHEN** the changes directory is listed
- **THEN** `archive` is not among the active changes

#### Scenario: No changes at all

- **WHEN** a project has no `changes/` directory
- **THEN** both lists are empty rather than an error

### Requirement: An archived change carries the date from its name

The system SHALL read an archived change's date from the `<YYYY-MM-DD>-` prefix
of its directory name, and SHALL use the name after that prefix as its name.

#### Scenario: A dated directory

- **WHEN** the directory is `2026-09-21-open-a-change-spec-in-detail`
- **THEN** the date is 2026-09-21 and the name is `open-a-change-spec-in-detail`

#### Scenario: A directory with no date

- **WHEN** an archived directory's name carries no date prefix
- **THEN** it is still listed, with no date and its whole directory name

#### Scenario: A name that only looks dated

- **WHEN** the directory is `2026-13-45-nonsense`
- **THEN** it is listed with no date rather than an impossible one

#### Scenario: The date is the name's, not the file's

- **WHEN** an archived change is loaded from a fresh clone
- **THEN** its date comes from its directory name and not from the filesystem

### Requirement: A change lists the artifacts it actually has

The system SHALL list the markdown files directly inside a change directory as
its artifacts, in a stable order, and SHALL NOT expect any particular one to be
present.

#### Scenario: The usual artifacts

- **WHEN** a change holds `proposal.md`, `design.md` and `tasks.md`
- **THEN** all three are listed

#### Scenario: A change with only a proposal

- **WHEN** a change holds only `proposal.md`
- **THEN** only that is listed and the change is still loaded

#### Scenario: An artifact nobody expected

- **WHEN** a change holds `notes.md`
- **THEN** it is listed too

#### Scenario: Files that are not artifacts

- **WHEN** a change holds `.openspec.yaml` and a `specs/` directory
- **THEN** neither is listed as an artifact

### Requirement: A change lists the capabilities it touches

The system SHALL list each directory under a change's `specs/` holding a
`spec.md` as a capability that change touches.

#### Scenario: A change touching two capabilities

- **WHEN** a change holds `specs/a/spec.md` and `specs/b/spec.md`
- **THEN** it names both capabilities

#### Scenario: A change touching none

- **WHEN** a change has no `specs/` directory
- **THEN** it names no capabilities and is still loaded

#### Scenario: The delta is not read yet

- **WHEN** the project is loaded
- **THEN** no delta file has been parsed

### Requirement: A change names its workflow schema

The system SHALL read a change's schema from the `schema` key of its
`.openspec.yaml`, and SHALL leave it unnamed when the file is absent or does not
say.

#### Scenario: A schema is named

- **WHEN** a change's `.openspec.yaml` reads `schema: spec-driven`
- **THEN** the change's schema is `spec-driven`

#### Scenario: Another schema

- **WHEN** a change's `.openspec.yaml` reads `schema: tinychange`
- **THEN** the change's schema is `tinychange`

#### Scenario: No file

- **WHEN** a change has no `.openspec.yaml`
- **THEN** it has no schema rather than a guessed one

#### Scenario: A file that is not readable as YAML

- **WHEN** a change's `.openspec.yaml` cannot be parsed
- **THEN** the change has no schema and loading continues

#### Scenario: YAML is read safely

- **WHEN** a YAML file asks for an arbitrary type to be constructed
- **THEN** it is refused, the file coming from a repository someone else wrote

### Requirement: A change carries its task counts

The system SHALL read each change's `tasks.md` when loading it and SHALL carry
its done and total.

#### Scenario: A change with tasks

- **WHEN** a change's `tasks.md` holds three done of five
- **THEN** the change reports 3 of 5

#### Scenario: A change with no tasks file

- **WHEN** a change has no `tasks.md`
- **THEN** it reports 0 of 0

### Requirement: The project's own configuration is located

The system SHALL find `config.yaml` or `config.yml` and read the project's
schema from it, and SHALL find `project.md` when it is there.

#### Scenario: Either spelling

- **WHEN** the project holds `config.yml` rather than `config.yaml`
- **THEN** it is found and read

#### Scenario: The project schema

- **WHEN** the configuration reads `schema: spec-driven`
- **THEN** the project's schema is `spec-driven`

#### Scenario: A project description

- **WHEN** the project holds `project.md`
- **THEN** it is located

#### Scenario: Neither file

- **WHEN** the project holds no configuration and no description
- **THEN** it loads, with both absent

### Requirement: An empty project loads

The system SHALL distinguish a project with nothing in it from a repository
with no project at all.

#### Scenario: An empty project

- **WHEN** `openspec/` exists but holds no specs and no changes
- **THEN** the project loads, empty

#### Scenario: Not the same as absent

- **WHEN** an empty project is compared with a repository that has none
- **THEN** the two outcomes differ
