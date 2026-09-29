## 1. The GitHub App

- [ ] 1.1 Delete the `specgetty-spike` app used for the spike, and verify
      authorizing against its client id now fails
- [ ] 1.2 Register the real GitHub App: repository permission `Contents:
      Read-only` and nothing else, webhook off, device flow enabled, a
      description saying what the app is. Verify by reading back
      `/user/installations` for a test installation and seeing
      `{"contents": "read", "metadata": "read"}` and nothing more
- [ ] 1.3 Put the client id in the source as a named constant with a comment
      saying it is public by design, and verify a release build carries no
      other credential by grepping the built APK for the word `secret`

## 2. The device flow, off a device

- [ ] 2.1 A transport interface for the two calls, so the flow can be driven by
      a fake. Verify by unit-testing the flow with no network in the test
- [ ] 2.2 Request a device code and parse the response. Verify a unit test reads
      the user code, the verification address, the expiry and the interval from
      a recorded response
- [ ] 2.3 Poll for the credential. Verify a unit test where the fake answers
      `authorization_pending` three times and then succeeds
- [ ] 2.4 Honour `slow_down` by lengthening the interval. Verify a unit test
      asserts the next request came later than the one before
- [ ] 2.5 Treat `expired_token`, `access_denied` and `device_flow_disabled` as
      distinct endings. Verify a unit test asserts a different outcome for each
- [ ] 2.6 Stop when the caller abandons it. Verify a unit test cancels mid-poll
      and asserts no further requests were made

## 3. The credential, and renewing it

- [ ] 3.1 A credential type that is either a typed token or one with an expiry
      and renewal material. Verify unit tests that a typed token has neither
- [ ] 3.2 Store both kinds in the existing vault. Verify a unit test round-trips
      each kind and asserts the stored form reveals neither
- [ ] 3.3 Read a credential stored before this change as a typed token. Verify a
      unit test reads the old stored form and gets a working typed credential
- [ ] 3.4 Renew before use when close to expiry, using only the client id.
      Verify a unit test with a fake clock renews an almost-expired credential
      and leaves a fresh one alone
- [ ] 3.5 Report a renewal that is refused as needing authorization again.
      Verify a unit test asserts that outcome rather than an authentication
      failure
- [ ] 3.6 `RepoStore` still receives a plain string. Verify its existing tests
      pass untouched

## 4. Telling the failures apart

- [ ] 4.1 Add the no-access-to-this-repository failure to `RepoError`. Verify a
      unit test asserts it is not the authentication failure
- [ ] 4.2 Classify the host's refusal as that rather than as authentication,
      including when its message mentions write access. Verify a unit test
      using the exact text the spike recorded: `Write access to repository not
      granted` with a 403
- [ ] 4.3 Keep a genuinely rejected credential classified as authentication.
      Verify the existing authentication tests still pass, against the local
      HTTP server that already refuses a wrong token

## 5. The authorization screen

- [ ] 5.1 Show the code, the address, and that the app is waiting. Verify an
      instrumented test asserts all three are on screen
- [ ] 5.2 Copy the code, and open the address outside the app. Verify an
      instrumented test asserts the copy action puts the code on the clipboard
- [ ] 5.3 Keep waiting across leaving the app and returning. Verify an
      instrumented test backgrounds and resumes the screen and asserts the flow
      completes
- [ ] 5.4 Say what happened for each ending from task 2.5, and offer a fresh
      code after an expiry. Verify an instrumented test per ending
- [ ] 5.5 Abandoning stores nothing. Verify an instrumented test asserts the
      vault is untouched afterwards

## 6. Authorizing is not installing

- [ ] 6.1 After approval, read which repositories the credential reaches.
      Verify a unit test against a recorded response listing two repositories
- [ ] 6.2 When it reaches nothing, say so and say how to fix it rather than
      reporting success. Verify an instrumented test with a fake reaching
      nothing asserts the screen does not claim success
- [ ] 6.3 When it does not cover the repository being added, say so before
      cloning. Verify a unit test asserts no clone was attempted
- [ ] 6.4 Name what was granted on success. Verify an instrumented test asserts
      the repository names are shown

## 7. The add form

- [ ] 7.1 Offer authorizing when the URL is one the app can authorize for, and
      not otherwise. Verify view model tests for a github.com URL, a GitLab URL
      and an unrecognised host
- [ ] 7.2 Show that a credential is held without showing it, and add nothing
      until confirmed. Verify view model tests assert the credential is not in
      any visible field and that nothing was added
- [ ] 7.3 Keep the typed token path exactly as it is. Verify the existing add
      form tests pass untouched

## 8. Documentation

- [ ] 8.1 Rewrite the claim in `docs/fdroid.md` that the app speaks only git to
      one host, naming the endpoints it now uses and why. Verify by reading it
      against the app's actual network calls
- [ ] 8.2 Record in `docs/fdroid.md` that the client id is public, what the app
      asks for, and how to substitute your own. Verify the stated permission
      matches what task 1.2 registered
- [ ] 8.3 Say in the README how to add a private repository both ways, and that
      authorizing needs the app installed on the repositories. Verify by
      following it on a phone without prior knowledge
- [ ] 8.4 Note in `BRIEFING.md` that the non-goal about the GitHub API is
      amended for authentication only, data staying on git. Verify the wording
      says which part is amended and which is not

## 9. End to end

- [ ] 9.1 Add a private repository on a real device by authorizing, and read it.
      Verify by doing it on the Fairphone, which is the only place the browser
      round trip is real
- [ ] 9.2 Let a credential expire and use the repository again. Verify by
      refreshing after the expiry has passed and asserting no interruption
- [ ] 9.3 Remove an authorized repository and confirm nothing is left. Verify an
      instrumented test asserts the vault holds neither credential nor renewal
      material
- [ ] 9.4 `scripts/gate.sh` passes, and coverage of the new authorization code
      is at or above the floor its package carries
