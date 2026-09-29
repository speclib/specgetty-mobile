# F-Droid readiness

`BRIEFING.md` sets hard requirements for inclusion in F-Droid. This is the audit
against them, with the one deviation stated plainly.

## Dependencies are FOSS

Every group on the release runtime classpath. The licence column was read from
the POM each artifact publishes, not from memory:

| Group | Licence as published |
|---|---|
| `androidx.*` (activity, annotation, appcompat, arch.core, autofill, collection, compose.*, concurrent, core, cursoradapter, customview, drawerlayout, emoji2, exifinterface, fragment, graphics, interpolator, lifecycle, loader, media3, navigation, navigationevent, profileinstaller, savedstate, startup, tracing, vectordrawable, versionedparcelable, viewpager, window) | Apache-2.0 |
| `androidx.camera`, `androidx.camera.viewfinder`, `androidx.camera.featurecombinationquery` | Apache-2.0, with BSD-3-Clause on part of `camera` |
| `androidx.compose.material3.adaptive` | Apache-2.0 |
| `androidx.datastore` | Apache-2.0, with BSD-3-Clause on part |
| `org.jetbrains`, `org.jetbrains.kotlin`, `org.jetbrains.kotlinx` | Apache-2.0 |
| `org.eclipse.jgit` | Eclipse Distribution License 1.0 (BSD-3-Clause) |
| `io.noties.markwon` | Apache-2.0 |
| `com.atlassian.commonmark` | BSD-2-Clause |
| `org.yaml` (SnakeYAML) | Apache-2.0 |
| `io.github.java-diff-utils` | Apache-2.0 |
| `com.google.zxing` | Apache-2.0 |
| `com.google.guava`, `com.google.dagger`, `com.google.auto.value`, `com.google.j2objc`, `com.google.code.findbugs` | Apache-2.0 |
| `com.googlecode.javaewah` | Apache-2.0 |
| `com.squareup.okio` | Apache-2.0 |
| `org.slf4j` | MIT |
| `org.checkerframework` | MIT |
| `org.jspecify` | Apache-2.0 |
| `jakarta.inject` | Apache-2.0 |
| `javax.inject` | its POM carries no licence block; JSR-330 is published under Apache-2.0 |

All OSI-approved, and all compatible with distributing an MIT app.

`com.google.guava:listenablefuture:1.0` is the empty placeholder artifact that
exists only to resolve a version conflict; it contains no code.

Verified absent from the release classpath: Google Play Services, Firebase,
Crashlytics, any analytics SDK, and any ad SDK.

```
nix develop --command ./gradlew -q :app:dependencies \
  --configuration releaseRuntimeClasspath \
  | grep -icE "play-services|firebase|crashlytics|gms|analytics|admob"
0
```

## Permissions

The app declares two.

| Permission | Why | What refusing it costs |
|---|---|---|
| `INTERNET` | Cloning and refreshing a repository over HTTPS. Without it the app has nothing to read. | Not refusable; it is install-time. |
| `CAMERA` | Reading a QR code that holds a repository URL. | Scanning. Typing and pasting a URL, and sharing one in, all still work. |

What keeps the camera permission proportionate:

- It is requested when scanning is first chosen, not at launch.
- `uses-feature android:name="android.hardware.camera" android:required="false"`,
  so the app installs on a device without a camera.
- The camera runs only while the scanner is on screen and is released when it
  leaves.
- Refusing leaves every other part of the app working.

No location, no contacts, no storage, no account permissions.

## Intents

One intent filter beyond the launcher: `ACTION_SEND` with `text/plain`, so a URL
can be shared in from a browser or a forge app.

`ACTION_VIEW` for `https` is deliberately **not** claimed. It would put this app
in the chooser for every link tapped on the phone, which would be hostile.

## What the app talks to

There is no telemetry endpoint, no update check and no crash reporter, and no
analytics of any kind.

Reading a project speaks git and nothing else, to one host: the one in the
repository URL a person typed. Not one byte of a spec, a change or a task list
comes from an API.

Authorizing a private GitHub repository is the one exception, and it is opt in.
A person who types an access token, or who reads only public repositories, never
reaches any of it. When the button is used, the app calls exactly four
addresses:

| Address | When | Why |
|---|---|---|
| `https://github.com/login/device/code` | The button is tapped | Asks for the short code the person types on github.com |
| `https://github.com/login/oauth/access_token` | Every few seconds while waiting, and again when a credential is close to expiring | Collects the credential once it is approved, and renews it |
| `https://api.github.com/user/installations` | Once, straight after approval | Reads which repositories the credential reaches, so the app can say what was granted instead of guessing |
| `https://api.github.com/user/installations/{id}/repositories` | Once, straight after the call above | Names those repositories |

