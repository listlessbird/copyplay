Status: ready-for-agent
Implementation status: completed

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Create the Android app foundation and first-run connection flow for one copyparty server. A user should be able to install/run Copyplay, enter a copyparty base URL, allow HTTP or HTTPS, validate that the root listing can be reached through the structured copyparty API, persist the server configuration locally, and land in the app shell.

This slice should establish the app architecture: Kotlin, Compose, single Activity, Material 3, modern Android target behavior, minSdk 29, local persistence, networking, and basic navigation between setup, home, browser placeholder, and settings placeholder.

## Acceptance criteria

- [x] The repo contains a runnable native Android project using Kotlin, Jetpack Compose, Material 3, and AndroidX-compatible build tooling.
- [x] The installed Android app name is Copyplay.
- [x] The app targets a modern Android SDK and sets minSdk 29.
- [x] A first-run setup screen accepts a copyparty base URL.
- [x] HTTP and HTTPS base URLs are accepted by app/network security configuration.
- [x] The app validates the URL by calling the copyparty structured listing API for the root.
- [x] A successful validation persists the server configuration locally and navigates into the app shell.
- [x] A failed validation shows a clear error without persisting a broken configuration.
- [x] The app assumes no copyparty authentication in v1.
- [x] Settings can show and edit the configured base URL enough to reconnect.
- [x] Tests cover URL validation success, validation failure, persistence, and first-run versus configured launch behavior.

## Blocked by

None - can start immediately

## Comments

- 2026-06-30: Implemented the Android scaffold, server connection module, copyparty root `?ls` validation, DataStore-backed server configuration, setup/home/browser/settings shell, and JVM tests. Verified with `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest` and `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew assembleDebug`.
