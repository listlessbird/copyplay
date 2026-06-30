# Copyplay Copyparty Video Player PRD

Status: ready-for-agent

## Problem Statement

The user wants a personal Android phone app for watching videos stored on a copyparty instance reachable through Tailscale. Existing media-library clients are more than the user needs right now. The immediate problem is simpler: browsing copyparty shares, seeing only folders and playable video entries, and launching a strong Android video player that handles common movie and TV-show playback needs well.

The app should not try to become a full media server client in v1. It should avoid scanning all media into Movies and TV Shows sections, avoid external metadata providers, and avoid complex server discovery. The first version should make the direct path from "open my copyparty server" to "watch this video with good playback controls and subtitles" reliable and pleasant.

## Solution

Build a native Android phone app named Copyplay using Kotlin, Jetpack Compose, and AndroidX Media3. The app will connect to one manually configured copyparty base URL, assuming Tailscale connectivity is already active outside the app. It will validate the configured URL by listing the copyparty root, then provide a video-focused folder browser.

The browser will show folders and video files only. Subtitle files and unrelated files will be hidden from the visible listing, but subtitle files should still be detected as sidecar candidates for playback. The app will stream videos directly over HTTP/HTTPS from copyparty using Media3 and preserve playback progress locally.

The playback behavior should follow Just Video Player, using the cloned reference under `reference/exobase` as the behavioral guide. The implementation should fit this app's Kotlin/Compose/copyparty architecture instead of importing the reference app wholesale. The app should reproduce the reference player's practical core: high-quality Media3 playback, embedded and sidecar subtitle support, audio/subtitle track selection, playback speed, double-tap and swipe seeking, brightness/volume gestures, resize modes, PiP, caption style integration, decoder priority where practical, and good error handling for unsupported codecs.

## User Stories

