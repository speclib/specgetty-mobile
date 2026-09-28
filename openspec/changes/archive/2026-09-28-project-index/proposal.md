## Why

Bean `specgetty-mobile-hqy0`, milestone `specgetty-mobile-61i0`.

The loader produces a project. The screens need it arranged: counts for the
header, changes grouped into active and archived, the archive newest first, and
a search that narrows a list of eighty-odd changes to the one being looked for.

`BRIEFING.md` sends the search to specgetty's `change-search`, and that spec is
worth following exactly rather than approximating, because its central decision
is one an approximation would get wrong:

> Names are matched by fuzzy subsequence because they are short and
> half-remembered; bodies are matched literally because a fuzzy subsequence
> matches nearly any document of real length.

A fuzzy search over artifact text returns everything, which is the same as
returning nothing. So the sigils are not decoration: `'` and `:` select
matchers that behave differently for a reason.

Fuzzy ranking is ported from `sahilm/fuzzy` rather than invented, since that is
what specgetty ranks with, and an ordering that disagrees with the TUI would
make the two tools feel like they disagree about the project.

## What Changes

- `index/Fuzzy`: subsequence matching with sahilm/fuzzy's scoring, ported.
- `index/Query`: the sigil grammar and the smart-case rule.
- `index/ProjectIndex`: counts, active and archived grouping, archive ordering,
  and search that reports which files matched.

## Capabilities

### New Capabilities

- `project-index`: the counts, grouping, ordering and search the screens read,
  computed over a loaded project.

## Impact

- No new dependencies. The fuzzy scoring is about eighty lines of Kotlin.
- Body search reads artifact and delta files from disk when a `:` query runs,
  and not before. A search over the vendored corpus reads 342 files, which is
  measured in the tests rather than assumed to be fast.
- `index` is named in the 80 percent rule.
