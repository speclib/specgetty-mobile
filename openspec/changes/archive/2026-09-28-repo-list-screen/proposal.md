## Why

Bean `specgetty-mobile-7swq`, milestone `specgetty-mobile-qofc`.

Five milestones of machinery and nothing on screen. This is screen 1 of
`BRIEFING.md`: every added repository with its statistics, and the means to add,
remove and switch.

The statistics are the reason this screen is worth building before anything
deeper. Spec count, active and archived change counts and open tasks as
`done/total` are the whole index exercised at once, and a wrong number here is
visible immediately, which is not true of a spec outline three levels down.

The add form is also the target of milestone 03's other half. Scanning a code
and sharing a link both end at this form, filled in and editable, so it is built
to be opened with a URL already in it even though nothing does that yet.

## What Changes

- `viewmodel/RepoListViewModel`: the list, its per-repository state, the add
  form and its validation, and the refresh.
- `ui/screen/RepoListScreen`: the rows, the statistics, pull-to-refresh, the add
  sheet and the remove confirmation.
- `ui/MarkdownText`: a Markwon-backed composable, needed here for nothing yet
  but the error explanations, and by every screen after this one.
- Empty, loading and error states drawn as `BRIEFING.md` describes, with the
  three failures saying which they are.

## Capabilities

### New Capabilities

- `repo-list-screen`: the first screen, listing repositories with their
  statistics and owning how one is added, removed or switched to.

## Impact

- New dependency: Markwon, with its table, strikethrough and task-list
  extensions. FOSS, and the renderer beans-on-droid uses.
- The view model is a plain class over `ProjectRepository`, unit tested on the
  JVM. The composable is tested on a device in milestone 09.
- Nothing here writes to a repository, in keeping with Phase 1.
