# Specgetty on Droid

An Android app for browsing and reading [OpenSpec](https://github.com/Fission-AI/OpenSpec)
projects in git repositories. It is the mobile counterpart of
[specgetty](https://github.com/speclib/specgetty), the OpenSpec TUI, and borrows
its git layer from [beans-on-droid](https://github.com/mipmip/beans-on-droid).

This app is not affiliated with the OpenSpec project.

## What it does

An OpenSpec project keeps its specifications in `openspec/` at a repository
root: the current specs under `specs/`, the work in progress under `changes/`,
and everything finished under `changes/archive/`. This app clones the repository
over HTTPS and lets you read all of it on a phone.

- **Repositories.** Add one by its HTTPS clone URL, by scanning a QR code, or by
  sharing a link into the app. Each row shows how many specs the project has,
  how many changes are active and archived, and how far the open tasks have got.
- **Project.** Four tabs: an overview, the changes, the specs, and the project's
  own configuration with the workflow schemas its changes use.
- **Changes.** One list, active and archived, searchable. Typing matches change
  names loosely; a leading `:` searches the text inside each change and says
  which files matched.
- **A change.** A tab per artifact file it actually has, rendered as Markdown,
  plus its task list with a box per task beside the progress.
- **Spec deltas.** Every requirement a change adds, modifies, removes or
  renames. For a change not yet archived, a modified requirement can be compared
  with the one it modifies: the difference, the original and the proposed.
- **A spec.** Its purpose, its requirements and their scenarios, each with a
  card. A file that does not fit the OpenSpec grammar still opens, lists every
  reason why with its line, and stays readable as Markdown.

Phase 1 reads. It does not edit, tick tasks, archive, discard, export, commit or
push. See the roadmap below.

## Installing it

The debug APK installs on any phone running Android 8.0 (API 26) or newer.

```bash
nix develop --command ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Without `adb`, copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and
open it; Android will ask you to allow installing from that source.

The debug build is unminified and carries the debug signing key, so it is about
36 MB. A release build is smaller, and is what F-Droid would produce.

## Adding a repository and a token

A public repository needs only its HTTPS clone URL:

```
https://github.com/speclib/specgetty
```

You do not have to get it exactly right. A page URL copied from a browser is
turned into one that clones: the query string and the fragment are dropped, and
a path that continues into a view of the repository, such as `/issues/12` or
`/tree/main/src`, is truncated back to the repository itself. The result lands
in the form, editable, and nothing is cloned until you press **Add**.

Three ways in, all ending at the same form:

- **Type or paste** the URL.
- **Scan a QR code** with the camera. Camera access is asked for the first time
  you scan, and refusing it leaves everything else working.
- **Share a link** into the app from a browser or a forge app.

SSH URLs are refused with a message saying so. Phase 1 speaks HTTPS only.

## A repository holding several projects

One repository can hold more than one OpenSpec project, one per directory. When
you add such a repository, the app lists the projects it found and you choose
which to add. Each chosen project becomes its own row, with its own specs,
changes and task counts.

A repository holding one project is added straight away, with nothing to choose.

Those rows share one downloaded copy and one credential, because they are one
repository:

- Refreshing fetches the repository once, however many of its rows are in the
  list, and reloads all of them.
- Removing a row frees nothing while another row from the same repository
  remains. The downloaded copy and the credential go with the last one, and the
  confirmation says which of the two is about to happen.

A project is found where an `openspec/` directory holds `config.yaml`,
`config.yml` or `project.md`. A directory named `openspec` holding none of those
is not offered, so a repository that keeps unrelated files under that name
contributes nothing.

Which projects a repository holds is settled when you add it. A project added to
the remote later appears by adding the repository again; the rows you already
have are left alone.

## A repository that points at a store

A repository whose `openspec/config.yaml` reads `store: <id>` and keeps no specs
or changes of its own holds no content. It names content kept somewhere else,
resolved through a registry of local paths on the machine that wrote it. A phone
has neither that registry nor those paths, so the app cannot follow it.

It says so, naming the id and the file that declared it, rather than claiming
there is no project. Add the repository that holds the content instead.

## A private repository

There are two ways, and the form offers both.

### Authorize with GitHub

For a repository on github.com. Tap **Or authorize with GitHub instead**. The
app shows a short code, you open github.com in a browser, type the code, and
approve.

GitHub then asks a second question: **which repositories** this app may see.
Approving without choosing any leaves the app able to read nothing, so choose
the repository you are adding. The app reads the answer back and tells you what
it was granted, rather than reporting success and failing at the clone.

Choosing "All repositories" covers that account only. A repository owned by an
organization needs the app installed on the organization as well, which an owner
there may have to approve. When the app is missing one, it says so and offers
the page where you add it.

Nothing is added until you press **Add**. Back out at any point and nothing is
stored.

The credential expires after about eight hours and renews itself before the next
read, without asking again. It is read-only: it cannot change anything in the
repository.

### Type an access token

For any host, github.com included. Create a personal access token with read
access and type it into the form.

Type it yourself: it is never taken from a scanned code or a shared link,
because a token on a photographable surface is a token you have given away.

### Either way

The credential is encrypted with a key held in the Android Keystore, kept apart
from the list of repositories, and deleted with the repository.

## Building

```bash
nix develop                     # JDK 17, the Android SDK, gradle
./gradlew assembleDebug
```

Without nix you need a JDK 17 and an Android SDK with build tools 37.0.0 and
platforms 26 and 37, with `ANDROID_SDK_ROOT` pointing at it.

## The gate

```bash
scripts/gate.sh
```

`nix flake check` covers what a sandboxed derivation can do: shellcheck over
`scripts/`, and `nixpkgs-fmt` over `flake.nix`. Gradle needs network access to
resolve dependencies, which a pure derivation does not get, so the build, the
unit tests, Android lint and the coverage verification run inside `nix develop`
from `scripts/gate.sh`.

The coverage floor is 70 percent over the bundle and 80 percent on the parser,
index and capture packages. `scripts/ship-change.sh` runs the gate before it
archives anything, so a change that fails it is never half-shipped.

The instrumented tests are not in the gate, because they need an emulator and a
gate that runs on every change should not. Run them before a release:

```bash
nix develop .#emulator --command ./scripts/e2e.sh
```

That starts an API 26 emulator if one of ours is not already up, runs the suite
against it, and stops only an emulator it started.

## How it is built

The grammar is not invented here. specgetty's `openspec/specs/` defines it,
`src/ui/specparse.go` implements it, and thirteen fixtures in
`src/ui/testdata/specs/` pin it, each taken from a live spec rather than made
up. Those fixtures are copied into this project's tests, and so is specgetty's
whole `openspec/` tree: 28 specs and 57 archived changes holding 111 delta
files. The parsers structure all of them.

Where this app and specgetty could be read as disagreeing, specgetty decides.

## Roadmap

Phase 2 is editing: ticking a task writes `tasks.md` back and commits and pushes
it through JGit. `RepoStore` grows write, commit and push, and the task parser
grows a renderer that rebuilds the file as it is on disk, which is what
specgetty's `task-checkboxes` requires.

Not planned: SSH authentication, the GitHub API, background sync, or scanning
for projects anywhere but `openspec/` at a repository root.

## Licence

MIT. See `LICENSE`.
