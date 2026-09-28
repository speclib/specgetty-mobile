---
# specgetty-mobile-e5xj
title: Gradle and app skeleton
status: completed
openspec-link: openspec/changes/archive/2026-09-28-gradle-skeleton-and-coverage-gate
type: epic
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T20:06:37Z
parent: specgetty-mobile-k5kk
---

A single-activity Compose app that assembles, with the fixed settings from BRIEFING.md.

- [ ] Gradle wrapper, AGP, Kotlin, Compose BOM via gradle/libs.versions.toml
- [ ] applicationId io.github.mipmip.specgettyondroid, minSdk 26, versionCode 1, versionName 0.1.0
- [ ] Single activity, Navigation Compose, Material 3 theme
- [ ] desugar_jdk_libs_nio enabled, needed by JGit on API 26
- [ ] JUnit and Compose UI test wiring
- [ ] ./gradlew assembleDebug and ./gradlew lint are clean
