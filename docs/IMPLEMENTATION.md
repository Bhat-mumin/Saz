# Saz 0.1.0 — implementation status

This is a real native Android local-player preview, not completion of every feature in the master brief. The UI direction follows the user's violet glass reference: original scenic illustrations, mood cards, luminous borders, a floating player/navigation area, full-screen player and lyrics, and a scenic ambient view.

## Implemented

- Kotlin/Compose/Material 3, Hilt injection, Room database, DataStore settings, Media3 ExoPlayer and MediaSessionService.
- Android file/folder picker import and MediaStore audio scan. Unicode metadata/search; artist/album grouping.
- Playback, seek, previous/next, shuffle, repeat one/all, queue add/remove/move, background service, audio focus/noisy-audio handling, wake lock, speed and sleep timer.
- Actual controller state drives both mini-player and full player. Playback errors have visible messaging.
- Favorites; playlist create/rename/delete and add/remove tracks; chronological playlist membership.
- Last-played timestamps with history opt-out and clearing. Atomic queue/position snapshots, restored paused.
- Song-specific user-imported LRC/static lyrics saved privately, timestamp parsing, line highlighting, follow/manual mode and text-size controls.
- Home/Search/Library/Discover/Settings; original mood illustration assets; user-created mood/regional collections; simple favorites/artist-prioritized discovery Flow.
- Scenic ambient player with minimal controls. Balanced glass effects and Performance preset; reduced-motion lyrics scrolling.
- Validated JSON backup merge for metadata, favorites, playlists and enabled history. Explicit limits and transactional database import.
- No network permission, analytics, embedded credentials, extraction of streaming media or copied Spotify resources.

## Important limitations / remaining work

- Spotify OAuth/App Remote and YouTube Data API discovery are not implemented or connected. Source labels explicitly say not linked; no fake results or playback.
- No licensed streaming catalogue, commercial lyrics catalogue, user profile synchronization or community playlists.
- Mood names label collections the user fills. They are not pre-licensed playlists. Scenic fallbacks are decorative Saz illustrations, not asserted album covers.
- Home Flow currently shuffles the local library; Discover Flow weights favorites and familiar artists. A unified, tested ranking algorithm with repetition controls remains to be implemented.
- History stores the latest timestamp per track, not an event-by-event listening log.
- Restoration preserves queue order, position and shuffle/repeat flags, but not the exact randomized shuffle traversal. Sleep timer does not survive process death.
- Library scanning merges metadata; stale/deleted files remain listed and report an error when played. Bulk relinking/removal needs a dedicated flow.
- Missing embedded artwork for picker-imported files falls back to Saz scenery. MediaStore album artwork is loaded when available.
- Queue snapshots are serialized atomically but building a snapshot currently walks the entire queue on player events. Very large queues require performance work.
- No equalizer, bass/treble controls, loudness normalization, crossfade, cast SDK or dedicated output chooser. Android system media output controls remain available.
- No measured High graphics preset, auto device calibration, adaptive artwork-derived palette, baseline profiles, macrobenchmarks or physical-device battery/60 FPS validation. Blur is a supported platform effect; Performance disables it in the player/lyrics.
- Full-screen player is scrollable on shorter displays. Full adaptive tablet layouts, RTL/device matrix, large-font and gesture testing remain.
- Backup v1 excludes settings, queue and lyric files; see BACKUP.md. URI permissions are not transferable. Repeated restore creates additional imported playlists.
- Approved icon remains a raster asset with minor edge fringe. Separate vector/monochrome/adaptive launcher and final wordmark/splash exports still need production work.
- Release configuration has R8/shrinking but no owner signing key. No production release or publishing occurred.

## Evidence

Final debug assemble and two JVM tests pass; lint reports 0 errors and 21 warnings. Two Android instrumentation tests passed on the visual build before the final status-bar and wording fixes. Final APK installation, launch, scan, background playback and paused restoration were checked manually. See TEST_RESULTS.md and docs/evidence for exact scope and environment limitations.
