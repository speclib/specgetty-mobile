## Why

Tracks [specgetty-mobile-m468](../../../.beans/specgetty-mobile-m468--support-for-store-only-repos.md).

One repository can hold several OpenSpec projects side by side.
`nivis-openspec-stores` holds four, one per directory at its top level, each
with its own `openspec/specs/`, `openspec/changes/` and `openspec/config.yaml`.
The app loads the project at the repository root and nothing else, so that
repository reads as "No OpenSpec project here" and none of its four projects can
be opened at all.

The same code path tells a second untruth. A repository whose `openspec/`
declares `store: <id>` and keeps no content of its own is a real project that
points at content held elsewhere on the machine that wrote it. A phone has no
store registry and no such local path, so it cannot follow the pointer. Saying
there is no project here is wrong, and says nothing about what is actually
needed.

## What Changes

- Look for projects throughout a cloned repository rather than only at its root.
  A directory holds a project when its `openspec/` holds `config.yaml`,
  `config.yml` or `project.md`, which is the rule specgetty's scanner applies.
- When a repository holds more than one project, ask which to add. Each accepted
  project becomes its own row in the repository list, carrying the path inside
  the repository where its `openspec/` lives.
- Keep the clone shared. Several rows from one URL clone once, hold one
  credential, and refresh once.
- **BREAKING** A repository list entry is no longer identified by its URL alone.
  The identity becomes the URL together with the path, so that one URL can
  appear more than once. A list written before this change reads back unchanged,
  every entry carrying the empty path that means the repository root.
- Report a `store:` declaration the app cannot follow as exactly that, naming
  the declared id and the file that declared it, rather than as an absent
  project.
- Say nothing about stores in the interface. A directory carrying
  `.openspec-store/store.yaml` is named by its directory, because specgetty's
  own rule is that an identity file alone does not make a store, and a clone is
  the case that rule exists for.

## Capabilities

### New Capabilities

- `project-discovery`: finding every project a cloned repository holds, which
  directories qualify, and how a declaration the app cannot follow is reported.

### Modified Capabilities

- `project-loading`: the project is no longer the one at the repository root.
  Loading targets a path within the repository, which is the root when that path
  is empty.
- `repo-registry`: an entry is identified by URL and path rather than by URL, so
  the list may hold one URL more than once. The clone and the credential stay
  keyed by URL alone, and removing one entry leaves both alone while another
  entry still uses them.
- `repo-list-screen`: adding a repository that holds more than one project asks
  which to add. A repository holding one project is added exactly as before.

## Impact

- `repo/OpenSpecLayout`: qualification of a directory as a project, and a sweep
  over a working copy.
- `repo/RepoStore`: `projectDir` takes the path within the repository.
- `store/RepoConfig`: a `path` field, an entry id derived from URL and path, and
  a clone id that stays derived from the URL.
- `store/RepoRegistry`: the vault and the clone directory keyed by clone id;
  removal conditional on the last entry for a URL.
- `data/ProjectRepository`: adding after a survey of what the clone holds, and
  refreshing a shared clone once.
- `viewmodel/RepoListViewModel`, `ui/screen/RepoListScreen`: choosing projects
  when there is more than one.
- `BRIEFING.md`: the Phase 1 non-goal bundles scanning with store resolution.
  Only the scanning half is amended; store resolution stays out.
