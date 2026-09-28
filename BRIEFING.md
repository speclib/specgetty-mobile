# Briefing: Specgetty on Droid, Phase 1 (read-only)

An Android app for browsing and reading [OpenSpec](https://github.com/Fission-AI/OpenSpec)
projects in git repositories. It is the mobile counterpart of
[specgetty](https://github.com/speclib/specgetty), the OpenSpec TUI, and borrows
its git layer from [beans-on-droid](https://github.com/mipmip/beans-on-droid).

This document is the source of truth for what Phase 1 is. OpenSpec proposals
refine it; they do not replace it.

## Fixed settings

| Setting        | Value                                   |
|----------------|-----------------------------------------|
| Application ID | `io.github.mipmip.specgettyondroid`     |
| App name       | Specgetty on Droid                      |
| License        | MIT                                     |
| minSdk         | 26                                      |
| versionCode    | 1                                       |
| versionName    | 0.1.0                                   |

The Application ID is permanent once published.

## Background

An OpenSpec project keeps its specifications in `openspec/` at the repository
root:

- `openspec/specs/<capability>/spec.md` holds the current specs.
- `openspec/changes/<name>/` holds active changes, each with artifacts such as
  `proposal.md`, `design.md`, `tasks.md`, a `.openspec.yaml`, and spec deltas
  under `specs/<capability>/spec.md`.
- `openspec/changes/archive/<YYYY-MM-DD>-<name>/` holds archived changes.
- `openspec/config.yaml` (or `config.yml`) and optionally `project.md` describe
  the project.

**First step:** read specgetty before writing anything. Its behaviour is
specified in `openspec/specs/` of the specgetty repo, and its parsers live in
`src/ui/specparse.go`, `src/ui/deltaparse.go` and `src/ui/taskitems.go`. The
fixtures in `src/ui/testdata/specs/` are the edge cases the Android parsers must
handle the same way. Do not derive the grammar from this briefing.

## Scope: Phase 1

- Add one or more repos by HTTPS URL, with an optional personal access token for
  private repos. QR scan, paste and share-into work as in beans-on-droid.
- Shallow clone (`depth 1`) into app-private storage.
- Refresh via pull-to-refresh: fetch plus hard reset to the remote branch.
- One project per repo, at `openspec/` in the repo root. No directory scanning
  and no store resolution.
- Load the project into an in-memory index on each clone or refresh. Parse specs
  and deltas lazily, when they are opened.

### Screens

1. **Repo list.** Every added repo with its statistics: spec count, active and
   archived change counts, and open tasks as `done/total` across active changes.
   Add, remove, switch.
2. **Project view.** A header with the repo name and its statistics, above four
   tabs:
   - **Overview:** counts, active changes by name, recently archived changes
     with their dates.
   - **Changes:** one list, grouped into Active (by name) and Archived (by
     archive date, newest first). Each row shows name, task progress
     `done/total`, number of specs, and archive date. Search as in specgetty's
     `change-search`: fuzzy on names by default, and a literal match on artifact
     text, with rows saying which files matched.
   - **Specs:** list of capabilities.
   - **Properties:** the project configuration (`project.md` rendered as
     Markdown, else `config.yaml` with YAML highlighting) and one row per
     workflow schema used by the changes, read from their `.openspec.yaml`.
3. **Change.** Tabs per artifact file found, plus a Specs tab. Artifacts render
   as Markdown. The tasks tab shows completion stats and draws checkboxes as
   boxes (not tappable in Phase 1).
4. **Change spec deltas.** From the change's Specs tab: an outline of every delta
   file, each requirement marked ADDED, MODIFIED, REMOVED or RENAMED. Tapping a
   node opens its card. For an active change, a node with an original in
   `openspec/specs/` offers diff, original and proposed, opening on the diff.
   Archived changes offer no comparison.
5. **Spec detail.** From the Specs tab: an outline of Purpose, requirements and
   their scenarios. Tapping a node opens its card. A scenario card lays out its
   WHEN/THEN clauses where they are recognised, and falls back to the raw text
   where they are not. A spec that does not fit the grammar still opens and says
   so.

On phones the outline and the card are two navigation levels. On wide screens
they sit side by side (Material 3 adaptive list-detail).

### States

Readable empty, loading and error states: auth failure, network error, and a
repo with no `openspec/` at its root ("no OpenSpec project here"), which is not
the same as an empty project.

## Non-goals (Phase 1)

- No editing, ticking tasks, archiving, discarding, exporting, committing or
  pushing.
- No store resolution and no scanning for nested `openspec/` directories.
- No calls to the `openspec` CLI. Schemas are shown by name only.
- No SSH authentication.
- No GitHub API usage. Plain git is the data layer.
- No background sync or notifications.

## Tech stack

- Kotlin, Jetpack Compose, Material 3, single activity, Navigation Compose.
- Current stable AGP, Kotlin and Compose BOM, with `gradle/libs.versions.toml`.
- **JGit 6.4.0**, pinned, with `desugar_jdk_libs_nio` and the `SystemReader`
  from beans-on-droid. Read that repo's README section on the git layer before
  touching the version.
- A FOSS Markdown renderer, the same one beans-on-droid uses.
- A YAML parser for `config.yaml` and `.openspec.yaml`.
- java-diff-utils for the delta comparison.
- Coroutines and a ViewModel per screen.
- Tokens encrypted with an Android Keystore key. Repo list in DataStore.

## Architecture

Keep the layers separate so Phase 2 only extends the lowest ones.

| Layer                    | Responsibility                                                |
|--------------------------|---------------------------------------------------------------|
| `repo/RepoStore`         | Clone, refresh, delete. Copied from beans-on-droid            |
| `project/ProjectLoader`  | Walks `openspec/` into changes, specs, config                 |
| `spec/SpecParser`        | `spec.md` to an outline. Only code that knows the spec grammar |
| `spec/DeltaParser`       | A change's spec file to a delta outline                        |
| `tasks/TaskParser`       | `tasks.md` to items and counts                                 |
| `index/ProjectIndex`     | Stats, grouping, ordering and search                           |
| `data/ProjectRepository` | App state and its transitions                                  |
| `viewmodel`, `ui`        | Screens, which talk only to view models                        |

## F-Droid readiness (hard requirements)

- Only FOSS dependencies. No Google Play Services, Firebase, Crashlytics,
  analytics or ads.
- No prebuilt binaries or jars committed.
- Include `LICENSE`.
- Fastlane metadata in `fastlane/metadata/android/en-US/` with `title.txt`,
  `short_description.txt`, `full_description.txt`, `changelogs/1.txt` and
  `images/phoneScreenshots/`.
- README and full description state that the app is not affiliated with the
  OpenSpec project.

## Quality bar and definition of done

- `./gradlew assembleDebug` succeeds.
- `./gradlew test` passes, with parser tests built on the fixtures from
  specgetty's `src/ui/testdata/specs/` plus real specs and changes from the
  specgetty repo itself, which tracks its own work in OpenSpec.
- Instrumented tests on an API 26 emulator cover add repo, browse changes,
  open a delta diff, and open a spec.
- `./gradlew lint` has no errors.
- Coverage is at least 70 percent overall and 80 percent on the parser and
  index packages.
- The specgetty repo itself (about 30 specs and its archive) loads and scrolls
  without noticeable lag.
- The README covers what the app is, how to build it, how to add a repo and
  token, and the Phase 2 roadmap.

## Phase 2, for orientation only

Ticking tasks, committed and pushed through JGit. `RepoStore` grows write,
commit and push. `TaskParser` grows a renderer that rebuilds `tasks.md` from the
file as it is on disk, as specgetty's `task-checkboxes` requires.

## If you get stuck

- If a file does not fit the grammar, show it as rendered Markdown and say so
  rather than hiding it.
- If specgetty and this briefing disagree about behaviour, specgetty's specs win
  and the difference is noted in the change's `design.md`.
