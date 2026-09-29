## Why

Bean `specgetty-mobile-5ztn`.

Adding a private repository currently means leaving the app, navigating GitHub's
settings on a phone, creating a personal access token, and pasting 93 characters
back. It is the worst moment in the app after the URL, and the URL already got
three ways in: typed, scanned, shared.

The bean asks for the obvious remedy: authorize on GitHub's own site and let
GitHub hand the app a credential.

Two facts decide the shape, and both were measured rather than assumed. A spike
registered a real GitHub App, ran the device flow, and cloned a private
repository with the resulting token.

**Only the device flow is possible.** GitHub's web application flow requires a
`client_secret` to exchange the code, and PKCE is additive rather than a
replacement for it. A secret cannot live in a FOSS app whose source is public.
The device flow needs only a `client_id`, which is public by design.

**A GitHub App grants less than an OAuth App.** There is no read-only OAuth
scope for private repositories: `repo` is the only scope that reaches private
code and it carries **write to every repository the person owns**. A GitHub App
declares `contents: read` and cannot exceed it, whatever the user clicks. For an
app that cannot write a byte, taking write over everything to save a paste is a
bad trade, and it is the trade the obvious implementation makes.

So: a GitHub App, authorized by the device flow. Phase 2 widens it to write
through GitHub's own consent prompt, which is the honest sequence.

## What Changes

- A new way to obtain a credential, beside typing one. The add form gains an
  "Authorize with GitHub" action for `github.com` URLs.
- `auth/DeviceFlow`: request a device code, show the user code, open the browser,
  and poll for the token. Pure over an injected transport, so its error
  vocabulary is unit tested rather than discovered on a device.
- `auth/TokenSource`: a stored credential grows from a string into something that
  may carry an expiry and a refresh token, and that refreshes itself before use.
  A typed token is the case with neither.
- A fourth repository error state: **the credential is good but this repository
  is not in the installation**. Today that returns HTTP 403, `classify()` reads
  `403` as an authentication failure, and the app tells the reader to check a
  token that is perfectly fine.
- The two-step nature of GitHub Apps is made explicit: authorizing yields a token
  that can reach nothing until the app is also installed on some repositories.

Not in this change: GitLab, Gitea and Codeberg. They have their own flows and
deserve their own bean. The typed token stays, and stays first for every other
forge.

## Capabilities

### New Capabilities

- `github-authorization`: obtaining a GitHub credential by authorizing on
  github.com rather than by typing one, and keeping that credential usable.

### Modified Capabilities

- `repo-registry`: a stored credential is no longer only an opaque string; it may
  carry an expiry and refresh itself. What the repository list records about it
  changes with that.
- `repo-store`: a new failure is distinguishable from an authentication failure,
  namely a valid credential with no grant on the repository asked for.
- `repo-list-screen`: the add form offers authorization as well as typing, for a
  host where it is available.

## Impact

- **No new dependencies.** JGit already brings a JDK HTTP client and
  `kotlinx-serialization-json` is already here. Verified against the release
  classpath.
- **No new permissions.** Opening the browser is `ACTION_VIEW`.
- **`docs/fdroid.md` changes.** The app gains a `client_id` in its source, which
  is public by design, and starts talking to `github.com` endpoints that are not
  git. The audit claims plain git is the only thing the app speaks; that claim
  stops being true and the document has to say so.
- **`BRIEFING.md` tension, and it is deliberate.** The non-goals say "No GitHub
  API usage. Plain git is the data layer." The data layer is untouched: this is
  authentication, and not one byte of repository content comes from the API. But
  it is the first GitHub-shaped code in an app whose URL layer deliberately
  treats every forge alike, and that asymmetry is real. Recorded here rather than
  discovered later.
- **`RepoStore` needs no change to authenticate.** The spike cloned a private
  repository with the token as the HTTPS username and an empty password, which is
  exactly what `RepoStore` already sends.
