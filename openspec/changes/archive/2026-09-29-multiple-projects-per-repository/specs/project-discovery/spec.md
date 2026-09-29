## Purpose

Finding every OpenSpec project a cloned repository holds, rather than assuming
there is one and that it sits at the root. Covers which directories qualify,
what each discovered project is called, and how a project that points at content
held elsewhere is reported.

## ADDED Requirements

### Requirement: A repository is surveyed for every project it holds

The system SHALL survey a cloned working copy for every directory that holds a
project, and SHALL report them ordered by their path within the repository so
that the result does not depend on the order the filesystem returns.

#### Scenario: A repository with one project at its root

- **WHEN** a repository holding a qualifying `openspec/` at its root is surveyed
- **THEN** one project is reported, at the empty path

#### Scenario: A repository holding several projects

- **WHEN** a repository holds qualifying `openspec/` directories in four of its
  top-level directories
- **THEN** four projects are reported, each at the path of its directory

#### Scenario: A project below the top level

- **WHEN** a qualifying `openspec/` lives several directories deep
- **THEN** it is reported at its full path within the repository

#### Scenario: A repository with none

- **WHEN** a repository holds no qualifying `openspec/` anywhere
- **THEN** no projects are reported

#### Scenario: A root project and a nested one

- **WHEN** a repository holds a qualifying `openspec/` at its root and another
  in a subdirectory
- **THEN** both are reported, the root one first

#### Scenario: The order does not depend on the filesystem

- **WHEN** the same repository is surveyed twice
- **THEN** the projects are reported in the same order both times

### Requirement: A directory qualifies by what its openspec directory holds

A directory SHALL be reported as a project when it holds a direct child
directory named `openspec` holding at least one of `config.yaml`, `config.yml`
or `project.md`. A directory named `openspec` holding none of those SHALL NOT
qualify, whatever else it contains.

This is the rule specgetty's scanner applies. A looser rule that accepted any
directory named `openspec` would fill the list with whatever a repository
happens to keep under that name.

#### Scenario: Qualifying by configuration

- **WHEN** a directory's `openspec/` holds `config.yaml`
- **THEN** that directory is reported as a project

#### Scenario: The other configuration spelling

- **WHEN** a directory's `openspec/` holds `config.yml` and no `config.yaml`
- **THEN** that directory is reported as a project

#### Scenario: Qualifying by a project file

- **WHEN** a directory's `openspec/` holds `project.md` and no configuration
- **THEN** that directory is reported as a project

#### Scenario: Content without any of the three

- **WHEN** a directory's `openspec/` holds `specs/` and `changes/` but none of
  `config.yaml`, `config.yml` or `project.md`
- **THEN** that directory is NOT reported

#### Scenario: A directory called openspec holding something else

- **WHEN** a directory named `openspec` holds unrelated files
- **THEN** it is NOT reported

#### Scenario: Inside the git directory

- **WHEN** a qualifying `openspec/` exists inside a `.git` subtree
- **THEN** it is NOT reported

#### Scenario: A project that qualifies but holds nothing yet

- **WHEN** a directory's `openspec/` holds `config.yaml` and no specs and no
  changes
- **THEN** it is reported, and loading it reports an empty project rather than
  an absent one

### Requirement: A discovered project is named by its directory

A project found in a subdirectory SHALL be named by that directory. The system
SHALL NOT take a name from `.openspec-store/store.yaml`.

An identity file alone does not make a directory a store: a clone keeps its copy
of that file, and a clone is exactly what the app holds. Naming a row from it
would give the clone the registered store's name on a device that has no
registry to confirm it.

#### Scenario: A project in a subdirectory

- **WHEN** a project is found at `nivis` within a repository
- **THEN** it is named `nivis`

#### Scenario: A directory carrying store identity

- **WHEN** a discovered directory holds `.openspec-store/store.yaml` naming an
  id different from the directory name
- **THEN** the project is still named by its directory, and the id is not shown

#### Scenario: A project at the root

- **WHEN** a project is found at the repository root
- **THEN** it is named as repositories are named, from the URL

### Requirement: A declaration the app cannot follow is reported as such

When a discovered project's configuration declares `store: <id>` and the project
holds neither specs nor changes of its own, the system SHALL report that the
project points at content it cannot reach, naming the declared id and the file
that declared it. It SHALL NOT report the project as absent or as empty.

A store declaration is followed through a registry of local paths on the machine
that wrote it. A phone has no such registry and no such paths, so the pointer
cannot be followed. Saying so is the difference between a person knowing the
repository is not the one to add and believing the app is broken.

#### Scenario: A repository that points at a store

- **WHEN** a project's `openspec/config.yaml` declares `store: nivis` and it
  holds no specs and no changes
- **THEN** the declared id and the file that declared it are reported
- **AND** the project is not reported as absent

#### Scenario: Content of its own outranks a declaration

- **WHEN** a project declares a store and also holds its own specs or changes
- **THEN** its own content is loaded and the declaration is ignored

#### Scenario: The declaration is not a single id

- **WHEN** the `store` key holds a list or a mapping rather than a string
- **THEN** the declaration is reported as malformed

#### Scenario: A repository with nothing at all

- **WHEN** a repository holds no qualifying `openspec/` and declares nothing
- **THEN** it reports that there is no OpenSpec project here, as before
