## Why

Bean `specgetty-mobile-2fuj`, milestone `specgetty-mobile-8swu`.

A change's progress is the one number shown in three places: the repo list, the
project header and every row of the changes list. It comes from counting
checkboxes in `tasks.md`, and specgetty is exact about which ones count.

specgetty's `src/ui/checkbox.go` matches `- [ ] ` and `- [x] ` at column zero
and nothing else, and says why:

> an indented or `*` prefixed checkbox is not counted in the totals, so drawing
> it as a box or letting it be toggled would make the display disagree with the
> numbers beside it

That is the rule worth copying exactly. A parser that is more generous than the
counter produces a screen whose boxes and whose totals contradict each other,
and the reader has no way to tell which is lying.

The second rule is that a task is rarely one line. OpenSpec's own generators
wrap a task and indent the continuation, so the checkbox line and the lines
under it are one item to a reader and have to be one item here.

## What Changes

- `tasks/TaskParser`: `tasks.md` to items with their done state, their
  continuation lines, and the source line each came from.
- `TaskStats`: done and total, and the aggregate across a set of changes, which
  is what the repo list shows.
- Section headings are carried, so the tasks tab can group as the file does.

## Capabilities

### New Capabilities

- `task-parsing`: counting and reading the checkboxes of a `tasks.md`, by the
  same rule that decides the totals.

## Impact

- No new dependencies.
- Each item keeps its source line index. Phase 2 needs it to rewrite the file in
  place, as specgetty's `task-checkboxes` requires. Nothing here writes.
- `tasks` is named in the 80 percent rule.
