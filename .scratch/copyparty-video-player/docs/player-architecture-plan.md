# Player Architecture Plan

## Goal

Fix the player exit jank and replace the text-control overlay without turning `PlayerScreen` into a larger shallow module.

## Deepening direction

### Player route module

**Seam:** the Compose route where a `PlaybackSession` enters the player.

**Interface:** `PlayerScreen(session, progressStore, onBack)`.

**Implementation kept local:** ExoPlayer construction, Media3 `PlayerView`, progress snapshots, Android window/display mutations, gesture listener, and route exit cleanup.

**Reason:** this is a real seam because it adapts Compose navigation and Android/Media3 lifecycle. Moving lifecycle work into unrelated domain modules would create hypothetical seams with one adapter.

### Player chrome module

**Seam:** the visual HUD surface inside the player route.

**Interface:** small immutable state plus callbacks: current title, enabled actions, speed/resize/orientation labels, and one callback per player command.

**Implementation hidden:** icon choices, layout, disabled states, content descriptions, contrast, spacing, and overflow behavior.

**Reason:** this deepens the control UI. The caller should not know how the HUD lays out or which icons represent commands.

### Window chrome helpers

**Seam:** Android `Activity` mutations caused by playback.

**Interface:** enter/exit player mode and restore captured display state.

**Implementation hidden:** orientation restore, screen brightness restore, system bar visibility, and legacy/modern system UI flags.

**Reason:** player exit bugs concentrate around this seam. Keeping these mutations local improves locality without inventing a broad platform abstraction.

## Implementation order

1. Add `PlayerChrome.kt` with state/actions and icon-first HUD.
2. Add small window chrome helpers in `PlayerScreen.kt` or adjacent file if they grow.
3. Route all exits through one `exitPlayer` path.
4. Save final progress through the activity `lifecycleScope`, not a composition-bound coroutine.
5. Update the two issue files after build/test verification.

