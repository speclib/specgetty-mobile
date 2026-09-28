## Why

Bean `specgetty-mobile-bl2k`, milestone `specgetty-mobile-8swu`.

A spec file inside a change is a delta, and almost every rule that makes a main
spec valid inverts in it. specgetty puts it plainly in `src/ui/deltaparse.go`:

> A delta header is the structure rather than a fault, `## Purpose` is written
> only for a capability the change introduces, and a requirement being removed
> carries a reason where a scenario would be.

So this is a second entry point rather than a flag on the spec parser. The two
share the mechanical half that landed with `spec-parser`, and differ exactly
where the validity rules differ, which is the part worth writing down.

Two rules are easy to get wrong and are the reason this has its own tests. A
requirement sitting under no delta header is never applied by `openspec
archive`, so it has to be reported rather than silently shown. And an operation
heading the four known ones do not match is carried through as written, because
the reader is better served by seeing what the file says than by having it
hidden for being unrecognised.

## What Changes

- `spec/DeltaParser`: one spec file of a change to an outline whose requirements
  carry their operation, or the reasons the file cannot be applied.
- Nodes carry the capability they came from, so a change's several deltas can be
  joined into one outline without losing which file a node belongs to.
- specgetty's own `openspec/` tree vendored into test resources: 28 specs and 57
  archived changes holding 110 delta files, which is the corpus `BRIEFING.md`
  names as the definition of done.

## Capabilities

### New Capabilities

- `delta-parsing`: reading a change's spec file into requirements marked with
  what the change does to them, and reporting what `openspec archive` would not
  apply.

## Impact

- The vendored corpus is 0.97 MB of markdown across 342 files. It lives in
  `app/src/test/resources/` and is not packaged into the APK.
- It serves more than this change: the project loader, the index and the
  performance measurement in milestone 09 all need a real project, and one that
  tracks its own work in OpenSpec is the honest specimen.
- No new dependencies.
