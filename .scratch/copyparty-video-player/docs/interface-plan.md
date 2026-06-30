# Copyplay Interface Plan

Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## Architecture Vocabulary

This plan uses the `codebase-design` vocabulary required by the architecture review: module, interface, implementation, seam, adapter, depth, leverage, and locality.

## First Slice Modules

### Server connection module

Files:

- `app/src/main/java/com/copyplay/domain/server/ServerConnectionRepository.kt`
- `app/src/main/java/com/copyplay/domain/server/CopypartyListingClient.kt`
- `app/src/main/java/com/copyplay/data/server/ServerConfigStore.kt`

Interface:

- `validateAndSave(rawBaseUrl: String): ServerConnectionResult`
- Accepts only `http` and `https` base URLs.
- Normalizes the configured base URL by trimming whitespace and trailing slash.
- Calls the copyparty root structured listing before persisting.
- Persists only after successful validation.
- Returns user-facing failure messages without leaking Ktor or DataStore details.

Depth:

The module hides URL normalization, structured-listing validation, and persistence ordering behind one method. The setup and settings screens get leverage from a small interface, and tests get locality because every success and rejection branch crosses the same seam.

### Copyparty listing module

Files:

- `app/src/main/java/com/copyplay/network/copyparty/KtorCopypartyListingClient.kt`
- `app/src/main/java/com/copyplay/network/copyparty/CopypartyHttpClientFactory.kt`

Interface:

- `listRoot(baseUrl: String): CopypartyListingResult`
- Calls `<baseUrl>/?ls`.
- Treats a response with `dirs` or `files` arrays as a valid copyparty structured listing.
- Maps network, unsupported-server, and invalid-response failures into domain failures.

Depth:

The module keeps Ktor, kotlinx serialization, HTTP status handling, and copyparty JSON shape inside one adapter. The next browser slice should deepen this module rather than exposing raw JSON to UI callers.

### Server config store module

Files:

- `app/src/main/java/com/copyplay/data/server/DataStoreServerConfigStore.kt`

Interface:

- `configuredServer: Flow<ServerConfig?>`
- `save(serverConfig: ServerConfig)`
- `clear()`

Depth:

The module hides DataStore key names and Android persistence details. It is deliberately narrow for issue 01; folder-listing cache in issue 03 should introduce a separate Room-backed module rather than expanding this interface.

## Future Interfaces

### Browser module

Seam for issue 02:

```kotlin
interface CopypartyFolderRepository {
    suspend fun loadFolder(server: ServerConfig, path: CopypartyPath): FolderLoadResult
}
```

Interface facts:

- `CopypartyPath` is a value object for root and nested folder paths.
- `FolderListing.visibleEntries` contains folders and recognized videos only.
- `FolderListing.hiddenSubtitles` retains subtitle files from the current folder for playback use.
- Folders sort before videos; each group uses natural ordering.
- Non-video files are intentionally absent from the listing model.
- Ktor response shape, URL encoding, copyparty `href` values, extension filtering, and natural sorting stay behind the module interface.

Depth:

The browser UI learns one method and one domain model. If the repository were deleted, every browser caller would need to relearn copyparty JSON, video/subtitle extension rules, and sorting. Keeping those facts local makes issue 03 cache replacement straightforward: the cache can become another adapter behind the same folder seam.

### Folder cache module

Seam for issue 03:

```kotlin
interface FolderListingCache {
    suspend fun get(server: ServerConfig, path: CopypartyPath): CachedFolderListing?
    suspend fun put(listing: FolderListing, fetchedAtEpochMillis: Long)
}
```

Interface facts:

- The cache key is server base URL plus folder path.
- Cached payloads preserve folder path, visible children, hidden subtitle candidates, size, modified time, and fetched timestamp.
- `CopypartyFolderRepository` owns cache policy: cache hit, cache miss, stale detection, forced refresh, and fallback when a network refresh fails.
- The UI receives `FolderLoadResult` values and never knows whether persistence is SQLite, Room, or another adapter.

Depth:

This module keeps persistence format, SQLite table names, JSON encoding, timestamps, and stale policy out of the browser UI. The cache has two adapters in practice: the SQLite implementation used by the app and in-memory fakes used by tests, so this is a real seam rather than a hypothetical one.

### Playback module

Seam for issue 04:

```kotlin
interface PlaybackRequestFactory {
    fun fromFolderVideo(server: ServerConfig, entry: FolderEntry.Video): PlaybackRequest
}
```

Interface facts:

- `PlaybackRequest` carries the server, file path, display title, and direct HTTP/HTTPS URL.
- The browser only hands off a selected `FolderEntry.Video`; it does not build URLs or know Media3 details.
- `PlaybackErrorMapper` classifies network/source, decoder/renderer, and unexpected playback failures into user-facing messages.
- The Compose player route hosts Media3 through Android View interop, but playback URL construction and error wording remain plain Kotlin test surfaces.

Depth:

This keeps direct URL construction, percent encoding, Media3 error vocabulary, and user-facing failure language behind a small interface. The first adapter is Media3-backed; tests use plain playback request/error mapping without needing an Android player instance.

## Deletion Test

If `ServerConnectionRepository` were deleted, setup and settings would each need to learn URL normalization, copyparty validation, and persistence ordering. That would reduce locality and make broken-server persistence easier to reintroduce.

If `CopypartyListingClient` were deleted, Ktor and copyparty JSON details would leak into the connection and browser modules. That would create a shallow interface for future folder browsing.

### Playback session module

Seam for issue 05:

