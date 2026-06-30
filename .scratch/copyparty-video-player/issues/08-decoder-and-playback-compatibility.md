Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Harden playback compatibility using the Just Video Player reference as the guide. The app should prefer efficient Media3/platform playback, investigate and adopt practical decoder extension support for broader audio codec coverage, expose decoder priority behavior where useful, and report unsupported media failures clearly.

## Acceptance criteria

- [ ] Media3 renderer setup follows the reference player's relevant decisions where applicable.
- [ ] Hardware/platform video decoding is preferred where the device supports it.
- [ ] Renderer fallback behavior is configured where Media3 supports it.
- [ ] FFmpeg/native audio extension support is investigated and included if practical for the project's build and distribution constraints.
- [ ] Decoder priority or equivalent playback compatibility setting follows the reference behavior where practical.
- [ ] The app handles common movie audio formats as well as practical for Media3 and included extensions.
- [ ] Unsupported codec errors distinguish codec limitations from network/server failures where possible.
- [ ] Build configuration documents any native extension tradeoffs such as APK size or ABI coverage.
- [ ] Tests cover compatibility-setting state and error classification through fake playback errors.
- [ ] Manual verification uses representative media files with common containers/codecs, including at least one file with non-trivial audio tracks if available.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