1. As the owner of a copyparty server, I want to enter my copyparty base URL, so that the app can connect to my personal media server.
2. As the owner of a Tailscale-only server, I want the app to work with plain HTTP, so that I do not need to configure TLS just for private Tailnet use.
3. As a personal user, I want the app to assume no copyparty authentication in v1, so that setup stays simple for my current deployment.
4. As a user setting up the app, I want the app to validate the URL before entering the browser, so that I know the server is reachable.
5. As a user with one media server, I want v1 to support one configured server, so that the app stays focused and easy to configure.
6. As a user who may add more servers later, I want the internal model not to block future multi-server support, so that v2 can grow without rewriting everything.
7. As a user browsing copyparty, I want to see top-level shares and folders, so that I can navigate the same structure I already use on the server.
8. As a video watcher, I want non-video files hidden, so that the browser is not cluttered with documents, images, archives, or other irrelevant entries.
9. As a video watcher, I want subtitle files hidden from the browser, so that folders stay focused on videos.
10. As a subtitle user, I want hidden subtitle files still detected automatically, so that subtitles work without cluttering the browsing UI.
11. As a user browsing deep folders, I want breadcrumbs, so that I can jump back to parent folders quickly.
12. As a phone user, I want Android back to move up one folder before exiting, so that navigation feels native.
13. As a user browsing changing server folders, I want pull-to-refresh, so that I can reload the current folder on demand.
14. As a user browsing media folders, I want folders first and videos second, naturally sorted, so that episode and movie ordering is predictable.
15. As a user on a private network, I want folder listings cached locally, so that browsing back and forth feels fast.
16. As a user with server changes, I want cached folder listings to refresh when stale, so that the app does not feel permanently out of date.
17. As a user opening a video, I want direct HTTP range streaming, so that playback starts without downloading the entire file.
18. As a user watching large files, I want seeking to work over HTTP, so that I can jump around movies and episodes.
19. As a user watching local-network media, I do not want offline download support in v1, so that the first version focuses on streaming playback.
20. As a user opening one video in a folder, I want the app to know the other videos in that folder, so that next and previous work naturally.
21. As a TV-show watcher, I want autoplay next by default, so that episodes continue without manual selection.
22. As a movie watcher, I want autoplay to stop when there is no next file, so that the app behaves sensibly at the end of a folder.
23. As a user who dislikes autoplay, I want a setting to disable autoplay next, so that playback can stop after each file.
24. As a user who partially watched a video, I want tapping it to resume immediately, so that continuing is frictionless.
25. As a user who wants to restart a video, I want a visible start-over action, so that immediate resume does not trap me.
26. As a user watching long videos, I want progress saved after meaningful playback, so that accidental opens do not pollute Continue Watching.
27. As a user managing progress, I want videos between 2% and 90% watched to appear as continue-watching items, so that active videos are easy to resume.
28. As a user finishing videos, I want videos at or beyond 90% treated as completed, so that completed items do not stay in Continue Watching.
29. As a user replacing a file with a better encode, I want progress keyed by server, path, size, and modified time where available, so that old progress does not apply to a different file.
30. As a user with embedded subtitles, I want subtitle tracks exposed in the player, so that I can select the right track.
31. As a user with sidecar subtitles, I want same-folder subtitle files matched automatically by basename and language suffix, so that common `.srt`, `.vtt`, `.ass`, and `.ssa` files work.
32. As a user with subtitle preferences, I want Android caption preferences respected, so that subtitle size, color, and style match system settings.
33. As a user with styled subtitles, I want behavior modeled after Just Video Player, so that embedded subtitle styling and bold-style settings behave predictably.
34. As a user with multiple audio tracks, I want audio track selection, so that I can choose the right language or commentary track.
35. As a user with multiple subtitle tracks, I want subtitle track selection, so that I can enable, disable, or switch subtitles.
36. As a user with language preferences, I want default audio-language behavior to follow the reference player, so that device/default language selection is sensible.
37. As a user watching varied encodes, I want the app to use Media3 hardware playback where possible, so that playback is efficient.
38. As a user watching files with uncommon audio codecs, I want Just Player-style decoder extension behavior where practical, so that files with AC3, EAC3, DTS, TrueHD, and similar audio have a better chance of playing.
39. As a user whose device cannot play a format, I want a clear error message, so that I know whether the file, codec, server, or network failed.
40. As a phone user, I want double-tap seek, so that I can quickly skip backward or forward.
41. As a phone user, I want horizontal swipe seek, so that I can scrub quickly without opening extra UI.
42. As a phone user, I want vertical brightness and volume gestures, so that I can adjust playback without leaving the video.
43. As a phone user, I want playback speed control, so that I can speed up or slow down playback.
44. As a phone user, I want fit and crop resize modes, so that I can handle videos with black bars or unusual aspect ratios.
45. As an Android phone user, I want Picture in Picture, so that playback can continue while I briefly use another app.
46. As a headphone user, I want playback to respond correctly to audio focus and device changes, so that audio interruptions do not behave badly.
47. As a Bluetooth headphone user, I want playback behavior to follow the reference player where applicable, so that audio/video sync issues are minimized.
48. As a user who opened videos recently, I want a home screen focused on videos I actually started, so that the app shows useful resume targets.
49. As a user who browses often, I want a Browse entry from home, so that I can quickly return to the copyparty browser.
50. As a user who changes servers or settings, I want a settings screen, so that I can update the base URL and playback preferences.
51. As the only intended user, I want no ads, tracking, or unnecessary permissions, so that the app remains private and simple.
52. As a developer implementing the app, I want player behavior guided by the Just Video Player reference, so that playback decisions are not invented from scratch.
53. As a developer implementing the app, I want the reference code to remain ignored under `reference/`, so that it can guide implementation without becoming part of this repository's source.
54. As the app owner, I want the Android app name to be Copyplay, so that the installed app is clearly branded for this project.

## Implementation Decisions

