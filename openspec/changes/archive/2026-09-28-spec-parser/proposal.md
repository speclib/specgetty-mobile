## Why

Bean `specgetty-mobile-74c9`, milestone `specgetty-mobile-8swu`.

`BRIEFING.md` is explicit that the grammar is not to be derived from it. It is
specified in specgetty's `openspec/specs/spec-detail-view/spec.md`, implemented
in `src/ui/specparse.go`, and pinned by thirteen fixtures in
`src/ui/testdata/specs/`, each taken from a live spec rather than invented.

specgetty's own rule, transcribed from OpenSpec 1.10.0, is worth restating
because it is what makes this parser small: the file decides two separate
questions, and only one of them belongs here.

> whether a file is a spec, which OpenSpec decides, and how to draw it, which
> this view decides

So the grammar is exactly OpenSpec's, and nothing is added to it. Where OpenSpec
is silent, as it is about how a scenario's content is written, what the file
says is carried through rather than made to fit a convention no tool enforces.
The `- **WHEN**` shape lives in a template and an error message, not in a rule,
which is why a scenario written as a bare paragraph is valid and must not render
blank. That was a real bug in specgetty, bean `specgetty-vfnn`, and the fixtures
exist because of it.

This change brings over the mechanical half that the delta parser will share:
the fence mask, the section reader and the scenario content reader.

## What Changes

- `spec/Markdown`: the fence mask, heading levels, section bodies and the
  CommonMark thematic break test, shared by both grammars.
- `spec/SpecParser`: `spec.md` to an outline of Purpose, requirements and
  scenarios, or a list of the reasons it is not a spec, each with its line.
- `spec/SpecModel`: the outline as values, with a stable path per node.
- specgetty's thirteen fixtures, copied verbatim into test resources, with a
  test asserting each lands on the same side of the grammar as it does there.

## Capabilities

### New Capabilities

- `spec-parsing`: reading a main spec by the rules OpenSpec's own parser uses,
  and reporting every reason a file does not fit them.

## Impact

- No new dependencies. Pure Kotlin over strings.
- The fixtures are MIT and by the same author, and are copied rather than
  paraphrased so that a difference in behaviour shows up as a failing test.
- `spec` is named in the 80 percent rule.
- Where this parser and `BRIEFING.md` could be read as disagreeing, specgetty
  decides, as the briefing instructs.
