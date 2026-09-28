# Specgetty on Droid

An Android app for browsing and reading [OpenSpec](https://github.com/Fission-AI/OpenSpec)
projects in git repositories. It is the mobile counterpart of
[specgetty](https://github.com/speclib/specgetty), the OpenSpec TUI, and borrows
its git layer from [beans-on-droid](https://github.com/mipmip/beans-on-droid).

This app is not affiliated with the OpenSpec project.

## Status

Phase 1, read-only, under construction. `BRIEFING.md` is the source of truth for
what Phase 1 is. Nothing is released yet.

## What it does

Add a repository by HTTPS URL, by scanning a QR code, or by sharing a link into
the app. The repository is shallow cloned into app-private storage and the
OpenSpec project at `openspec/` in its root is loaded into an in-memory index.
From there you can browse changes, specs, task lists and spec deltas, and
compare an active change's delta against the spec it modifies.

Phase 1 does not edit anything. No ticking tasks, no archiving, no committing
and no pushing. See the Phase 2 roadmap in `BRIEFING.md`.

## Building

```bash
nix develop            # JDK, Android SDK, gradle
./gradlew assembleDebug
```

Without nix you need a JDK 17 and an Android SDK with build tools 37.0.0 and
platforms 26 and 36 installed, and `ANDROID_SDK_ROOT` pointing at it.

## The gate

```bash
scripts/gate.sh
```

`nix flake check` covers what a sandboxed derivation can do. Gradle needs
network access for dependency resolution, so the build, test, lint and coverage
half runs inside `nix develop`. The coverage floor is 70 percent overall and 80
percent on the parser and index packages.

## Adding a repository and a token

Public repositories need only their HTTPS clone URL. A private one also needs a
personal access token with read access to the repository. The token is stored
encrypted with an Android Keystore key, and it is never read from a scanned code
or a shared link: those fill the URL field only, and you type the token
yourself.

## Licence

MIT. See `LICENSE`.
