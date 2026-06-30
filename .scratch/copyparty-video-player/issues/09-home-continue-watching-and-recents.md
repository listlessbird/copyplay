Status: ready-for-human

Implementation status: implemented - manual home playback verification pending

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Build the home surface around videos the user has actually started. Home should provide Continue Watching, Recently Played, and Browse entry points using the progress/completion state from playback. It should not show recently browsed folders or attempt to present a Movies/TV Shows library.

## Acceptance criteria

- [x] Home appears after setup and uses persisted playback state.
- [x] Continue Watching shows only videos with saved progress greater than 2% and less than 90%.
- [x] Recently Played shows videos the user actually started, including completed videos when appropriate.
- [x] Videos that never passed the progress-save threshold do not appear as started videos.
- [x] Tapping a Continue Watching item resumes immediately.
- [x] Tapping a completed recent item opens playback with sensible start/resume behavior consistent with the PRD.
- [x] Browse entry opens the copyparty browser.
- [x] Home does not show folders as recent items.
- [x] Home does not classify Movies or TV Shows.
- [x] Tests cover home list eligibility, ordering, resume actions, completed handling, and browse navigation.

## Notes

- Home rows are derived only from `PlaybackProgressStore.progressEntries`; folder browsing history is not used.
- `HomeFeedPolicyTest` covers list eligibility, ordering, save-threshold exclusion, and resume/start-over actions.
- `PlaybackSessionFactoryTest` covers the single-item home handoff for continue and completed entries.
- Manual verification remains open for tapping real persisted rows after device playback.

## Blocked by

- .scratch/copyparty-video-player/issues/05-folder-playlist-autoplay-and-progress.md
