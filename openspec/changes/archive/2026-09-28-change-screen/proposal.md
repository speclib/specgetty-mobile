## Why

Bean `specgetty-mobile-vqw2`, milestone `specgetty-mobile-v0ko`.

Screen 3 of `BRIEFING.md`: a tab per artifact file found, plus a Specs tab.
Artifacts render as Markdown; the tasks tab shows completion stats and draws
checkboxes as boxes, not tappable in Phase 1.

"Per artifact file found" is the part worth stating plainly. A change is not
required to have a `proposal.md`, a `design.md` and a `tasks.md`, and the
vendored corpus proves it: a tinychange change has no design, and some have
files nobody planned for. So the tabs come from the directory listing rather
than from a list of expected names, and a change with one file gets one tab.

The tasks tab is the only one that is not plain Markdown. It draws each task's
checkbox as a box so that what is on screen agrees with the count beside it,
which is the rule `task-parsing` already owns. The boxes are drawn, not tapped:
ticking one is Phase 2, and a box that looks tappable and is not would be worse
than a box that plainly is not.

## What Changes

- `viewmodel/ChangeViewModel`: the change, its artifacts as tabs, the selected
  tab, the parsed tasks and the capabilities it touches.
- `ui/screen/ChangeScreen`: the tab row, Markdown artifacts, the tasks tab with
  its boxes and stats, and the Specs tab listing the deltas.
- Navigation into the delta view, which milestone 07's other half builds.

## Capabilities

### New Capabilities

- `change-view`: reading one change: its artifacts, its progress, and which
  capabilities it touches.

## Impact

- No new dependencies. Markwon is already here.
- Artifacts are read when their tab is opened, not when the change is opened, so
  a change with a long design document costs nothing until it is looked at.
- Nothing here writes. The checkbox is a glyph.
