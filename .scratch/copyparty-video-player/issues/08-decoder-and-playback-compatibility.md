Status: ready-for-human

Implementation status: implemented - manual media verification pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Harden playback compatibility using the Just Video Player reference as the guide. The app should prefer efficient Media3/platform playback, investigate and adopt practical decoder extension support for broader audio codec coverage, expose decoder priority behavior where useful, and report unsupported media failures clearly.

## Acceptance criteria

- [x] Media3 renderer setup follows the reference player's relevant decisions where applicable.
- [x] Hardware/platform video decoding is preferred where the device supports it.
- [x] Renderer fallback behavior is configured where Media3 supports it.
- [x] FFmpeg/native audio extension support is investigated and included if practical for the project's build and distribution constraints.
- [x] Decoder priority or equivalent playback compatibility setting follows the reference behavior where practical.
- [x] The app handles common movie audio formats as well as practical for Media3 and included extensions.
- [x] Unsupported codec errors distinguish codec limitations from network/server failures where possible.
- [x] Build configuration documents any native extension tradeoffs such as APK size or ABI coverage.
- [x] Tests cover compatibility-setting state and error classification through fake playback errors.
- [ ] Manual verification uses representative media files with common containers/codecs, including at least one file with non-trivial audio tracks if available.

## Notes

- Copyplay now enables Media3 extension renderer mode after platform decoders and enables decoder fallback.
- Native FFmpeg is intentionally not bundled in this slice. Media3's FFmpeg module requires local module wiring, NDK/CMake/Ninja, FFmpeg source builds, ABI packaging, licensing review, and APK-size decisions.
- The build/distribution tradeoff is documented in `.scratch/copyparty-video-player/docs/playback-compatibility.md`.
- Manual verification remains open for real files and device codec behavior.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
