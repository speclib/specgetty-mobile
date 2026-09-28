## 1. Gradle build

- [x] 1.1 `settings.gradle.kts` with `google()` and `mavenCentral()`, and the
      `:app` module
- [x] 1.2 Version catalog at `gradle/libs.versions.toml`
- [x] 1.3 Gradle wrapper committed, with its checksum
- [x] 1.4 `app/build.gradle.kts` with the fixed settings from `BRIEFING.md`
- [x] 1.5 Core library desugaring with `desugar_jdk_libs_nio`

## 2. App skeleton

- [x] 2.1 `AndroidManifest.xml` with the INTERNET permission and one activity
- [x] 2.2 Application class
- [x] 2.3 Material 3 theme
- [x] 2.4 Single activity hosting a Navigation Compose graph
- [x] 2.5 A placeholder destination, so the graph is real rather than notional

## 3. Coverage gate

- [x] 3.1 JaCoCo applied, with the tool version in the catalog
- [x] 3.2 `jacocoTestReport` over the debug unit tests
- [x] 3.3 `jacocoCoverageVerification` with a 70 percent bundle rule
- [x] 3.4 An 80 percent package rule naming the parser and index packages
- [x] 3.5 Exclusions for framework glue that a unit test cannot reach
- [x] 3.6 `scripts/gate.sh` runs assemble, test, lint and the verification

## 4. Proof

- [x] 4.1 A unit test that runs, so the gate measures something rather than
      dividing by zero
- [x] 4.2 `./gradlew assembleDebug` succeeds
- [x] 4.3 `./gradlew test` passes
- [x] 4.4 `./gradlew lint` has no errors
- [x] 4.5 `scripts/gate.sh` passes end to end
