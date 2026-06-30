Status: ready-for-human

Implementation status: implemented - manual phone verification pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Implement the phone playback controls and gestures expected from the Just Video Player reference. The player should feel like a serious video player, not a minimal media demo: playback speed, double-tap seek, horizontal seek gesture, vertical brightness and volume gestures, resize modes, PiP, audio focus behavior, and clear control overlays should work together.

## Acceptance criteria

- [x] Playback speed control is available in the player.
- [x] Double-tap seek works in both directions with clear feedback.
- [x] Horizontal swipe seeking works with clear feedback.
- [x] Vertical gesture brightness adjustment works on the expected side of the screen.
- [x] Vertical gesture volume adjustment works on the expected side of the screen.
- [x] Fit and crop resize modes are available.
- [x] Picture in Picture works on supported Android versions.
- [x] Audio focus is requested and handled correctly.
- [x] Playback responds sensibly to headphone or audio route changes.
- [x] Control overlay behavior follows the Just Video Player reference where applicable.
- [x] Gestures do not conflict badly with Android system navigation or player controls.
- [x] Tests cover gesture-to-command mapping, resize state, speed state, PiP eligibility, and audio-focus state where practical.
- [ ] Manual verification covers gestures, resize, PiP, and audio focus on the user's Android 16 phone.

## Notes

- Reference grounding came from Just Player/ExoBase source rather than tests; that project does not include a relevant local test suite.
- Automated coverage is in `PlayerControlsTest` for speed stepping, resize toggling, double-tap seek, horizontal scrub seek, vertical brightness/volume routing, PiP eligibility, and noisy-route policy.
- Manual Android 16 verification remains open for real touch feel, PiP system behavior, brightness/volume side effects, and audio-route changes.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
