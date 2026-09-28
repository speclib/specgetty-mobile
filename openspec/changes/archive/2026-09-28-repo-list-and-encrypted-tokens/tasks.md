## 1. Values

- [x] 1.1 `RepoConfig` with id, URL, label and whether a token exists
- [x] 1.2 `repoIdFor` deriving a stable, file-name-safe id from a URL
- [x] 1.3 `labelFor` taking the last URL segment without `.git`
- [x] 1.4 `RepoList` with add, remove and activate as pure transformations
- [x] 1.5 Adding an existing URL updates rather than duplicates
- [x] 1.6 Removing the active repository picks another

## 2. URL validation

- [x] 2.1 Reject empty, SSH, non-web and host-less URLs, each with its own message
- [x] 2.2 Accept an HTTPS URL with a host

## 3. Tokens

- [x] 3.1 `TokenVault` interface: put, get, remove
- [x] 3.2 `KeystoreTokenVault` using AES-GCM under an Android Keystore key
- [x] 3.3 A random IV per encryption, packed with the ciphertext
- [x] 3.4 Tokens in their own store, never in the repository list

## 4. Registry

- [x] 4.1 `RepoCatalog` interface over the list and the vault
- [x] 4.2 `RepoRegistry` backed by DataStore, taking the store by injection
- [x] 4.3 A blank token stores nothing and records no token
- [x] 4.4 Removing a repository removes its token
- [x] 4.5 Undecodable stored data yields an empty list
- [x] 4.6 Unknown fields in stored data are ignored

## 5. Proof

- [x] 5.1 Unit tests for the id, the label and every `RepoList` transformation
- [x] 5.2 Unit tests for every validation message
- [x] 5.3 Registry tests over an in-memory store with a fake vault
- [x] 5.4 The gate passes
