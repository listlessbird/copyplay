Status: ready-for-agent
Implementation status: completed

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Persist folder listings locally and use stale-while-refresh behavior in the browser. When a user revisits a folder, the cached listing should render immediately if available, while stale data refreshes in the background. Manual refresh should replace the cache with fresh server data.

This slice keeps browsing responsive without introducing a media-library scanner.

## Acceptance criteria

- [x] Folder listings are persisted locally with folder path, visible children, hidden subtitle candidates, size, modified time where available, and fetched timestamp.
- [x] Reopening a cached folder displays cached data immediately.
- [x] Stale cached folders refresh in the background without blocking initial display.
- [x] Pull-to-refresh forces a fresh fetch and updates the cache.
- [x] API failures while cached data exists keep the cached listing visible and surface a non-destructive error.
- [x] API failures without cached data show a recoverable error state.
- [x] Cache behavior does not scan unrelated folders or build a global media library.
- [x] Tests cover cache hit, cache miss, stale refresh, forced refresh, and failed refresh with/without cached data.

## Blocked by

- .scratch/copyparty-video-player/issues/02-video-only-copyparty-browser.md

## Comments

- 2026-06-30: Implemented a SQLite folder-listing cache adapter behind `FolderListingCache`, with stale-while-refresh policy owned by `CopypartyFolderRepository`. Browser loads cached folders immediately, refreshes stale entries in the background, and keeps cached data visible on failed forced refreshes. Verified with `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest` and `JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew assembleDebug`.