The cloning that follows is plain git over HTTPS, the same call an unauthorized
clone makes, with the credential as the password.

`BRIEFING.md` records this as an amendment to its own non-goal: the GitHub API
is used for authentication, and for nothing else.

## The GitHub App, and its client id

`GITHUB_CLIENT_ID` in `auth/DeviceFlow.kt` is a public identifier, committed on
purpose.

It has to be. GitHub's web flow needs a client secret to exchange a code, and
PKCE is not offered as a replacement, so a FOSS app that ships its source cannot
use that flow at all. The device flow needs only the client id, which GitHub
documents as public.

What the app asks for:

| Permission | Level |
|---|---|
| Repository contents | Read-only |
| Repository metadata | Read-only |

Nothing else. No write anywhere, no organization permissions, no account
permissions, no webhook. A credential this app holds cannot change a repository.

Approving on github.com is only half of it: GitHub separately asks which
repositories the app may see, and a person may choose none. The app reads the
answer back and says so, rather than reporting success and failing at the clone.

To build with your own app instead: register a GitHub App with those two
permissions, no webhook, device flow enabled, and replace that one constant. No
other change is needed, and no secret is introduced by doing so.

## What trusting this app means

The app is registered as a public GitHub App, which it has to be. A private one
installs only on the account that owns it, so every user other than its owner
would be unable to install it at all. Public means anyone may install it on
their own account or on an organization they can install to. Nobody who installs
it gains anything over the app's registration: its settings, its ownership and
its keys stay where they are.

A credential the app holds is issued to one person, expires in about eight
hours, and reaches only the repositories that person chose. It can read those
and nothing else, and it can write nothing at all.

What it cannot do is remove the app's owner from the picture, and pretending
otherwise would be dishonest. Every GitHub App has an owner, and an owner may
generate a private key for it. A private key mints installation tokens without
the installing person present, and those tokens reach every repository the app
is installed on. That is how GitHub Apps work, and no amount of care on the
client can design it away.

What can be said, and checked:

- No private key has been generated for this app. An owner can see this on the
  app's settings page, where the key list is empty.
- Generating one would be a deliberate act, not a side effect of a release.
- Nothing in this repository could use a private key if one existed. There is no
  server, and the app makes the four calls named above and no others.
- A person who would rather not extend that trust has two ways out that need no
  permission from anyone: type a personal access token instead, which involves
  no app at all, or register their own GitHub App and rebuild with its client
  id, as described above.

Installing on an organization needs an owner of that organization to approve it,
which is the organization's decision rather than this app's.

A release build carries no other credential. Checking it:

```bash
APK=app/build/outputs/apk/release/app-release-unsigned.apk
for d in $(unzip -Z1 "$APK" | grep '^classes.*\.dex$'); do
  unzip -p "$APK" "$d" | strings -n 4 | grep -i secret
done
```

Every hit is a Java cryptography API name: `SecretKey`, `SecretKeySpec`,
`SecretKeyFactory`, `KeyStore$SecretKeyEntry`. Those come from the token vault
and from JGit's commit signing, and none of them is a stored credential. The
client id itself appears exactly once.

## Binaries in the repository

The briefing says no prebuilt binaries or jars. There is exactly one:

```
gradle/wrapper/gradle-wrapper.jar   sha256 7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172
```

**This is a deliberate deviation.** The Gradle wrapper jar is the standard way
to pin a build's Gradle version, F-Droid's build server handles wrapper jars as
a matter of course, and the alternative is an unpinned Gradle whose version
drifts under the build.

What makes it auditable:

- It was generated by `gradle wrapper --gradle-version 9.8.0` in an empty
  directory rather than copied from anywhere, and its checksum recorded above.
- `gradle-wrapper.properties` pins `distributionSha256Sum`, so the Gradle
  distribution the wrapper downloads is verified rather than trusted.
- A change to the jar shows up in review as a changed checksum rather than as an
  opaque binary diff.

No dependency jars, no `.so` files and no prebuilt AARs are committed.

## Signing

The APK published on the GitHub releases page is signed with this project's own
key. F-Droid signs with its own key unless a build is reproducible, and this
build is not.

**Those two signatures cannot update each other.** Android refuses an install
whose signature differs from the one already on the device, so a user who
installed from a release and then wants the F-Droid build has to uninstall
first, which takes their repository list and their encrypted credentials with
it.

That break is accepted rather than solved. The alternative is asking F-Droid for
a reproducible build, which verifies its own build against ours and then ships
our signature. It is the right answer for an app with users, and it costs a
byte-reproducible gradle build, which is work this project has not done. At
`versionCode` 1 there is nobody to break, so the cost of the break is at its
lowest it will ever be, and the cost of solving it is at its highest relative to
what it buys. When this app has users and F-Droid inclusion is pursued, this
section is what says what they are about to be asked to do.

