## 1. State

- [x] 1.1 `ProjectState`: absent, loading, loaded, no project, failed
- [x] 1.2 The failure carries the `RepoError` that caused it
- [x] 1.3 No project here is its own state, not a failure

## 2. Repository

- [x] 2.1 Store, registry, loader and dispatcher taken by injection
- [x] 2.2 Add: register, clone, load, index
- [x] 2.3 Refresh: fetch and reset, then reload and reindex
- [x] 2.4 Remove: delete the working copy, the entry, the token and the state
- [x] 2.5 Switch: activate, loading from disk if this session has not
- [x] 2.6 The token stored at add time is used for later refreshes
- [x] 2.7 State exposed as a flow, one entry per repository

## 3. Parse cache

- [x] 3.1 A spec parsed on first open and kept
- [x] 3.2 Keyed by file and modification time, so a rewrite reparses
- [x] 3.3 Deltas cached the same way

## 4. Proof

- [x] 4.1 Tests over local git remotes, with no emulator
- [x] 4.2 A test that loading a project parses no spec
- [x] 4.3 A test that reopening a spec does not reparse it
- [x] 4.4 The gate passes
