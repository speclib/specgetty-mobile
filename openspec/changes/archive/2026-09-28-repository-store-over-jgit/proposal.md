## Why

Bean `specgetty-mobile-cjzs`, milestone `specgetty-mobile-4cpl`.

The app has nothing to read until a repository is on the device. `BRIEFING.md`
puts plain git at the data layer, rules out the GitHub API and SSH, and borrows
this layer from beans-on-droid, whose README documents why JGit 6.4.0 is pinned.

Two things here are not obvious and are the reason this is its own change.

JGit assumes a desktop. It reads `~/.gitconfig`, asks for a hostname and expects
`java.nio.file`. On Android none of that holds, and the failure is a crash deep
inside a clone rather than a clear error. beans-on-droid solved it with a
`SystemReader` that points JGit at an app-private config directory, plus core
library desugaring, which milestone 01 already enabled.

An error has to say which kind of error it is. A bad token, an unreachable host
and a repository that simply has no `openspec/` directory are three different
things to the person holding the phone, and `BRIEFING.md` requires all three to
be readable states. JGit reports all of them as a `TransportException` or worse,
so classification belongs here and not in the UI.

## What Changes

- `repo/AndroidGit`: install a `SystemReader` that confines JGit to an
  app-private directory.
- `repo/RepoStore`: clone at depth 1, refresh by fetch plus hard reset to the
  remote branch, and delete. HTTPS only, with an optional token.
- `repo/RepoError` and `RepoResult`: authentication, network, not an OpenSpec
  project, and unknown, as values rather than exception types.
- `repo/OpenSpecLayout`: resolve `openspec/` at the repository root and the
  paths under it. One project per repo, no scanning, no store resolution.

## Capabilities

### New Capabilities

- `repo-store`: getting a repository onto the device and keeping it current,
  and saying precisely what went wrong when that fails.

## Impact

- New dependency: JGit 6.4.0, pinned. Bumping it is not a routine upgrade, and
  the reason lives in beans-on-droid's README.
- Tests use `file://` remotes created by JGit itself, so the whole layer is
  covered by plain JVM unit tests with no emulator and no network.
- `RepoStore` and `OpenSpecLayout` are ordinary classes and stay inside the 70
  percent floor. `AndroidGit` touches the JGit singleton and is excluded.
