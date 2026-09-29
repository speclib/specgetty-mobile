## Context

See `proposal.md` for motivation. What follows is what a spike established
against a real GitHub App, and what that constrains.

Today a credential is a string. `RepoRegistry` records whether one exists,
`KeystoreTokenVault` encrypts it, and `RepoStore` sends it as the HTTPS username
with an empty password. Nothing about it can expire or change.

The spike registered a GitHub App, ran the device flow, and cloned a private
repository. Five results, each of which changes a decision below:

| Measured | Result |
|---|---|
| `ghu_` token authenticates `git clone` over HTTPS | yes, with the token as username and an empty password |
| Refresh needs a `client_secret` | **no**, `client_id` and the refresh token suffice |
| Token lifetime | `expires_in: 28800`, refresh token present |
| Permissions actually granted | `{"contents": "read", "metadata": "read"}` |
| Authorizing without installing | valid token, zero reach, HTTP 403 on clone |

## Goals / Non-Goals

**Goals:**

- Obtain a credential without leaving a keyboard, and never grant more than
  reading.
- Leave `RepoStore`'s wire behaviour untouched, because the spike proved it is
  already correct.
- Make the device flow's own error vocabulary testable off a device.

**Non-Goals:**

- Other forges. The shape below is deliberately general where it is free to be
  and GitHub-specific where it is not, but nothing is built for GitLab or Gitea.
- Writing. `contents: read` is declared and Phase 2 widens it.
- Replacing the typed token. It stays, and it stays the only option for every
  host that is not `github.com`.

## Decisions

### A GitHub App, not an OAuth App

There is no read-only OAuth scope for private repositories. `repo` is the only
scope that reaches private code and it grants write over every repository the
person owns. A GitHub App declares `contents: read` and cannot exceed it.

The spike confirmed the grant is what it claims: `{"contents": "read",
"metadata": "read"}`.

One honest limit. The **repositories** an installation covers are the user's
choice, and GitHub offers "All repositories" first; the spike's install landed
there. So the app guarantees *read-only*, not *few repositories*. The half that
is guaranteed is the half that matters, since it is the half the app could
otherwise abuse.

Rejected: OAuth App with the device flow. Same effort, strictly more power, and
nothing to upgrade later because it starts at maximum.

### The device flow, because nothing else is available

The web flow requires a `client_secret` at the code exchange, and PKCE is
additive rather than a substitute. A FOSS app publishes its source; a secret in
it is not a secret. The `client_id` is public by design and safe to ship.

Rejected: shipping a secret obfuscated. It is not a mitigation, it is a delay.

Rejected: a redirect broker we host. It would keep the secret off the device at
the cost of a server this project does not have, cannot promise to keep running,
and which would see every authorization.

### The credential becomes a `TokenSource`, and only the vault notices

```
   before                          after

   String?                         sealed interface Credential
      │                              ├── Typed(token)
      ▼                              └── GitHub(token, expiresAt, refreshToken)
   RepoStore                                     │
                                                 ▼
                                   TokenSource.current(): String?
                                     refreshes when near expiry
                                                 │
                                                 ▼
                                            RepoStore   ← unchanged
```

`RepoStore` keeps taking a `String?`. Something above it decides what that string
is. The spike is what licenses this: the token already works in the shape
`RepoStore` sends, so the wire layer has no reason to learn about expiry.

Rejected: refreshing inside `RepoStore`. It would put an HTTP call to
`github.com` inside the class whose whole job is git, and make its unit tests
need a token server.

### Refresh before use, not retry after failure

The token lasts eight hours and a refresh needs no secret, so the cheap thing is
also the correct thing: check expiry before handing the string over, refresh if
it is close, and only then clone.

Retry-on-401 is the alternative and it is worse here, because a 403 from this
API means two different things (see below) and a retry loop would have to tell
them apart from the outside.

Expiry could be disabled in the app's settings, which would remove this section
entirely. It is deliberately left on: a read-only token that expires is a
smaller thing to lose than one that does not, the refresh costs about forty
lines, and the spike proved it works without a secret.

### A fourth error state, because 403 currently lies

The spike's most useful result. With a valid token and no installation:

```
remote: Write access to repository not granted.
fatal: ... The requested URL returned error: 403
```

"Write access", on a clone. And `403` is in `RepoStore.AUTH_MARKERS`, so
`classify()` returns `RepoError.Authentication` and the screen says "Check the
access token" about a token that is perfect.

So `RepoError` grows `NoAccessToRepository`, and the message points at the
installation rather than the credential. Distinguishing them is not guesswork:
the app knows whether the credential came from an authorization, and can ask
`/user/installations` which repositories the installation covers.

### Authorizing and installing are two steps, and the screen must say so

The spike hit this twice. The device flow completes, GitHub says yes, the token
is valid, and it can reach nothing. Silence is the worst possible response, so
the authorization screen checks `/user/installations` on success and, when the
answer is empty or does not cover the repository being added, sends the person
back to install rather than letting them discover it at clone time.

### The `client_id` is published, and the app is named

The `client_id` goes in the source. A fork that does not change it authorizes
against this project's app, which is why the app is registered with a name and a
description that say what it is, and why it asks for `contents: read` and
nothing else. Anyone uneasy about that can register their own and change one
constant.

## Risks / Trade-offs

- **The app becomes GitHub-shaped.** → Everything above the transport stays
  forge-neutral: a `Credential` that may refresh is not a GitHub idea. Only
  `auth/github/` knows about device codes. A second forge adds a sibling, not a
  rewrite.
- **A token the app cannot refresh.** A refresh token lasts six months; leave the
  app alone for longer and it is dead. → The failure is already a first-class
  state, and re-authorizing is the same two taps as authorizing.
- **`/user/installations` is a GitHub API call, which `BRIEFING.md`'s non-goals
  disclaim.** → Argued in the proposal and accepted: it is authentication, not
  data. Recorded in `docs/fdroid.md` rather than left for a reader to find.
- **The install step loses people.** Two web trips is worse than one, and the
  point of the feature was fewer steps. → Mitigated by checking rather than
  hoping, but not removed. If it proves annoying in use, the fallback is not a
  redesign: it is the typed token, which never goes away.
- **A stored refresh token is a second secret.** → It goes in the same
  Keystore-backed vault as the access token, under the same key, and is deleted
  with the repository.
- **The spike's app was installed on "All repositories".** → The screen should
  name what the installation covers when it reports success, so the person can
  see what they granted rather than assume.

## Migration Plan

Nothing to migrate. A typed token is `Credential.Typed`, which has no expiry and
no refresh token, so existing repositories keep working untouched and the vault's
stored form is read as `Typed` when it carries no expiry.

Rollback is removing the action from the add form. Credentials already obtained
keep working until they expire, and the typed path is unaffected.

## Open Questions

- Whether GitHub returns `verification_uri_complete` for GitHub Apps. If it does,
  the browser can open on a page with the code already filled in, and the person
  types nothing at all. This changes the authorization screen's copy and nothing
  else, so it can be answered when that screen is built.
- Whether to offer "install on selected repositories" guidance in the screen's
  copy, given GitHub offers "All repositories" first. A wording question, not a
  structural one.
