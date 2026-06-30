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

## Checkpoint Scope

This document covers issue 01 and the seams it creates for issues 02 through 04. It intentionally does not design subtitle, decoder, gesture, PiP, or progress interfaces yet; those should be planned when their prerequisite modules exist.
