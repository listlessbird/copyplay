Status: ready-for-agent
Implementation status: partial - manual verification pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Add embedded and sidecar subtitle support plus audio/subtitle track selection. The browser should keep subtitle files hidden, but playback should attach matching sidecar subtitles from the current folder and expose embedded Media3 tracks. Behavior should follow Just Video Player's subtitle and track-selection model where applicable.

## Acceptance criteria

- [x] Sidecar subtitles in the current folder are detected while remaining hidden from the browser.
- [x] Sidecar matching supports same basename and common language/forced suffix variants.
- [x] Supported sidecar formats include SRT, VTT, ASS, and SSA where Media3 supports them.
- [x] Matching sidecar subtitles are attached to the Media3 media item for playback.
- [x] Embedded subtitle tracks are exposed in the player track-selection UI.
- [x] Audio tracks are exposed in the player track-selection UI.
- [x] Users can enable, disable, and switch subtitle tracks.
- [x] Users can switch audio tracks.
- [x] Track IDs/selections are handled following the reference player's behavior where practical.
- [x] Android caption preferences are respected for subtitle appearance.
- [x] Reference-player subtitle style decisions, including embedded style handling and bold-style option where practical, are reflected in settings or defaults.
- [ ] Tests cover sidecar matching, hidden subtitle handling, media item subtitle attachment, track-selection state, and no-subtitle cases.
- [ ] Manual verification covers embedded subtitles, sidecar subtitles over HTTP, and audio track switching.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md

## Comments

- 2026-06-30: Implemented sidecar subtitle matching for same-basename, language-suffix, forced-suffix, SRT/VTT/ASS/SSA files; attached matching sidecars to Media3 media items; enabled Media3's subtitle button and built-in audio/subtitle track controls; applied Android user caption style/text-size defaults. Track selection currently relies on Media3's built-in player UI rather than Copyplay-owned persisted track IDs. Verified with `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest` and `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew assembleDebug`. Remaining gaps: direct Media3 track-selection UI tests and manual embedded subtitle, sidecar-over-HTTP, and audio-track switching verification.
