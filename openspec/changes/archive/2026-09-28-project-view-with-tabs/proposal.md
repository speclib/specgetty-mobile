## Why

Bean `specgetty-mobile-7osr`, milestone `specgetty-mobile-qofc`.

Screen 2 of `BRIEFING.md`, and the first screen that shows a project rather than
a count of one. A header with the repository name and its statistics, over four
tabs: Overview, Changes, Specs and Properties.

The Changes tab is the substantial one. It is a single list grouped into Active
and Archived, and it carries the search from `change-search`. The sigil grammar
that spec defines was written for a TUI where a leading `'` or `:` is easy to
type and easy to see. On a phone it is neither, so the sigils stay, since a
query typed in either tool should mean the same thing, and a control is added
beside the field that writes them. The grammar is the contract; the keyboard is
not.

The Properties tab shows `project.md` as Markdown when there is one and the
`config.yaml` otherwise, plus a row per workflow schema the changes use. That
last part is why the loader reads every change's `.openspec.yaml`, and this is
where it becomes visible.

## What Changes

- `viewmodel/ProjectViewModel`: the project for one repository, the selected
  tab, and the search query and its results.
- `ui/screen/ProjectScreen`: the header, the four tabs, and their content.
- Navigation from the repository list into this screen and back.
- A `YamlText` composable: monospaced, with comments and keys distinguished,
  which is as far as `BRIEFING.md`'s "YAML highlighting" needs to go.

## Capabilities

### New Capabilities

- `project-view`: the four tabs over one repository's project, and how a change
  is found in them.

## Impact

- No new dependencies.
- Search runs against the index, so a `:` query reads artifact files. On the
  vendored corpus that is 342 files; the tab shows progress rather than
  appearing to hang, and milestone 09 measures it on a device.
- Opening a change or a spec navigates to screens milestones 07 and 08 build.
  Until they exist the rows are present and inert, which is honest rather than
  hidden.
