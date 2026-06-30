Status: ready-for-agent
Implementation status: completed

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Build the copyparty folder browser on top of the validated server connection. The browser should list shares and nested folders using the structured API, show only folders and video files, hide subtitle and unrelated files from the visible list, preserve hidden subtitle metadata for later playback, and provide phone-friendly navigation.

The completed slice should let a user browse real copyparty folders comfortably even before playback is fully implemented.

## Acceptance criteria

- [x] The browser loads the configured server root and nested folders from the copyparty structured API.
- [x] Visible listings include folders and recognized video files only.
- [x] Subtitle files are hidden from the visible browser list.
- [x] Non-video files are hidden from the visible browser list.
- [x] Hidden subtitle entries from the current folder are retained in the folder model for playback use.
- [x] Listings sort folders first and videos second using natural ordering.
- [x] The UI includes a compact breadcrumb path with jump-back behavior.
- [x] Android back moves to the parent folder before leaving the browser.
- [x] Pull-to-refresh reloads the current folder.
- [x] Loading, empty, and error states are clear and recoverable.
- [x] Tests cover filtering, hidden subtitle retention, natural sorting, breadcrumbs, parent navigation, and refresh behavior.

## Blocked by

- .scratch/copyparty-video-player/issues/01-app-scaffold-and-server-connect.md

## Comments

- 2026-06-30: Implemented the folder browser repository and Compose browser. The browser uses copyparty `?ls`, shows folders and video files only, keeps subtitle files hidden in the folder model, supports breadcrumbs, pull-to-refresh, and Android back-to-parent. Verified with `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest` and `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew assembleDebug`.
