# Playback Compatibility Notes

Status: implemented - manual media verification pending

## Renderer Policy

Copyplay uses Media3 `DefaultRenderersFactory` through the player screen adapter.

- Platform/hardware decoders remain first priority.
- Native FFmpeg/app decoder extension renderers are packaged from the reference AARs under `app/libs`.
- Extension renderers are enabled after platform decoders by default.
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

Copyplay now vendors the same style of prebuilt decoder AARs used by the reference player:

- `lib-decoder-ffmpeg-release.aar`
- `lib-decoder-av1-release.aar`
- `lib-decoder-iamf-release.aar`
- `lib-decoder-mpegh-release.aar`

The FFmpeg AAR includes native libraries for `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`. The reference FFmpeg build declares these enabled decoders: `vorbis`, `opus`, `flac`, `alac`, `pcm_mulaw`, `pcm_alaw`, `mp3`, `amrnb`, `amrwb`, `aac`, `ac3`, `eac3`, `dca`, `mlp`, and `truehd`.

## Verification

Automated coverage:

- `PlaybackCompatibilityTest` covers default renderer policy and fake Media3 error classification.
- Existing playback error tests cover user-facing copy for network/server and unsupported-codec failures.

Manual verification still needs representative files on device:

- common MP4/H.264/AAC,
- MKV with non-trivial audio such as AC3/EAC3/DTS if available,
- unsupported or partially supported media to confirm the codec error path,
- network/server failure to confirm source errors still show server-oriented copy.
