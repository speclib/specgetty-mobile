# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- The README now shows the app rather than only describing it: a recording of a
  spec being stepped through card by card, screenshots, and a way to download a
  signed APK instead of building one yourself.

## [0.1.0] - 2026-09-29

### Added

- Releases. A tagged version is built, signed and published with an APK you can
  install, and its notes come from this file. A tag that disagrees with what the
  app is built as is refused rather than published.
- Every push is checked in public, and the repository shows what it is made of:
  its specs, its requirements, its open tasks and its test coverage.
- Authorizing a private GitHub repository from the app, instead of typing an
  access token. The credential is read-only, expires in about eight hours and
  renews itself. The app reads back which repositories it actually reaches and
  says so, rather than reporting success and failing at the clone.
- Several OpenSpec projects in one repository. A repository holding more than
  one lists them and asks which to add; each chosen project becomes its own row,
  and the rows share one downloaded copy and one credential. Removing a row
  leaves both alone while another row from that repository remains.
- A repository whose `openspec/config.yaml` points at a store now says which
  store and which file, rather than reporting that there is no project.
- Project scaffolding: OpenSpec, beans, the nix flake and the ship script.
- An Android application that builds and installs, showing an empty repository
  list. Nothing can be added to it yet.
- The git layer: shallow clone, refresh by fetch and hard reset, and delete,
  over HTTPS with an optional access token. Failures say whether they were an
  authentication problem, a network problem, or a repository with no OpenSpec
  project in it.
- A repository list that survives a restart, with access tokens encrypted under
  an Android Keystore key and kept apart from the list itself.
- URL capture: a repository address is found in whatever text it arrives in, and
  a forge page URL is turned into one that actually clones.
- The spec parser, reading a `spec.md` by the rules OpenSpec's own parser uses.
  A file that does not fit them says every reason why, with the line for each.
- The delta parser, reading a change's spec file into requirements marked ADDED,
  MODIFIED, REMOVED or RENAMED, and reporting what archiving would not apply.
- The task parser, counting a change's checkboxes by the same rule that decides
  the totals, so the boxes drawn and the number beside them cannot disagree.
- The project loader, walking `openspec/` into its specs, its active and
  archived changes, their artifacts, their workflow schemas and their progress.
- The project index: counts, grouping, archive ordering, and search. Typing
  matches change names loosely; a leading `:` searches the text inside a change
  instead and says which files it found the words in.
- App state: adding, refreshing, removing and switching repositories, with
  loading, empty and error states kept apart. A repository with no OpenSpec
  project in it says so rather than looking like a failure.
- The repository list screen: add a repository by its HTTPS URL with an optional
  access token, see its specs, changes and task progress at a glance, pull to
  refresh, and remove one after confirming.
- The project view: a header with the project's size over four tabs. Overview,
  Changes with its search, Specs, and Properties showing `project.md` or the
  configuration and the workflow schemas the changes use.
- The change screen: a tab per artifact file the change actually has, rendered
  as Markdown, a tasks tab drawing each checkbox as a box beside the progress,
  and a Specs tab listing the capabilities the change touches.
- The spec delta view: every requirement a change adds, modifies, removes or
  renames, and for a change not yet archived, the difference between a modified
  requirement and the one it modifies.
- The spec screen: an outline of a capability's Purpose, requirements and
  scenarios, with a card for each. A file that does not fit the grammar opens to
  every reason why, with its line, and stays readable as Markdown.
- A repository URL can be scanned from a QR code or shared in from another app
  instead of typed. Either way it lands in the add form, editable, and nothing
  is cloned until you press Add. An access token is never taken from a capture.
- An F-Droid readiness audit, Fastlane store metadata, and a README covering
  what the app is, how to build and install it, and how to add a repository.
- On a wide screen the outline of a spec or a delta sits beside the card instead
  of above it, and the back action means what each arrangement makes it mean.
- End-to-end tests on an API 26 emulator covering adding a repository, browsing
  its changes, opening a spec delta's difference, and reading a spec.
- Store screenshots, taken by driving the app on an emulator rather than drawn.
- A spec can be read straight through: a card now has next and previous buttons
  that step through the outline in the file's own order, and the outline marks
  the node you are on. Neither end wraps.

### Fixed

- Scanning a QR code no longer crashes the app the moment a code is recognised.
  The camera handed the decoded text straight to the screen from its own thread;
  it is now passed to the screen and acted on there. A code recognised in
  several frames running is also acted on once rather than once per frame.
- The Properties tab on a project no longer has its label broken across two
  lines on a phone. The four tabs now take the width their labels need, and the
  row scrolls when they do not all fit.
