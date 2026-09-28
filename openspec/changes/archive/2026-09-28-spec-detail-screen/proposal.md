## Why

Bean `specgetty-mobile-zg3i`, milestone `specgetty-mobile-mryq`.

Screen 5 of `BRIEFING.md`, and the point of the app. Everything so far counts
specs; this reads one: an outline of Purpose, requirements and their scenarios,
with a card per node. A scenario's card lays out its WHEN and THEN clauses where
they are recognised and falls back to the raw text where they are not.

The last sentence of the briefing's description is the one that matters most:
a spec that does not fit the grammar still opens and says so. specgetty learned
this the expensive way, and its spec is explicit about why:

> The reader SHALL NOT be left with one transient line for a file with several
> faults, and the file SHALL remain readable as markdown

So a file that cannot be structured opens to a report of every reason with its
line, and the whole file is still reachable as Markdown. Hiding it would make
the one spec a person most needs to fix the one they cannot look at.

## What Changes

- `viewmodel/SpecViewModel`: the parsed spec, the outline, the selected node,
  the problems when there are any, and the raw text.
- `ui/screen/SpecScreen`: the outline, the cards, the clause layout, and the
  report with its Markdown fallback.

## Capabilities

### New Capabilities

- `spec-view`: reading one capability's spec, and what happens when the file
  does not fit the grammar.

## Impact

- No new dependencies.
- The spec is parsed through the repository's cache, so leaving a spec and
  returning to it does not parse it again.
- The outline and the card are two navigation levels here. Putting them side by
  side on a wide screen is the next change, and is deliberately not mixed into
  this one.
