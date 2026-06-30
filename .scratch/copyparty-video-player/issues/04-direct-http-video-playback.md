Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Let a user tap a visible video in the copyparty browser and stream it directly through Media3. Playback should use the copyparty HTTP/HTTPS file URL, rely on range-capable streaming for seeking, and provide the initial player route/surface needed for the rest of the playback features.

The player should be implemented in this app's Kotlin/Compose architecture while following Just Video Player's Media3 behavior as the reference.

## Acceptance criteria

- [ ] Tapping a video in the browser opens a full-screen player route.
- [ ] The player streams from the copyparty HTTP/HTTPS URL without downloading the full file first.
- [ ] Media3 is used as the playback engine.
- [ ] The player supports play, pause, seek, duration, current position, buffering state, and failure display.
- [ ] Seeking works for range-capable server responses.
- [ ] Unsupported codec, network, and missing-file failures show clear user-facing errors.
- [ ] The player can return to the browser without losing app navigation state.
- [ ] The implementation follows the Just Video Player reference for core Media3 setup where applicable.
- [ ] Tests cover playback route construction, URL handoff, basic player state mapping, and error-state mapping with fake player/client seams.
- [ ] Manual verification notes cover direct HTTP playback and seeking against a real copyparty file.

## Blocked by

- .scratch/copyparty-video-player/issues/02-video-only-copyparty-browser.md
