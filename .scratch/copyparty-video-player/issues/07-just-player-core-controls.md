Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Implement the phone playback controls and gestures expected from the Just Video Player reference. The player should feel like a serious video player, not a minimal media demo: playback speed, double-tap seek, horizontal seek gesture, vertical brightness and volume gestures, resize modes, PiP, audio focus behavior, and clear control overlays should work together.

## Acceptance criteria

- [ ] Playback speed control is available in the player.
- [ ] Double-tap seek works in both directions with clear feedback.
- [ ] Horizontal swipe seeking works with clear feedback.
- [ ] Vertical gesture brightness adjustment works on the expected side of the screen.
- [ ] Vertical gesture volume adjustment works on the expected side of the screen.
- [ ] Fit and crop resize modes are available.
- [ ] Picture in Picture works on supported Android versions.
- [ ] Audio focus is requested and handled correctly.
- [ ] Playback responds sensibly to headphone or audio route changes.
- [ ] Control overlay behavior follows the Just Video Player reference where applicable.
- [ ] Gestures do not conflict badly with Android system navigation or player controls.
- [ ] Tests cover gesture-to-command mapping, resize state, speed state, PiP eligibility, and audio-focus state where practical.
- [ ] Manual verification covers gestures, resize, PiP, and audio focus on the user's Android 16 phone.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
