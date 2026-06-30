Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Build the home surface around videos the user has actually started. Home should provide Continue Watching, Recently Played, and Browse entry points using the progress/completion state from playback. It should not show recently browsed folders or attempt to present a Movies/TV Shows library.

## Acceptance criteria

- [ ] Home appears after setup and uses persisted playback state.
- [ ] Continue Watching shows only videos with saved progress greater than 2% and less than 90%.
- [ ] Recently Played shows videos the user actually started, including completed videos when appropriate.
- [ ] Videos that never passed the progress-save threshold do not appear as started videos.
- [ ] Tapping a Continue Watching item resumes immediately.
- [ ] Tapping a completed recent item opens playback with sensible start/resume behavior consistent with the PRD.
- [ ] Browse entry opens the copyparty browser.
- [ ] Home does not show folders as recent items.
- [ ] Home does not classify Movies or TV Shows.
- [ ] Tests cover home list eligibility, ordering, resume actions, completed handling, and browse navigation.

## Blocked by

- .scratch/copyparty-video-player/issues/05-folder-playlist-autoplay-and-progress.md
