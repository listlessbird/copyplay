# Playback Compatibility Notes

Status: implemented - manual media verification pending

## Renderer Policy

Copyplay uses Media3 `DefaultRenderersFactory` through the player screen adapter.

- Platform/hardware decoders remain first priority.
- Extension renderers are enabled after platform decoders when present in the app build.
- Decoder fallback is enabled so Media3 can try lower-priority decoders when initialization fails.
- Dolby Vision profile 7 mapping is not applied because the current Media3 dependency does not expose the Just Player `setMapDV7ToHevc` adapter API used by the reference checkout.

The app policy is represented by `PlaybackCompatibilityPolicy.defaultSettings()` and adapted to Media3 in `PlayerScreen`.

## FFmpeg Extension Decision

Media3's FFmpeg decoder module is not a normal Maven dependency. The checked reference source under `reference/media3/libraries/decoder_ffmpeg/README.md` requires:

- depending on local Media3 modules,
- installing an Android NDK plus CMake/Ninja,
- fetching FFmpeg source,
- selecting decoders,
- building native binaries per ABI,
- accepting separate FFmpeg licensing and APK-size tradeoffs.

That is too much hidden build and distribution surface for the current Copyplay slice. The current app therefore documents `includesNativeFfmpegExtension = false` and leaves FFmpeg as an explicit future packaging decision rather than silently adding native binaries.

## Verification

Automated coverage:

- `PlaybackCompatibilityTest` covers default renderer policy and fake Media3 error classification.
- Existing playback error tests cover user-facing copy for network/server and unsupported-codec failures.

Manual verification still needs representative files on device:

- common MP4/H.264/AAC,
- MKV with non-trivial audio such as AC3/EAC3/DTS if available,
- unsupported or partially supported media to confirm the codec error path,
- network/server failure to confirm source errors still show server-oriented copy.
