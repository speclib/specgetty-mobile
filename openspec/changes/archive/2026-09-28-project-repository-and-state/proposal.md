## Why

Bean `specgetty-mobile-9zzg`, milestone `specgetty-mobile-61i0`.

Everything below this point is a function of its inputs: a parser takes text, the
loader takes a directory, the index takes a project. Nothing yet holds what the
app is currently showing, or knows that a clone is in flight.

`BRIEFING.md` asks for readable empty, loading and error states, and names three
errors that must be told apart: auth failure, network error, and a repo with no
`openspec/` at its root, which is not the same as an empty project. Making those
states values rather than a pair of booleans is what stops a screen inventing a
fourth one by accident.

Two decisions belong here. The project is reloaded into an in-memory index on
each clone and refresh, as the briefing specifies. And a spec is parsed when it
is opened and then kept, because the same spec is opened, left and reopened as
a reader moves between the outline and its cards, and parsing it three times
would be three times the work for one answer.

## What Changes

- `data/ProjectState`: absent, loading, loaded, no project here, or failed with
  the error that caused it.
- `data/ProjectRepository`: add, remove, switch, refresh. Owns the clone, the
  load and the index, and exposes state as a flow.
- A parse cache keyed by file and its modification time, so a refresh that
  rewrote a spec reparses it and one that did not does not.

## Capabilities

### New Capabilities

- `app-state`: what the app is showing, what it is doing, and what went wrong,
  as values the screens read rather than derive.

## Impact

- No new dependencies.
- The repository takes its store, registry, loader and dispatcher by injection,
  so the whole layer is unit tested without an emulator.
- Parsing stays lazy. Opening the repo list of a project with 28 specs parses
  none of them.
