## Why

Bean `specgetty-mobile-wvxc`, milestone `specgetty-mobile-4cpl`.

`RepoStore` clones whatever it is handed and forgets it. Something has to
remember which repositories exist across launches, which one is being looked at,
and the token that gets into a private one.

The token is the part that needs care. It grants read access to somebody's
source, and `BRIEFING.md` requires it encrypted with an Android Keystore key.
Keeping it in the same store as the repository list would mean one careless log
line or one backup rule leaks it, so the list and the tokens are separated: the
list records only whether a token exists, never the token.

The repository id also has to be decided here rather than left to chance. It
names the directory `RepoStore` clones into, so it has to be stable across
launches and safe as a file name. Deriving it from the URL gives both, and makes
adding the same repository twice an update rather than a duplicate.

## What Changes

- `store/RepoConfig`: a repository's id, URL and label, and a `RepoList` that
  knows how to add, remove and activate, as pure values.
- `store/RepoUrl`: validation that rejects SSH and anything that is not HTTP or
  HTTPS, with a message a person can act on.
- `store/TokenVault` and `store/KeystoreTokenVault`: tokens encrypted with
  AES-GCM under an Android Keystore key, kept in their own store.
- `store/RepoRegistry`: the list in DataStore, with the vault behind the same
  interface so callers never handle ciphertext.

## Capabilities

### New Capabilities

- `repo-registry`: which repositories the app knows about, which one is active,
  and how the token for a private one is kept.

## Impact

- New dependencies: DataStore preferences and kotlinx-serialization-json.
- Backup is already off in the manifest, so an encrypted token cannot leave the
  device through a cloud backup.
- `RepoList`, `repoIdFor`, `labelFor` and `RepoUrl` are pure and unit tested.
  `KeystoreTokenVault` needs the Android Keystore and is excluded from coverage;
  milestone 09 covers it with an instrumented test.
