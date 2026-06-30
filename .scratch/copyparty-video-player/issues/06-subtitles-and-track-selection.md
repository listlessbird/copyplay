Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Add embedded and sidecar subtitle support plus audio/subtitle track selection. The browser should keep subtitle files hidden, but playback should attach matching sidecar subtitles from the current folder and expose embedded Media3 tracks. Behavior should follow Just Video Player's subtitle and track-selection model where applicable.

## Acceptance criteria

- [ ] Sidecar subtitles in the current folder are detected while remaining hidden from the browser.
- [ ] Sidecar matching supports same basename and common language/forced suffix variants.
- [ ] Supported sidecar formats include SRT, VTT, ASS, and SSA where Media3 supports them.
- [ ] Matching sidecar subtitles are attached to the Media3 media item for playback.
- [ ] Embedded subtitle tracks are exposed in the player track-selection UI.
- [ ] Audio tracks are exposed in the player track-selection UI.
- [ ] Users can enable, disable, and switch subtitle tracks.
- [ ] Users can switch audio tracks.
- [ ] Track IDs/selections are handled following the reference player's behavior where practical.
- [ ] Android caption preferences are respected for subtitle appearance.
- [ ] Reference-player subtitle style decisions, including embedded style handling and bold-style option where practical, are reflected in settings or defaults.
- [ ] Tests cover sidecar matching, hidden subtitle handling, media item subtitle attachment, track-selection state, and no-subtitle cases.
- [ ] Manual verification covers embedded subtitles, sidecar subtitles over HTTP, and audio track switching.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
