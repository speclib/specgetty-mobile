## Why

Beans `specgetty-mobile-e5xj` and `specgetty-mobile-vo5e`, milestone
`specgetty-mobile-k5kk`.

Nothing else in `BRIEFING.md` can be built or proven until an Android module
assembles and a gate runs over it. The two beans are one change because each is
useless alone: a coverage gate with no build measures nothing, and a build with
no gate means the first ship of this project is the one ship that was never
checked.

The fixed settings in `BRIEFING.md` are permanent once published, the
application ID especially. They are encoded here rather than left to a later
change to get right.

## What Changes

- A Gradle build with a single `:app` module, its versions in
  `gradle/libs.versions.toml`, and the wrapper committed.
- A single-activity Compose app with Material 3 and Navigation Compose, showing
  a placeholder destination. No feature behaviour: that is milestone 02 onward.
- Core library desugaring with `desugar_jdk_libs_nio`, which JGit needs on API
  26 and which cannot be retrofitted cheaply later.
- A JaCoCo report and a `jacocoCoverageVerification` task carrying the floors
  from `BRIEFING.md`: 70 percent overall, 80 percent on the parser and index
  packages.
- `scripts/gate.sh` gains its gradle half in earnest, and the packages the
  80 percent rule applies to are named in the build rather than in prose.

## Capabilities

### New Capabilities

- `build-and-gate`: what the project builds into, which settings are fixed, and
  what has to pass before a change can be shipped.

## Impact

- New: `settings.gradle.kts`, `build.gradle.kts`, `app/`, the Gradle wrapper.
- The 80 percent rule names packages that do not exist yet. A JaCoCo `PACKAGE`
  rule with an empty `includes` applies to every package, not to none, so
  leaving the list out would impose 80 percent everywhere. It is seeded with the
  package names the briefing calls the parser and index packages. JaCoCo has
  nothing to match until those packages exist, and the rule starts biting the
  moment they do.
- No dependency here is one the briefing does not already require.
