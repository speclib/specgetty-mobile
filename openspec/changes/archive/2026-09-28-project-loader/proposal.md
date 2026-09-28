## Why

Bean `specgetty-mobile-ggx5`, milestone `specgetty-mobile-61i0`.

The parsers read one file each. Something has to walk `openspec/` and say what
is in it: which capabilities have specs, which changes are active and which are
archived, what each change's artifacts are and how far its tasks have got.

`BRIEFING.md` draws the line for Phase 1 and it is worth keeping: one project
per repo, at `openspec/` in the root, no directory scanning and no store
resolution. It also says specs and deltas parse lazily, when they are opened.
That splits the work cleanly. Walking the tree and counting tasks is cheap and
happens on every clone and refresh; parsing a spec is not, and happens when a
finger lands on it.

Archived changes carry their date in the directory name, `<YYYY-MM-DD>-<name>`.
Reading the date from the name rather than from the filesystem is deliberate:
a clone gives every file the time it was written to disk, so a modification time
would sort the archive by when the phone fetched it.

## What Changes

- `project/ProjectModel`: the project, its specs and its changes as values.
- `project/ProjectLoader`: walk `openspec/` into that model, reading each
  change's artifacts, its delta capabilities, its workflow schema and its task
  counts.
- The archive date parsed from the directory name, with a change whose name does
  not carry one still listed.
- `config.yaml` or `config.yml` read for the project's schema, and `project.md`
  located when present.

## Capabilities

### New Capabilities

- `project-loading`: walking an OpenSpec project into specs, changes and
  configuration, without parsing a spec until it is opened.

## Impact

- New dependency: snakeyaml, for `config.yaml` and each change's
  `.openspec.yaml`. It is already in the version catalog and is the parser
  `BRIEFING.md` asks for.
- YAML is read with `SafeConstructor`: these files come from a repository
  somebody else wrote, and a YAML parser that can construct arbitrary types is
  a way into the app.
- Loading is eager about structure and task counts, and lazy about spec
  content, which is what the briefing specifies.
