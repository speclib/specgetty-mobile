# Specgetty on Droid

An Android app for browsing and reading [OpenSpec](https://github.com/Fission-AI/OpenSpec)
projects in git repositories. It is the mobile counterpart of
[specgetty](https://github.com/speclib/specgetty), the OpenSpec TUI, and borrows
its git layer from [beans-on-droid](https://github.com/mipmip/beans-on-droid).

Phase 1 is read-only: add repos by HTTPS URL, shallow clone them, and browse the
project at `openspec/` in the repo root. No editing, no ticking tasks, no
pushing, no calls to the `openspec` CLI, no GitHub API. Plain git is the data
layer.

Not affiliated with the OpenSpec project.

## The source of truth

`BRIEFING.md` in this repo defines what Phase 1 is. OpenSpec proposals refine
it; they do not replace it. Fixed settings, screens, non-goals, the tech stack,
the layer boundaries and the definition of done all live there.

Two rules from the briefing that decide arguments:

- Read specgetty before writing a parser. Its grammar is specified in its
  `openspec/specs/`, implemented in `src/ui/specparse.go`, `src/ui/deltaparse.go`
  and `src/ui/taskitems.go`, and pinned by the fixtures in
  `src/ui/testdata/specs/`. Do not derive the grammar from the briefing.
- Where specgetty and the briefing disagree about behaviour, specgetty's specs
  win, and the difference is noted in the change's `design.md`.

Local checkouts to read from:

| Repo            | Path                             |
|-----------------|----------------------------------|
| specgetty       | `~/gh.speclib/specgetty`         |
| beans-on-droid  | `~/gh.mipmip/beans-on-droid`     |

## Commands

```bash
nix develop                     # dev shell with the JDK and Android SDK
nix develop .#emulator          # shell with the emulator and system images
scripts/gate.sh                 # the gate: flake check, build, test, lint, coverage
./gradlew assembleDebug         # build the debug APK
./gradlew test                  # unit tests
./gradlew lint                  # must have no errors

beans list --ready              # what to work on next
beans prime                     # the beans workflow
openspec list                   # active changes

scripts/ship-change.sh <change-name> [subject]   # gate, archive, commit, push
```

The gate is in two halves. `nix flake check` covers what a pure derivation can
do: shellcheck over `scripts/`, and `nixpkgs-fmt` over `flake.nix`. Gradle needs
network access to resolve dependencies, which a sandboxed derivation does not
get, so the build, test, lint and coverage half runs inside `nix develop` from
`scripts/gate.sh`. That script skips the gradle half until `./gradlew` exists.

The coverage floor is 70 percent overall and 80 percent on the parser and index
packages. Once `./gradlew` exists, the gate fails while there are no tests. That
is intended: tests come before the first ship, and a failed gate never leaves a
half-shipped change.

## Version control

`jj`, with the remote at `git@github.com:speclib/specgetty-mobile.git`. Commit
after every archived OpenSpec change. Commits are authored by Pim Snel alone: no
`Co-authored-by` trailers and no generated-with attribution.

## Beans

When I refer to issues like specgetty-mobile-rn3b checkout the task
in @.beans/specgetty-mobile-rn3b-*.md

In this project we will use these tasks as epics for making openspec proposals.

WHEN you create a proposal at a link to this task in the proposal.md.
WHEN a bean is used to create an proposal change the status to "in-progress"
WHEN a proposal is archived add the link to the archived proposal in the frontmatter of this task like this:

```
openspec-link: openspec/changes/archive/....
```

You are allowed to update these statuses in the task frontmatter:

- in-progress
- todo
- draft
- completed
- scrapped

When making changes you are allowed to update the date/time in `updated_at` in the task frontmatter

Besides updating status and openspec-link, you are NOT ALLOWED to modify the contents of the task file.
