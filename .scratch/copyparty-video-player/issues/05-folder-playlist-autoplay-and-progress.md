Status: ready-for-agent
Implementation status: completed

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Add folder-local playlist behavior, autoplay next, and playback progress persistence. When a user starts a video from a folder, the player should know the other videos in that folder, provide next/previous behavior, autoplay the next video by default, and remember progress by stable file identity.

This slice should make the app useful for watching sequential episodes without building a media library.

## Acceptance criteria

- [x] Starting a video constructs a playlist from the current folder's visible video files.
- [x] Playlist ordering matches the browser's natural video ordering.
- [x] Next and previous move within the current folder playlist.
- [x] End of video autoplays the next folder video by default.
- [x] End of the last folder video stops or exits completion state cleanly.
- [x] A setting can disable autoplay next.
- [x] Progress is saved after playback passes 30 seconds.
- [x] Continue-watching eligibility is greater than 2% and less than 90% watched.
- [x] At or beyond 90% watched is treated as completed.
- [x] Progress keys use server, path, size, and modified time when available, with path fallback when required.
- [x] Tapping a partially watched video resumes immediately.
- [x] The player provides a visible start-over action.
- [x] Tests cover playlist construction, next/previous, autoplay next, disabled autoplay, progress thresholds, completed thresholds, resume, and start-over.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md

## Comments

- 2026-06-30: Implemented folder-local playback sessions, playlist handoff from browser to player, Media3 playlist setup with autoplay-next preference, previous/next/start-over controls, DataStore-backed playback preferences/progress persistence, and progress eligibility rules. Verified with `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest` and `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew assembleDebug`.
