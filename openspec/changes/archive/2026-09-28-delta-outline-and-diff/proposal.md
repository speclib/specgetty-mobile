## Why

Bean `specgetty-mobile-v6en`, milestone `specgetty-mobile-v0ko`.

Screen 4 of `BRIEFING.md`: an outline of every delta file, each requirement
marked ADDED, MODIFIED, REMOVED or RENAMED, with a card per node. For an active
change, a node with an original in `openspec/specs/` offers diff, original and
proposed, opening on the diff. Archived changes offer no comparison.

The rule about archived changes is the one worth stating, because it looks like
an omission and is not. An archived change has already been applied: the
requirement in `openspec/specs/` **is** the proposed version. Diffing them would
show no difference and imply the change did nothing, which is worse than
offering nothing.

The comparison needs the requirement's text, and the parsers produce an outline
rather than source ranges. Rather than widen the model, the text is sliced out
of the file by the same masked-line rules the parsers use, so a `### Requirement:`
heading inside a fenced block does not cut the wrong slice.

## What Changes

- `spec/RequirementSource`: the source text of one requirement, sliced by the
  shared grammar helpers.
- `spec/Comparison`: find a delta requirement's original in the main spec, and
  produce a unified diff of the two.
- `viewmodel/DeltaViewModel`: the change's delta files parsed, the outline, the
  selected node, and its comparison when there is one.
- `ui/screen/DeltaScreen`: the outline, the operation marks, the card, and the
  diff, original and proposed views.

## Capabilities

### New Capabilities

- `delta-view`: reading a change's spec deltas and comparing a modified
  requirement with the one it modifies.

## Impact

- New dependency: java-diff-utils, which `BRIEFING.md` names. Apache-2.0.
- Deltas are parsed when this screen opens, through the repository's cache, so
  opening the same change twice parses once.
- A delta file that does not fit the grammar opens showing why, as a main spec
  does.
