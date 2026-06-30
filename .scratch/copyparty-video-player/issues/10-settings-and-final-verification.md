Status: ready-for-human

Implementation status: settings implemented - manual end-to-end verification pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Complete the v1 settings and verification pass. Settings should expose the configured server URL and the playback preferences established in earlier slices, while final verification should exercise the real end-to-end app on a phone with a copyparty instance over Tailscale.

This slice should leave v1 coherent rather than a set of disconnected features.

## Acceptance criteria

- [x] Settings can view and change the configured copyparty base URL.
- [x] Changing the base URL revalidates the server before replacing the active configuration.
- [x] Settings expose autoplay-next behavior.
- [x] Settings expose playback preferences implemented from the reference player, such as track language, decoder priority, subtitle style, or PiP options where included by earlier slices.
- [x] Settings avoid unsupported or unimplemented options.
- [x] The app does not request unnecessary permissions for the v1 scope.
- [ ] End-to-end manual verification covers setup, root browsing, nested browsing, hidden non-video files, hidden subtitle files, direct playback, seeking, sidecar subtitles, embedded tracks, audio tracks, gestures, PiP, autoplay next, progress resume, and home continue-watching.
- [x] Known limitations are documented locally, especially any playback formats unsupported by the device or Media3 configuration.
- [x] The local issue/PRD status reflects remaining work accurately after verification.

## Notes

- Final manual checklist and known limitations are in `.scratch/copyparty-video-player/docs/final-verification.md`.
- The Android manifest currently requests only `INTERNET`.
- Manual verification remains open because it requires the user's Android 16 phone, Tailscale, and representative media on a copyparty server.

## Blocked by

- .scratch/copyparty-video-player/issues/06-subtitles-and-track-selection.md
- .scratch/copyparty-video-player/issues/07-just-player-core-controls.md
- .scratch/copyparty-video-player/issues/08-decoder-and-playback-compatibility.md
- .scratch/copyparty-video-player/issues/09-home-continue-watching-and-recents.md
