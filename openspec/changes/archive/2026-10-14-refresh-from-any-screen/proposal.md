## Why

Tracks [specgetty-mobile-4dtx](../../../.beans/specgetty-mobile-4dtx--a-single-repo-or-store-should-be-refreshable.md)
and [issue 1](https://github.com/speclib/specgetty-mobile/issues/1).

Only the repository list can be refreshed. A reader four screens into a change
has to walk back to the list, pull, and walk back in, losing their tab, their
selected card and their place in a long document on the way. The gesture belongs
where the reading happens.

Underneath that sits a smaller untruth. The state machine treats a refresh as a
first clone: it sets the project to loading, which empties the screen, and on
failure sets it to failed, which replaces a readable project with an error.
`RepoStore.refresh` fetches and then hard-resets, so a failure is a fetch
failure and the working copy on disk is exactly as it was. Throwing away what it
holds, because the network was unreachable for a second in a tunnel, is wrong.

## What Changes

- Pull to refresh on the project, change, spec and delta screens. Each refreshes
  the repository the screen is reading, which is the only thing git can do and
  is what the reader means.
- A refresh of a project that is already loaded keeps its index current while it
  runs. The screen keeps its content, its tabs and its place, and says the
  refresh is happening without replacing anything.
- A refresh that fails keeps the index it already had, because the working copy
  was never touched. The failure is reported alongside the content rather than
  instead of it, and carries the action it calls for: retrying a network
  failure, authorizing a repository the authorization does not cover.
- A refresh with no index to fall back on is unchanged: loading, then loaded,
  no project here, or failed with a reason.
- A refresh outlives the screen that asked for it. It runs today in the asking
  view model's scope, so leaving the screen cancels the fetch. The list is the
  only screen that can ask and the list is never left, so this has never
  happened; putting the gesture on a screen that can be popped makes it
  reachable, and a reader who taps back is not cancelling a fetch.
- One refresh per working copy, observable from every screen reading it. Two
  rows sharing a store share the spinner, and pulling again while one is in
  flight does nothing rather than racing a second fetch.
- A change screen re-reads its artifacts after a refresh. Their text is cached
  by file name with nothing to invalidate it, so an open proposal tab would go
  on showing text the refresh had replaced.

## Capabilities

### New Capabilities

None. The gesture is new on four screens, the behaviour behind it is not.

### Modified Capabilities

- `app-state`: a refresh is no longer the same transition as a clone. Loading
  and failed are what a repository with nothing to show says; a loaded
  repository stays loaded across a refresh and across a refresh that fails. A
  refresh belongs to the working copy rather than to the screen that asked.
- `repo-list-screen`: a row that is loaded keeps its counts through a failed
  refresh, and the failure is told rather than drawn in place of the row.
- `project-view`: the screen can be pulled to refresh.
- `change-view`: the screen can be pulled to refresh, and its artifacts are
  re-read when one lands.
- `spec-view`: the screen can be pulled to refresh, including from an open card,
  and a card whose node the refresh removed says so.
- `delta-view`: the screen can be pulled to refresh, including from an open
  comparison.

## Impact

| File                                   | What changes                               |
|----------------------------------------|--------------------------------------------|
| `data/ProjectRepository`               | owns the refresh and its scope, keyed by clone; loading and failed only with no index |
| `viewmodel/ProjectViewModel`           | exposes the gesture and the outcome        |
| `viewmodel/ChangeViewModel`            | the same, plus invalidating the artifact text it caches |
| `viewmodel/SpecViewModel`              | the same                                   |
| `viewmodel/DeltaViewModel`             | the same                                   |
| `viewmodel/RepoListViewModel`          | reads the shared refresh state instead of its own flag |
| `ui/screen/ProjectScreen`              | the gesture, a report, scrollable empty states |
| `ui/screen/ChangeScreen`               | the same                                   |
| `ui/screen/SpecScreen`                 | the same                                   |
| `ui/screen/DeltaScreen`                | the same                                   |
| `ui/screen/RepoListScreen`             | reports a failed refresh of a loaded row   |

No new dependency: `PullToRefreshBox` is already in use on the list, and the
snackbar host it needs comes from the Material 3 artifact already on the
classpath. Nothing in `repo/`, `project/`, `spec/` or `index/` is touched, so
the parsers and the git layer are untouched by this change.
