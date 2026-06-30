# Final Verification

Status: manual device verification pending

## Automated Verification

Current local verification command:

```sh
JAVA_HOME=$HOME/jdks/temurin-21 ./gradlew testDebugUnitTest assembleDebug
```

This covers JVM domain and ViewModel-adjacent behavior plus debug APK assembly.

## Permissions

The v1 manifest requests only:

- `android.permission.INTERNET`

No storage, media-library, location, notification, camera, or microphone permissions are requested.

## Manual Device Checklist

Run on the Android 16 phone with Tailscale connected and a reachable copyparty base URL.

- [ ] Fresh setup validates the copyparty URL and lands on Home.
- [ ] Settings shows the configured URL and rejects an invalid replacement URL.
- [ ] Root browsing lists folders and videos only.
- [ ] Nested browsing follows folders and supports back-to-parent navigation.
- [ ] Non-video files remain hidden from browser listings.
- [ ] Subtitle files remain hidden from browser listings but are available as sidecar candidates.
- [ ] Direct HTTP playback starts for representative MP4/H.264/AAC content.
- [ ] Seeking works from the Media3 controls.
- [ ] Sidecar SRT/VTT/ASS/SSA subtitle files attach when names match.
- [ ] Embedded subtitle tracks appear in Media3 track controls.
- [ ] Embedded audio tracks appear in Media3 track controls.
- [ ] Playback speed, fit/crop, double-tap seek, horizontal seek, brightness gesture, and volume gesture work without bad input conflicts.
- [ ] Picture in Picture starts on supported playback.
- [ ] Autoplay-next follows the Settings toggle.
- [ ] Progress resumes after meaningful playback.
- [ ] Completed videos leave Continue Watching and remain in Recently Played.
- [ ] Home Continue Watching rows resume immediately.

## Known Limitations

- Native FFmpeg audio decoding is not bundled. Media3 platform decoders are preferred, extension renderer mode is enabled for build-included extensions, and decoder fallback is enabled.
- Playback support for AC3, EAC3, DTS, TrueHD, and similar tracks depends on the device platform codecs unless a future build explicitly packages Media3's FFmpeg extension.
- Track selection persistence by per-file track ID is deferred; Copyplay currently relies on Media3's built-in track controls for embedded audio/subtitle switching.
- Manual verification for real phone gestures, PiP, audio-route changes, embedded tracks, and sidecar-over-HTTP behavior is still required.
