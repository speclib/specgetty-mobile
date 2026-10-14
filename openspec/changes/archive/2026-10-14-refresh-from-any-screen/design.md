## Context

See `proposal.md` for why. What shapes the approach is what is already there.

`ProjectRepository` owns the clone, the load and the index, and publishes
`states: Flow<Map<String, ProjectState>>`. Every screen reads from it and
re-resolves itself on each emission, by name rather than by holding an object:
`ChangeViewModel` finds its change by `dir.name`, `SpecViewModel` finds its
capability by name and its open card by `node.path`, `DeltaViewModel` the same.
`ChangeScreen` already renders "This change is no longer in the project." The
screens were built to survive a reload landing under them. Only the gesture and
the state semantics are missing.

Two existing facts decide most of this:

- `RepoStore.refresh` fetches and then hard-resets. A failure is a fetch failure
  or a credential failure, both of which happen before the reset, so the working
  copy is untouched. There is no half-reset to defend against in practice.
- `ProjectRepository.refresh` already fans out: one fetch per `cloneId`, then
  every entry on that clone reloads. The data layer already does what "a single
  repo or store should be refreshable" asks for. The bean's "or store" needs no
  code at all.

The one thing that is wrong today is scope. `refresh` is a `suspend fun` called
as `scope.launch { repository.refresh(id) }` from a view model, where `scope` is
`viewModelScope`. `RepoListViewModel` is the navigation root and is never
popped, so its refreshes always finish. Any other screen can be popped, and
popping it cancels the fetch.

## Goals / Non-Goals

**Goals:**

- One mechanism for the gesture, used by five screens, not five copies of it.
- A refresh that is a property of the working copy, observable from anywhere.
- `ProjectState` keeps its five values and its meaning. No sixth state, no
  boolean beside it.

**Non-Goals:**

- Refreshing a single file, a single change or a single spec. Git fetches a
  repository; the gesture means the repository wherever it is made.
- Following a change into the archive when a refresh archives it under the
  reader. The screen says it is gone, which is correct; following it is its own
  idea.
- Any change to `RepoStore`, the parsers or the index.
- Offline detection, retry with backoff, or refreshing on a timer.

## Decisions

### Loading and refreshing are two facts, not one

`ProjectState.Loading` means there is nothing to show. Refreshing means
something is showing and is being replaced. Conflating them is what makes a
screen blank itself for the length of a fetch.

So `refresh` sets `Loading` only when `stateOf(id)` is not `Loaded`, and sets
`Failed` only when there was no index to keep. A loaded repository stays
`Loaded` all the way through a refresh, successful or not.

Alternatives considered:

- `Loading(previous: ProjectIndex?)`. Carries the stale index in the state, so
  every `when` over the state has to decide what to do with a loading state that
  has content. Spreads the problem rather than containing it.
- Each view model remembers its last `Loaded` index and shows it through
  `Loading`. Five copies of the same guard, and the repo list would still need
  its own answer.

### The refresh runs in the repository's scope, keyed by clone

`ProjectRepository` gains a `CoroutineScope` of its own, a
`Map<cloneId, Job>` of what is in flight, and
`refreshing: StateFlow<Set<String>>` of clone ids. A screen asks for a refresh
and does not await it.

This gives three things at once: a refresh survives the screen being popped;
asking twice is idempotent, because an in-flight job for that clone means there
is nothing to start; and the indicator is correct on every screen reading that
clone, which is what two rows sharing a store need. `RepoListViewModel`'s
`_refreshing` flag becomes a view over this instead of its own state.

The scope belongs to the repository, which lives in `SpecgettyApplication` and
outlives every screen, so there is nothing to cancel it prematurely.

Alternative considered: keep `refresh` a `suspend fun` and have the calling
screen await it and report the outcome. Cheapest to write, and the only one of
the three mechanisms a reader can tell apart, because it is the one where
tapping back silently abandons the fetch.

### The outcome is held per clone, not emitted as an event

A `SharedFlow` of outcomes is the obvious shape for something transient, and is
wrong here: the refresh outlives the screen, so the screen collecting when it
lands may not be the screen that asked, and an event emitted while a screen is
being replaced is lost.

Instead the repository holds `outcomes: StateFlow<Map<String, RefreshOutcome>>`
keyed by clone id, carrying the `RepoError` of the last failed refresh. Starting
a refresh clears the entry for that clone; a screen clears it when it has shown
it. Whichever screen is in front when the failure lands shows it.

### The failure carries its next step

The `RepoError` is already specific enough to act on. What is shown is a
snackbar, with an action where there is one to offer:

| `RepoError`              | Action              |
|--------------------------|---------------------|
| `Network`                | Try again           |
| `NoAccessToRepository`   | Authorize           |
| `Authentication`         | none                |
| `PointsElsewhere`        | none                |
| `NoOpenSpecProject`      | none                |
| `Unknown`                | none                |

The tunnel is the common case, so Try again is the one that earns its place.
`AuthorizeScreen` already exists for the second.

### One composable carries the gesture

All five screens have a `Scaffold`, a `repoId` and the same three needs: a
`PullToRefreshBox`, a `SnackbarHost`, and the call. They get a shared
composable that takes the repo id and the content, so the gesture is written
once. Without it the same twelve lines land in five files and drift.

### Empty states are made scrollable

Material 3's pull-to-refresh rides nested scroll, so it only fires from a child
that reports a scroll. The centred `Column`s that say "No OpenSpec project
here", "This change is no longer in the project" and "This change touches no
capability" report nothing, so the gesture would be dead exactly where a fetch
is most wanted. Giving them `verticalScroll` costs nothing visually and makes
the gesture work everywhere.

### The change screen's artifact cache gets a key

`ChangeViewModel.artifacts` is a `mutableMapOf<String, ArtifactContent>` filled
by `getOrPut`, with nothing that invalidates it. `SpecCache` next door is keyed
on path, length and modification time precisely so a rewritten file is parsed
again. The artifact map follows it rather than being cleared wholesale, so that
refreshing a change with a long design document does not re-read everything the
refresh left alone.

This is a defect, not a feature, but it is unreachable today: the only way to
refresh is to leave the screen, which clears the view model. The gesture is what
makes it reachable, so it is fixed here.

## Risks / Trade-offs

**A refresh landing under an open card moves the reader.** The outline is
rebuilt, and a card open on a node the refresh deleted has nothing behind it.
Mitigation: the view models already re-resolve by name and by path, so the
common case lands cleanly; the cases that cannot are spelled out as scenarios
and say what happened instead of showing stale text.

**A refresh now outlives its screen, so it can land on a screen that knows
nothing about it.** Mitigation: that is the point, and the outcome is held per
clone rather than emitted, so whichever screen is in front reports it.

**The states a screen reads change meaning.** Code that treats `Loading` as "a
fetch is happening" is now wrong. Mitigation: only the repository sets these,
the five view models read them, and the change is small enough to grep for.

**Keeping a stale index after a failure could mislead.** A reader could believe
they are looking at the remote when they are looking at a copy from yesterday.
Mitigation: the failure is told, with its reason, and the alternative is worse:
replacing a readable project with an error page because of one unreachable
moment.

## Migration Plan

None. No stored data changes shape, no entry in the repository list is rewritten
and nothing on disk moves. A build without this change and a build with it read
the same working copies and the same catalog.
