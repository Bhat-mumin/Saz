# Roadmap — 15 November 2026

The architecture, original branding and violet glass design direction are approved. Version 0.1.0 is an implemented local-player preview; it is not final acceptance of the full master brief.

| Milestone | Current status | Remaining acceptance |
|---|---|---|
| 1. Analysis and architecture | Complete and approved | Reference report retained |
| 2. Branding | Approved raster glass icon in app | Adaptive/monochrome/vector exports and wordmark |
| 3. Native project | Kotlin/Compose/Hilt/Gradle builds | Owner-signed release validation |
| 4. Playback | Media3 service/controller implemented; PCM smoke test passes | Physical audio focus, Bluetooth, lock screen and format matrix |
| 5. Library and persistence | Picker/folder/MediaStore, Room, atomic queue restoration | Missing-file cleanup, large-library and permission-denial matrix |
| 6. Interface | Five tabs, mood art, floating navigation/player, immersive player/lyrics | User visual review; tablet, font scaling, RTL and TalkBack matrix |
| 7. Search and collections | Local search, favorites, playlist CRUD, imported lyrics | Broader interaction and persistence tests |
| 8. Spotify | Explicitly unavailable | Eligible API account, OAuth and official remote implementation |
| 9. YouTube | Explicitly unavailable | API project, discovery and approved visible playback/handoff |
| 10. Regional/mood | User-filled collection entry points | Authorized catalogue/tagging and regional acceptance |
| 11. Flow and ambient | Local mix, scenic ambient, sleep timer | Unified tested ranking, repetition controls |
| 12. Backup/privacy/audio | Validated metadata merge, private local storage, speed/timer | Relinking, broader malformed-input matrix, EQ/crossfade |
| 13. Performance | Performance preset and R8 configuration | Physical-device startup, memory, frame and battery measurements |
| 14. Delivery | Debug APK, source, docs and emulator tests | Complete integration/hardware/security/release QA |

See IMPLEMENTATION.md for precise feature limits and TEST_RESULTS.md for executed checks. Do not count unexecuted acceptance items as passing. External-service work requires authorized account configuration; performance acceptance requires named physical hardware. No publishing has occurred.