```kotlin
interface PlaybackSessionFactory {
    suspend fun fromFolderSelection(
        listing: FolderListing,
        selectedVideo: FolderEntry.Video,
        startMode: PlaybackStartMode,
    ): PlaybackSession
}
```

Interface facts:

- `PlaybackSession` carries the folder-local video playlist, selected index, resume position, and autoplay-next setting.
- Playlist items are built from the current folder's visible video entries, preserving the browser's natural video ordering.
- Progress identity is server base URL plus file path plus size and modified timestamp when present.
- Resume is immediate for partially watched items; `PlaybackStartMode.StartOver` forces position `0`.
- The player receives a ready-to-play session and does not rebuild folder ordering, progress identity, or preference rules.

Depth:

The module hides playlist construction, progress lookup, resume thresholds, and autoplay preference lookup behind one interface. If this module were deleted, browser and player modules would both need to relearn progress identity and folder ordering, losing locality.

### Playback progress module

Seam for issue 05:

```kotlin
interface PlaybackProgressStore {
    suspend fun get(identity: PlaybackIdentity): PlaybackProgress?
    suspend fun save(progress: PlaybackProgress)
}
```

Interface facts:

- Progress is saved only after playback passes 30 seconds.
- Continue-watching eligibility is greater than 2% and less than 90% watched.
- At or beyond 90% watched is treated as completed.
- The store persists recently played entries for the later home module without requiring a media-library scan.

Depth:

The module keeps DataStore keys, JSON encoding, pruning, and threshold calculations away from UI callers. Tests can cross the same interface as the player and home modules.

### Playback preferences module

Seam for issue 05:

```kotlin
interface PlaybackPreferencesStore {
    val preferences: Flow<PlaybackPreferences>
    suspend fun setAutoplayNext(enabled: Boolean)
}
```

Interface facts:

- Autoplay next defaults to enabled.
- Settings can disable autoplay without knowing Media3 playlist details.
- The player maps the preference to Media3 `pauseAtEndOfMediaItems` behavior.

Depth:

This keeps preference persistence and Media3 playback behavior separate. The preference module is small but real because it has two callers: settings and playback session creation.

### Sidecar subtitle module

Seam for issue 06:

```kotlin
object SidecarSubtitleMatcher {
    fun match(
        server: ServerConfig,
        video: FolderEntry.Video,
        hiddenSubtitles: List<SubtitleCandidate>,
    ): List<PlaybackSubtitleTrack>
}
```

Interface facts:

- Matching sidecars are attached to `PlaybackRequest.subtitleTracks`.
- The matcher accepts same-basename subtitles and language or forced suffixes such as `.en.srt`, `.en.forced.ass`, and `.forced.srt`.
- Supported sidecar MIME mappings are SubRip, WebVTT, and SSA/ASS.
- Non-matching hidden subtitle files stay hidden and are not attached to unrelated videos.
- The player translates `PlaybackSubtitleTrack` into Media3 `SubtitleConfiguration` values with label, MIME type, language, subtitle role flags, and default or forced selection flags.

Depth:

The module hides suffix parsing, subtitle MIME mapping, URL construction, and default/forced flag policy. If this module were deleted, the player would need to learn browser hidden-file details and filename conventions, reducing locality.

### Track selection module

Seam for issue 06:

The first issue-06 implementation uses Media3's `PlayerView` and `PlayerControlView` track-selection controls as the module interface for embedded audio/subtitle tracks. Copyplay enables the subtitle button and uses Media3's built-in settings/audio menus instead of duplicating track-selection override code in app state.

Interface facts:

- Sidecar subtitles are exposed as Media3 text tracks through `SubtitleConfiguration`.
- Embedded audio and subtitle tracks are exposed by Media3 from the loaded media item.
- The player applies Android user caption style and text size defaults through `SubtitleView`.
- Track persistence by ID is deferred until Copyplay has a custom track-selection module; relying on Media3 keeps the current interface small.

Depth:

This keeps track enumeration, override application, and embedded track parsing behind Media3's tested player UI module. A custom module would become worth adding when Copyplay needs persisted per-file track IDs or app-specific track labels.

### Player control policy module

Seam for issue 07:

```kotlin
object PlayerGesturePolicy {
    fun doubleTapSeekTarget(side: SeekSide, currentPositionMillis: Long, durationMillis: Long?): Long
    fun horizontalScrubTarget(dragDistancePx: Float, density: Float, startPositionMillis: Long, durationMillis: Long?): Long
    fun verticalTarget(startX: Float, viewWidth: Int, borderPx: Float): VerticalGestureTarget?
}
```

Interface facts:

- Playback speed stepping, resize mode toggling, gesture-to-seek mapping, vertical brightness/volume routing, PiP eligibility, and noisy-route handling are plain Kotlin policies.
- The player UI owns Android side effects: Media3 speed/seek calls, `PlayerView` resize mode, activity brightness, music-stream volume, and `PictureInPictureParams`.
- The gesture policy follows the Just Player reference values where applicable: 10 second double-tap seek, capped horizontal scrub deltas, left-side brightness, right-side volume, and ignored edge regions.
- Audio focus remains Media3-owned through `setAudioAttributes(..., true)`; Copyplay only configures phone playback to pause sensibly on headphone or route changes.

Depth:

The module keeps touch math and platform eligibility rules out of the Compose screen. The UI can remain an adapter over Media3 and Android window/audio APIs, while tests cover the behavior that would otherwise be trapped inside a gesture listener.

## Checkpoint Scope

This document covers issues 01 through 07. It intentionally does not design decoder or home interfaces yet; those should be planned when their prerequisite modules exist.
