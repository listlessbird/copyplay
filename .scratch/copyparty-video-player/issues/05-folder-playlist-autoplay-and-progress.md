Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Add folder-local playlist behavior, autoplay next, and playback progress persistence. When a user starts a video from a folder, the player should know the other videos in that folder, provide next/previous behavior, autoplay the next video by default, and remember progress by stable file identity.

This slice should make the app useful for watching sequential episodes without building a media library.

## Acceptance criteria

- [ ] Starting a video constructs a playlist from the current folder's visible video files.
- [ ] Playlist ordering matches the browser's natural video ordering.
- [ ] Next and previous move within the current folder playlist.
- [ ] End of video autoplays the next folder video by default.
- [ ] End of the last folder video stops or exits completion state cleanly.
- [ ] A setting can disable autoplay next.
- [ ] Progress is saved after playback passes 30 seconds.
- [ ] Continue-watching eligibility is greater than 2% and less than 90% watched.
- [ ] At or beyond 90% watched is treated as completed.
- [ ] Progress keys use server, path, size, and modified time when available, with path fallback when required.
- [ ] Tapping a partially watched video resumes immediately.
- [ ] The player provides a visible start-over action.
- [ ] Tests cover playlist construction, next/previous, autoplay next, disabled autoplay, progress thresholds, completed thresholds, resume, and start-over.

## Blocked by

- .scratch/copyparty-video-player/issues/04-direct-http-video-playback.md
