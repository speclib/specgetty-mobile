## 1. The model

- [x] 1.1 A capability: its name and its spec file
- [x] 1.2 A change: name, directory, archived, date, artifacts, capabilities,
      schema and task counts
- [x] 1.3 A project: its directory, capabilities, active and archived changes,
      configuration and description

## 2. Walking

- [x] 2.1 Capabilities from `specs/`, in name order, `spec.md` required
- [x] 2.2 Active changes from `changes/`, excluding `archive`
- [x] 2.3 Archived changes from `changes/archive/`
- [x] 2.4 Artifacts: markdown files directly inside a change directory
- [x] 2.5 Capabilities a change touches, from its own `specs/`
- [x] 2.6 Task counts per change
- [x] 2.7 Nothing parsed that is not needed for the structure

## 3. Dates and configuration

- [x] 3.1 Archive date from the `<YYYY-MM-DD>-` prefix, name from the rest
- [x] 3.2 An undated or impossible date leaves the change listed without one
- [x] 3.3 Each change's schema from `.openspec.yaml`
- [x] 3.4 The project's schema from `config.yaml` or `config.yml`
- [x] 3.5 `project.md` located when present
- [x] 3.6 YAML read with a safe constructor, and a bad file does not stop a load

## 4. Proof

- [x] 4.1 Tests over a built temporary project for every scenario
- [x] 4.2 A test loading the vendored specgetty project and reporting its shape
- [x] 4.3 `project` package coverage at or above 80 percent
- [x] 4.4 The gate passes
