Status: ready-for-agent
Implementation status: architecture review pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Replace the always-visible text-button player overlay with compact icon-first playback chrome. Copyplay should feel like a mature phone video player: the video remains the primary surface, controls use familiar media icons, the current title stays readable, and extra player actions sit in HUD chrome rather than in a debug-like button cluster.

Keep the implementation inside the app's Kotlin/Compose/Media3 structure. Use the Just Player and MX Player style as behavior references, not as a wholesale UI import.

## Acceptance criteria

- [ ] The large text-button overlay is removed from normal playback.
- [ ] Player chrome exposes back, previous, next, start-over, speed, resize, rotation, and PiP with icon-first controls where supported.
- [ ] The current video title is visible in player chrome and ellipsizes cleanly.
- [ ] Disabled actions, such as previous on the first item or PiP when unavailable, have disabled visual and interaction states.
- [ ] Gesture feedback remains short and centered, without colliding with the player chrome.
- [ ] The HUD visual treatment is restrained, high contrast, and consistent with the Copyplay dark product theme.
- [ ] Build verification passes after the HUD replacement.

## Blocked by

- .scratch/copyparty-video-player/issues/11-stabilize-player-exit-and-immersive-lifecycle.md

## Notes

- Reference anchors: `reference/exobase/app/src/main/res/layout/exo_player_control_view.xml` and the dynamic icon button setup in `PlayerActivity.java`.
