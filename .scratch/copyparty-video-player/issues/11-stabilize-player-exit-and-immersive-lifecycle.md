Status: ready-for-agent
Implementation status: architecture review pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Stabilize the player exit and immersive lifecycle so leaving playback feels clean instead of janky. When the user backs out of the player, Copyplay should save the latest eligible progress, stop presenting the video surface, restore Android system UI and player-mutated display state, and return to the prior route without disturbing browser/home navigation.

This should follow the Just Player reference behavior where applicable: save on pause/stop, restore controller/system bars on exit, clear player resources explicitly, and reset temporary orientation/display changes.

## Acceptance criteria

- [ ] The player back action and Android back both use the same clean exit path.
- [ ] Eligible progress is saved when leaving playback, without relying on a composition-bound coroutine that can be cancelled during navigation.
- [ ] ExoPlayer is paused, cleared, and released when the player route leaves composition.
- [ ] Temporary player display mutations are restored on exit, including orientation, screen brightness, and system bars.
- [ ] Leaving the player returns to the previous Copyplay route without losing browser/home navigation state.
- [ ] Tests or build verification cover the changed lifecycle path where practical.

## Blocked by

None - can start immediately

## Notes

- Reference anchors: `reference/exobase/app/src/main/java/com/brouken/player/PlayerActivity.java` lifecycle methods around `onPause`, `onStop`, `onBackPressed`, and `releasePlayer`.
