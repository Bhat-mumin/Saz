# Saz 0.1.0 — implementation status

This native Kotlin/Compose preview uses the approved Walnut system: mineral surfaces, walnut text, plum controls, Literata headings and Source Sans 3 reading text. Local playback remains Media3-backed. Online providers are separated by their capabilities; see INTEGRATIONS.md.
## Implemented

- Kotlin/Compose/Material 3, Hilt injection, Room database, DataStore settings, Media3 ExoPlayer and MediaSessionService.
- Android file/folder picker import and MediaStore audio scan. Unicode metadata/search; artist/album grouping.
- Playback, seek, previous/next, shuffle, repeat one/all, queue add/remove/move, background service, audio focus/noisy-audio handling, wake lock, speed and sleep timer.
- Actual controller state drives both mini-player and full player. Playback errors have visible messaging.
- Favorites; playlist create/rename/delete and add/remove tracks; chronological playlist membership.
- Last-played timestamps with history opt-out and clearing. Atomic queue/position snapshots, restored paused.
- Song-specific user-imported LRC/static lyrics saved privately, timestamp parsing, line highlighting, follow/manual mode and text-size controls.
- Home/Search/Library/Discover plus a Settings action; user-created mood/regional collections; simple favorites/artist-prioritized discovery Flow.
- Quiet listening view with minimal controls. Opaque surfaces without blur; reduced-motion player entry and lyrics scrolling.
- Validated JSON backup merge for metadata, favorites, playlists and enabled history. Explicit limits and transactional database import.
- Explicit user-initiated network features; no analytics, embedded API credentials, streaming-media extraction or copied Spotify resources.
- YouTube discovery, public/unlisted playlist browsing and visible official IFrame playback; Audius free-download search; DownloadManager import; LRCLIB synced/plain lyrics when available.

## Important limitations / remaining work

- Spotify is an external search handoff. Spotify OAuth/App Remote and private YouTube OAuth libraries are not connected. Public YouTube playlists can be browsed by pasting a link; this does not read listening history or import private taste data.
- No licensed streaming catalogue, commercial lyrics catalogue, user profile synchronization or community playlists.
- Mood names label collections the user fills. They are not pre-licensed playlists. Scenic fallbacks are decorative Saz illustrations, not asserted album covers.
- Home Flow currently shuffles the local library; Discover Flow weights favorites and familiar artists. A unified, tested ranking algorithm with repetition controls remains to be implemented.
- History stores the latest timestamp per track, not an event-by-event listening log.
- Restoration preserves queue order, position and shuffle/repeat flags, but not the exact randomized shuffle traversal. Sleep timer does not survive process death.
- Library scanning merges metadata; stale/deleted files remain listed and report an error when played. Bulk relinking/removal needs a dedicated flow.
- Missing embedded artwork for picker-imported files falls back to Saz scenery. MediaStore album artwork is loaded when available.
- Queue snapshots are serialized atomically but building a snapshot currently walks the entire queue on player events. Very large queues require performance work.
- No equalizer, bass/treble controls, loudness normalization, crossfade, cast SDK or dedicated output chooser. Android system media output controls remain available.
- No measured High graphics preset, auto device calibration, adaptive artwork-derived palette, baseline profiles, macrobenchmarks or physical-device battery/60 FPS validation. Walnut removes blur from player/lyrics in both graphics presets.
- Full-screen player is scrollable on shorter displays. Full adaptive tablet layouts, RTL/device matrix, large-font and gesture testing remain.
- Backup v1 excludes settings, queue and lyric files; see BACKUP.md. URI permissions are not transferable. Repeated restore creates additional imported playlists.
- Approved icon remains a raster asset with minor edge fringe. Separate vector/monochrome/adaptive launcher and final wordmark/splash exports still need production work.
- Release configuration has R8/shrinking but no owner signing key. No production release or publishing occurred.

## Evidence

See PROVIDER_TEST_RESULTS.md, WALNUT_REVIEW.md and APK_REVIEW.md for current tests, screenshots, packaged-artifact observations and exact limits. TEST_RESULTS.md retains the earlier baseline and must not be read as evidence for the new provider integration.
