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

Expected seam for issue 02:

```kotlin
interface CopypartyFolderRepository {
    suspend fun loadFolder(server: ServerConfig, path: CopypartyPath, refresh: RefreshMode): FolderListing
}
```

The interface should return a domain `FolderListing` with visible folders/videos and hidden subtitle candidates. The caller should not know about copyparty JSON fields, extension filtering, natural sorting, or subtitle retention rules.

### Playback module

Expected seam for issue 04:

```kotlin
interface PlaybackSessionFactory {
    fun createSession(request: PlaybackRequest): PlaybackSession
}
```

The interface should accept a folder-local video selection and return a playback session model. Media3, direct HTTP URL construction, player-state mapping, and error classification belong behind this seam.

## Deletion Test

If `ServerConnectionRepository` were deleted, setup and settings would each need to learn URL normalization, copyparty validation, and persistence ordering. That would reduce locality and make broken-server persistence easier to reintroduce.

If `CopypartyListingClient` were deleted, Ktor and copyparty JSON details would leak into the connection and browser modules. That would create a shallow interface for future folder browsing.

## Checkpoint Scope

This document covers issue 01 and the seams it creates for issues 02 through 04. It intentionally does not design subtitle, decoder, gesture, PiP, or progress interfaces yet; those should be planned when their prerequisite modules exist.
