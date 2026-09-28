## 1. JGit on Android

- [x] 1.1 `AndroidGit.install` sets a `SystemReader` pointing at an app-private
      directory
- [x] 1.2 Installing twice is a no-op
- [x] 1.3 A fixed hostname rather than a lookup
- [x] 1.4 JGit dependency pinned at 6.4.0 in the version catalog

## 2. Errors as values

- [x] 2.1 `RepoError` with authentication, network, not an OpenSpec project and
      unknown
- [x] 2.2 `RepoResult` with success and failure, and accessors for each
- [x] 2.3 Classify a JGit exception by walking its cause chain

## 3. RepoStore

- [x] 3.1 Clone at depth 1 into a directory named by the repository id
- [x] 3.2 Remove an existing working directory before cloning over it
- [x] 3.3 Remove a partial working directory when a clone fails
- [x] 3.4 Refresh by fetch plus hard reset to the remote branch
- [x] 3.5 Refuse to refresh something that was never cloned
- [x] 3.6 Delete, reporting whether anything was removed
- [x] 3.7 Present a token as the HTTPS username, and nothing when it is blank

## 4. OpenSpecLayout

- [x] 4.1 Resolve `openspec/` at the repository root only
- [x] 4.2 Report no OpenSpec project when it is absent
- [x] 4.3 Distinguish an empty project from an absent one
- [x] 4.4 Derive `specs/`, `changes/` and `changes/archive/`
- [x] 4.5 Accept `config.yaml` or `config.yml`, and neither
- [x] 4.6 Resolve `project.md` when present

## 5. Proof

- [x] 5.1 A local remote helper that builds a real git repository with JGit
- [x] 5.2 Clone, shallowness, isolation, refresh, deletion and failure tests
- [x] 5.3 Layout tests over a temporary directory tree
- [x] 5.4 The gate passes