The debug APK kept by the Check workflow carries the Android SDK's universal
debug key, which is public. It is a build of a pull request to put on a phone,
not a way to install the app, and installing it hits the same refusal against a
release. The README does not offer it.

## The signing key

| | |
|---|---|
| Held in | `~/.android/keystores/specgetty-mobile-release.jks`, outside any checkout, mode 600 |
| Backed up to | a second medium, offline, kept apart from the first |
| Reaches a release through | four repository secrets, never a file in the tree |

The certificate a release carries, which is what a downloaded APK can be checked
against:

```
CN=Specgetty on Droid, O=speclib, C=NL
4096-bit RSA, SHA384withRSA, valid until 2054-02-14
SHA-256  88:39:7b:84:d9:e0:8a:6d:b1:eb:e3:39:8c:27:b9:dd:
         27:2b:f9:43:59:cc:49:bc:6f:15:43:a0:47:c7:50:a9
```

```bash
apksigner verify --print-certs specgetty-on-droid-<version>.apk
```

The four secrets are `SPECGETTY_KEYSTORE_BASE64`, `SPECGETTY_KEYSTORE_PASSWORD`,
`SPECGETTY_KEY_ALIAS` and `SPECGETTY_KEY_PASSWORD`. The Release workflow decodes
the keystore outside the checkout and deletes it when the job ends, whatever the
job did.

The build reads the same four values from the environment under the names
`SPECGETTY_KEYSTORE`, `SPECGETTY_KEYSTORE_PASSWORD`, `SPECGETTY_KEY_ALIAS` and
`SPECGETTY_KEY_PASSWORD`. When the environment holds no keystore the build
produces an unsigned APK rather than failing, which is what makes F-Droid's
build from source, and anyone else's, work without this project's secrets.

A keystore generated by a current `keytool` is PKCS12, where the key password
and the store password are the same value. Giving them different values produces
a keystore that opens and then fails at signing with "Given final block not
properly padded", which is a confusing way to learn this.

**Losing the keystore cannot be recovered from.** Nobody who installed from a
release could be updated again. The only remedy is a new application ID, and
`BRIEFING.md` says the application ID is permanent once published, so there is
no remedy. The backup is the whole mitigation.

Neither a keystore nor a signing password is in this repository:

```bash
git ls-files | grep -iE '\.(jks|keystore|p12)$'
git grep -iE 'storePassword *= *"|keyPassword *= *"'
```

Both find nothing. The build names the environment variables it reads; it holds
no value for any of them.

## Licence

MIT. `LICENSE` is at the repository root.

## Screenshots and the recording

The seven in `fastlane/metadata/android/en-US/images/phoneScreenshots/` and the
recording in `docs/images/` were taken by driving the app, not composed by hand.
Retake them with:

```bash
nix develop .#emulator --command ./scripts/screenshots.sh
nix develop .#emulator --command ./scripts/recording.sh
```

The project they show is written by the test that takes them. Somebody else's
specifications are not this project's to publish.

**Both are taken on API 26**, the same version the instrumented tests run on.
That is not the version they should be taken on. The tests use API 26 because it
is the minimum the app supports and where a regression shows up first, which is a
reason about correctness and says nothing about what the app should look like in
a picture, where 2017 system chrome is the wrong answer.

Moving them to API 36 is blocked by `specgetty-mobile-ityo`: the instrumented
suite does not run on API 36 at all. Every test that adds a repository times out,
including the one that only needs an error message to appear. `scripts/emulator.sh`
takes an API level, so this becomes one line in each script once that is fixed.

The recording is driven by a Compose test rather than by taps at fixed
coordinates, so that it cannot quietly start filming the wrong thing when a
layout moves. The test signals when the app has reached the spec, and the script
starts the camera then; without that the recording opens on a launcher and spends
most of its length on a form. It is encoded as a GIF because GitHub will not play
an mp4 from a relative path, and held to 800 KB so that the README is not the
thing a reader waits for.

## Not affiliated

The README and the F-Droid full description both state that this app is not
affiliated with the OpenSpec project. It reads OpenSpec projects; it is not one
of them.

## Re-running this audit

```bash
nix develop --command ./gradlew -q :app:dependencies \
  --configuration releaseRuntimeClasspath \
  | grep -icE "play-services|firebase|crashlytics|gms|analytics|admob"

git ls-files | grep -iE '\.(jar|so|aar|dex|apk)$'
sha256sum gradle/wrapper/gradle-wrapper.jar
```
