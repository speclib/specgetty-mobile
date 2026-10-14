---
# specgetty-mobile-4dtx
title: a single repo or store should be refreshable
status: completed
type: feature
priority: normal
openspec-link: openspec/changes/archive/2026-10-14-refresh-from-any-screen
created_at: 2026-10-06T20:56:34Z
updated_at: 2026-10-14T00:00:00Z
---

Currently only all repo's can be refreshed with a swipe down. When in a single project a swipe down should refresh the repo of this project.

## Summary of Changes

Shipped as `openspec/changes/archive/2026-10-14-refresh-from-any-screen`.

Pull to refresh now works on the project view, a change, a spec and a delta, not
only on the repository list. Each fetches the repository the screen is reading,
which also answers the "or store" half: several list entries sharing one working
copy share one fetch, and all of them reload from it.

What the gesture needed underneath it:

- A refresh of a project that is already loaded keeps its index. Loading and
  failed now mean there is nothing to show, so a screen is never asked to blank
  itself for the length of a fetch, and a failed fetch no longer replaces a
  readable project with an error. Fetching and resetting leaves the working copy
  alone when the fetch is what failed, so there was never anything to take away.
- The refresh belongs to the working copy rather than to the screen that asked.
  It ran in the asking view model's scope, so tapping back would have cancelled
  it. The repository now owns it, one fetch per clone, observable from every
  screen reading that clone.
- The failure is told as a snackbar carrying what can be done about it: Try
  again for a repository that could not be reached, Authorize for one the
  authorization does not cover.

Three bugs the gesture made reachable, all fixed here:

- A list shorter than the window ended where its content ended, leaving a dead
  strip below it that answered no pull. Found by dumping the semantics tree when
  the Overview tab would not refresh: the list stopped at 879px on a 1794px
  screen and the gesture began below it.
- The project view read its index through a plain getter, and the change screen
  built its tab row the same way. Neither is a snapshot read, so neither redrew
  when a refresh landed under an open screen.
- An artifact opened in a change was cached by file name with nothing to
  invalidate it, so a refresh that rewrote it left the old text on screen.

Verified by 18 view model tests, 7 instrumented tests on the API 26 emulator,
and the gate: 623 unit tests, lint clean, coverage 92 percent of instructions.

Not done, and deliberately: following a change into the archive when a refresh
archives it under the reader, and refreshing on a timer or on resume.
