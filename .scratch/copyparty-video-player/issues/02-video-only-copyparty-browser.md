Status: ready-for-agent

## Parent

.scratch/copyparty-video-player/PRD.md

## What to build

Build the copyparty folder browser on top of the validated server connection. The browser should list shares and nested folders using the structured API, show only folders and video files, hide subtitle and unrelated files from the visible list, preserve hidden subtitle metadata for later playback, and provide phone-friendly navigation.

The completed slice should let a user browse real copyparty folders comfortably even before playback is fully implemented.

## Acceptance criteria

- [ ] The browser loads the configured server root and nested folders from the copyparty structured API.
- [ ] Visible listings include folders and recognized video files only.
- [ ] Subtitle files are hidden from the visible browser list.
- [ ] Non-video files are hidden from the visible browser list.
- [ ] Hidden subtitle entries from the current folder are retained in the folder model for playback use.
- [ ] Listings sort folders first and videos second using natural ordering.
- [ ] The UI includes a compact breadcrumb path with jump-back behavior.
- [ ] Android back moves to the parent folder before leaving the browser.
- [ ] Pull-to-refresh reloads the current folder.
- [ ] Loading, empty, and error states are clear and recoverable.
- [ ] Tests cover filtering, hidden subtitle retention, natural sorting, breadcrumbs, parent navigation, and refresh behavior.

## Blocked by

- .scratch/copyparty-video-player/issues/01-app-scaffold-and-server-connect.md
