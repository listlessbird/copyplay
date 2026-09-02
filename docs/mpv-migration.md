# libmpv Migration

## Purpose

Copyplay uses libmpv instead of Media3/ExoPlayer for direct Copyparty playback. The change gives the app mpv's FFmpeg-backed container and codec support and delegates ASS/SSA rendering to libass while preserving Copyplay's navigation, folder playlist, resume, progress, gestures, PiP, and player chrome.

## Dependency

The runtime dependency is `dev.jdtech.mpv:libmpv:1.0.0` from Maven Central. It is used through its instance-oriented API; Copyplay does not vendor native libraries or build mpv itself.

The referenced v1.0.0 build pins mpv 0.41.0, FFmpeg 8.1, and libass 0.17.4. Its AAR contains arm64-v8a, armeabi-v7a, x86, and x86_64 libraries. Copyplay deliberately applies no ABI filter so emulator support remains available.

## Reference repositories

All repositories were shallow-cloned under the ignored `reference/` directory. No reference source is tracked or copied into Copyplay.

| Repository | Commit | Use |
| --- | --- | --- |
| libmpv-android | `fcf6745703dc1265bca88f12fee8fc355ddf251e` | Exact wrapper API and packaged native build |
| mpv-android | `474111adc4abe5b67f3f8082c8a307e80d45c174` | Android Surface, AudioTrack, and mpv option patterns |
| mpvEx | `4151a45f862550a91b7a8efe35a6b19841242d48` | Compose/player architecture reference |
| findroid | `b170f7d8c5b3967ff5bdd4a6de3c5ef8433e5914` | Streaming-player concepts only; no GPL code copied |
| nextplayer | `5824581a828e9eb311ac1f8a2141f7825850ef74` | Player UX concepts only; no GPL code copied |
| mpv | `174b638f297626dc247d6436bd9ad0340be03a7c` | Command and property semantics |

## Architecture

`PlayerScreen` owns one `MpvPlaybackEngine` for the lifetime of a playback session and consumes only Copyplay state and actions. `PlaybackEngine`, `PlaybackEngineState`, and `PlaybackTrack` describe app capabilities without native types or mpv property names.

`MpvPlaybackEngine` is the native boundary. It owns the per-instance `MPVLib`, applies options before initialization, observes properties and events, maps seconds to milliseconds, loads one `PlaybackRequest`, and publishes typed state. Copyplay's `PlaybackSession` remains the source of playlist position and autoplay decisions.

`MpvVideoSurface` hosts an Android `SurfaceView` in Compose. Its `SurfaceHolder.Callback` attaches, resizes, and detaches the surface without destroying the player during temporary surface recreation. Compose chrome and gestures remain overlays; video frames are not drawn through Compose.

## mpv options

The configuration is intentionally small:

- `config=no` prevents device or desktop configuration from changing app behavior.
- `profile=fast`, `vo=gpu`, `gpu-context=android`, and `opengl-es=yes` provide the Android GPU output used by the reference players.
- `ao=audiotrack,opensles` prefers Android AudioTrack with OpenSL ES fallback, and `audio-set-media-role=yes` identifies media playback.
- `hwdec=mediacodec,mediacodec-copy` requests Android hardware decoding and allows mpv to fall back to FFmpeg software decoding. Software mode sets `hwdec=no`. `hwdec-current` reports what mpv actually selected.
- `cache=yes`, `cache-pause-initial=yes`, `demuxer-max-bytes=64MiB`, and `demuxer-max-back-bytes=32MiB` provide bounded remote-media buffering and backward-seek cache.
- `idle=yes`, `keep-open=yes`, and `force-window=no` keep one headless player instance between Copyplay items without creating another Android window.
- `save-position-on-quit=no` leaves resume ownership with Copyplay. `ytdl=no` avoids unrelated URL extraction.

Fit uses `panscan=0`; crop uses `panscan=1`. Pinch zoom maps the Copyplay scale to mpv's logarithmic `video-zoom` property without changing video aspect ratio. Speed maps to `speed`, and absolute seeking sets `time-pos` after Copyplay computes the target.

## Subtitle handling

Embedded subtitles are read from mpv's `track-list`. External `.ass`, `.ssa`, `.srt`, and `.vtt` sidecars are added once per load with `sub-add`, including title and language metadata. Default and forced hints are passed as mpv flags when present. mpv/libass performs all subtitle parsing, styling, positioning, and rendering; Copyplay has no Android subtitle renderer or ASS conversion path.

Sidecar matching still anchors subtitles to the complete video base name. It additionally recognizes common `default`, `sdh`, `signs`, `full`, and `commentary` qualifiers, with tests retaining rejection of unrelated files.