- Build a native Android phone app named Copyplay using Kotlin, Jetpack Compose, and AndroidX Media3.
- Use a single-activity architecture with Compose navigation for setup, home, browser, settings, and player routes.
- Embed Media3 player surfaces and mature player controls through Android View interop where that is the pragmatic path.
- Target modern Android. Use minSdk 29, latest available compileSdk/targetSdk, edge-to-edge UI, Material 3, PiP, modern network security behavior, and Android 16-compatible target behavior.
- Support one configured copyparty server in v1.
- Use manual base URL entry. Do not attempt Tailscale discovery or Tailscale API integration in v1.
- Assume no copyparty authentication in v1.
- Allow both HTTP and HTTPS base URLs. Plain HTTP is required for Tailnet-only deployments.
- Validate the configured server by calling the copyparty listing API and confirming the root can be read.
- Use copyparty's structured API for listing. HTML scraping is out of scope unless later inspection proves the API cannot provide required data.
- Present a file-browser-first app. Do not build Movies or TV Shows library sections in v1.
- Show folders and video files in the browser. Hide non-video files and subtitle files from the visible listing.
- Detect sidecar subtitles from hidden subtitle files in the current folder and attach them during playback.
- Use extension-based file filtering for v1. Include common video formats such as MP4, MKV, WebM, AVI, MOV, M4V, TS, M2TS, WMV, and FLV, subject to Media3/device support.
- Use extension-based subtitle detection for v1. Include SRT, VTT, ASS, and SSA, and keep room for TTML where Media3 supports it.
- Use breadcrumbs, pull-to-refresh, Android back-to-parent behavior, folders-first ordering, and natural sorting in the browser.
- Persist lightweight folder listing cache locally using Room or an equivalent Android persistence layer. Use stale-while-refresh behavior for folder listings.
- Persist playback progress and recent/continue-watching data locally.
- Key playback progress by server plus path plus size and modified timestamp when the copyparty API exposes size and modification time. Fall back to path identity when required.
- Stream videos directly from copyparty HTTP/HTTPS URLs. Do not download full files before playback.
- Rely on HTTP range behavior for seeking large files.
- Build a folder-local playlist when a video starts. Next, previous, and autoplay operate within the current folder's video list.
- Autoplay next is enabled by default. Include a setting to disable it.
- Save progress after playback passes 30 seconds.
- Show videos in Continue Watching when progress is greater than 2% and less than 90%.
- Treat videos at or beyond 90% as completed.
- Tapping a partially watched video resumes immediately. Provide a visible start-over action.
- Follow Just Video Player for player behavior. Use `reference/exobase` as a behavioral and implementation reference, especially for Media3 setup, track selection, subtitle handling, decoder preferences, gestures, PiP, caption styling, and playback state handling.
- Recreate the reference player's behavior inside this app's Kotlin/Compose architecture. Do not import the reference app wholesale.
- Prefer Media3 hardware/platform decoding for video and renderer fallback where available.
- Investigate and adopt Just Player-style FFmpeg/native audio extension support where practical for broad audio codec coverage.
- Expose audio track selection, subtitle track selection, playback speed, double-tap seek, horizontal seek gesture, brightness/volume gestures, resize modes, and PiP in v1.
- Respect Android caption preferences for subtitle appearance and follow the reference player's handling of embedded subtitle styles and bold subtitle style options.
- Do not add external metadata providers such as TMDB in v1.
- Do not scan all shares into a Movies/TV Shows media library in v1.
- Do not support multiple copyparty servers in v1.
- Do not add offline downloads in v1.

## Testing Decisions

- Test behavior at the highest practical seam: user-visible app flows and stable domain contracts rather than implementation details.
- For the copyparty client seam, use fake API responses that represent root listings, nested folder listings, video files, subtitle files, non-video files, size, modified time, and error responses.
- Browser tests should verify visible filtering, hidden subtitle retention for playback, breadcrumbs, natural sorting, pull-to-refresh behavior, back-to-parent navigation, stale cache display, and refresh replacement.
- Playback state tests should verify progress thresholds, completed thresholds, immediate resume, start-over behavior, folder-local playlist construction, next/previous, and autoplay next.
- Subtitle tests should verify basename and language-suffix sidecar matching, hidden subtitle files, embedded track exposure where Media3 test hooks allow it, and subtitle absence behavior.
- Player behavior should be covered with focused integration/manual verification against representative files because decoder, renderer, PiP, gestures, and audio-focus behavior depend on Android platform and device support.
- Use the Just Video Player reference as the parity checklist for playback behavior, not as a golden source for every implementation detail.
- Include manual device verification on the user's Android 16 phone for direct HTTP playback, seeking, subtitles, track selection, gestures, PiP, autoplay next, and resume.

## Out of Scope

- Movies and TV Shows library detection.
- Full media-server-client behavior.
- External metadata lookup, posters, ratings, casts, and episode descriptions.
- Multiple copyparty servers.
- Copyparty authentication.
- Tailscale discovery or Tailscale account integration.
- Offline downloads.
- Android TV optimization.
- Showing non-video files in the browser.
- File deletion or server mutation actions.
- Importing the Just Video Player app wholesale.
- GitHub issue or PR management for this feature.

## Further Notes

- The cloned Just Video Player reference lives under `reference/exobase`, and `reference/` is gitignored.
- The repo currently has no Android scaffold. The first implementation slice must create the Android project structure.
- The first real implementation pass should inspect copyparty's structured listing API with a representative server response before hardening the API parser.
- Playback is the core product risk. Browser and caching should stay simple enough that player parity with the reference remains the priority.