## Track handling

The adapter reads `track-list/count` and each track's `id`, `type`, `title`, `lang`, `codec`, `selected`, `default`, `forced`, and `external` properties into `PlaybackTrack`. Audio selection sets `aid`; subtitle selection sets `sid`, and `sid=no` disables subtitles. A Copyplay-owned Material 3 dialog replaces Media3's track dialog.

## Hardware decoding

Hardware mode requests `mediacodec,mediacodec-copy`; software mode uses `no`. The UI labels actual software fallback when `hwdec-current` reports `no`, instead of claiming that requested hardware decode is active. Runtime changes preserve the current mpv file and Copyplay session; they do not reset the folder playlist or progress rules.

Hardware decoding was not validated on a physical arm64 device during this migration. The x86_64 emulator is suitable for integration and lifecycle checks, not proof of real-device MediaCodec performance.

## Lifecycle

The playback screen creates and releases exactly one native player. MPV observers are registered after initialization and removed before `destroy`. Surface attach/detach and release are idempotent, and commands are rejected after release. The engine stores application context only.

Activity lifecycle events pause playback in the background and resume only playback that was previously intended to play. PiP does not create another engine and does not trigger the background pause path. Audio focus and `ACTION_AUDIO_BECOMING_NOISY` are handled by an Android-only controller outside the domain layer.

Progress remains Copyplay-owned and is persisted about every five seconds while playing, before item changes, on back/disposal, and at natural EOF. Autoplay advances only for EOF near the known duration; replacement, release, premature EOF, and native failures do not advance the folder playlist.

## Licensing

The `libmpv-android` Kotlin/JNI wrapper is MIT licensed. The native contents of its AAR have their own terms. In the pinned source, FFmpeg is built with `--enable-gpl` and `--enable-version3`; mpv can be LGPL or GPL depending on its build, and the packaged combination must therefore be treated as GPLv3 for distribution review. libass is ISC licensed, and other bundled components retain their respective notices.

Before distributing an APK, Copyplay needs a project-level licensing decision plus the corresponding source offer/source availability, copyright notices, and license texts for the exact AAR build. A permissive wrapper license alone is not sufficient. Findroid and Next Player were GPL reference material only; mpvEx is Apache-2.0 reference material; no source was copied from them.

## Verification

Baseline on the pre-migration tree:

- `./gradlew test assembleDebug`: passed.
- `just build-debug`: passed.
- Debug APK: 95,857,829 bytes.

Automated coverage exercises time conversion, state reduction, track parsing, external subtitle commands, hardware/software policy, playlist EOF policy, sidecar matching, controls, and the existing progress/session behavior.

- `./gradlew test assembleDebug`: passed, 61 unit tests with zero failures.
- `just build-debug`: passed.
- `git diff --check`: passed.
- Final debug APK: 219,967,503 bytes.
- Delta: +124,109,674 bytes (+129.5%). The debug APK contains all four native ABIs; release size can be reduced later with per-ABI artifacts without removing x86_64 development support.

Emulator/network checks performed on an Android 16 x86_64 AVD:

- The emulator had no Tailscale Android client installed. Its Linux host was connected to Tailscale, and Android could reach the host's Copyparty service through the host's tailnet IPv4 address via emulator NAT.
- Copyplay validated and saved that tailnet address, reported it online, listed the remote shares, and streamed instead of downloading the whole files first.
- A small H.264/AAC MP4 reached active playback and accepted pause/play and seek controls.
- A large MP4 with a matching external ASS sidecar reached active playback and accepted a forward seek.
- A multi-gigabyte H.264/DDP MKV with multiple tracks reached active playback and accepted a forward seek.
- The semantic instrumentation used for these targeted checks completed its playback assertions but exposed an Android 16 ActivityScenario/Navigation teardown exception after the player route had already been popped. That temporary harness was not retained; normal in-app back navigation was verified separately without a crash.

## Known limitations

- No physical arm64 device was available, so real MediaCodec hardware decode, performance, HDR, Bluetooth/headphone routing, and physical-device PiP remain unverified.
- External ASS loading was exercised with a real sidecar, but styled libass output was not held on screen long enough for a visual style/positioning audit.
- No representative external VTT, HEVC, AV1, VP9, DTS, TrueHD, AVI, WMV, FLV, M2TS, TS, or MOV fixture was manually exercised in this environment.
- PiP, rotation, surface reattachment, focus loss, and noisy-route behavior are implemented but still need the full physical-device matrix.
- The emulator did not itself join the tailnet; it reached a tailnet-bound host through the already-connected development machine.
- Release distribution is blocked on the GPL/native-notice review described above.
