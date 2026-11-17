# Roadmap — 16 November 2026

The architecture and original launcher branding remain; Walnut was approved on 15 November 2026 to replace the violet glass interface. Version 0.1.0 is a provider-integration preview; it is not final acceptance of the full master brief.

| Milestone | Current status | Remaining acceptance |
|---|---|---|
| 1. Analysis and architecture | Complete and approved | Reference report retained |
| 2. Branding | Approved raster glass icon in app | Adaptive/monochrome/vector exports and wordmark |
| 3. Native project | Kotlin/Compose/Hilt/Gradle builds | Owner-signed release validation |
| 4. Playback | Media3 service/controller implemented; PCM smoke test passes | Physical audio focus, Bluetooth, lock screen and format matrix |
| 5. Library and persistence | Picker/folder/MediaStore, Room, atomic queue restoration | Missing-file cleanup, large-library and permission-denial matrix |
| 6. Interface | Walnut theme, four tabs plus Settings action, integrated mini-player, player/lyrics | User visual review; tablet, font scaling, RTL and TalkBack matrix |
| 7. Search and collections | Local search, favorites, playlist CRUD, imported lyrics | Broader interaction and persistence tests |
| 8. Spotify | External search handoff implemented; account not connected | Eligible API account, OAuth and official remote implementation |
| 9. YouTube | Public search/playlist links, metadata, visible IFrame playback implemented | Private account OAuth, broader playback device matrix |
| 10. Regional/mood | User-filled collection entry points | Authorized catalogue/tagging and regional acceptance |
| 11. Flow and ambient | Local mix, scenic ambient, sleep timer | Unified tested ranking, repetition controls |
| 12. Backup/privacy/audio | Validated metadata merge, private local storage, speed/timer | Relinking, broader malformed-input matrix, EQ/crossfade |
| 13. Performance | Performance preset and R8 configuration | Physical-device startup, memory, frame and battery measurements |
| 14. Delivery | Debug APK, source, docs and emulator tests | Complete integration/hardware/security/release QA |

See IMPLEMENTATION.md for precise feature limits and PROVIDER_TEST_RESULTS.md for current executed checks. Do not count unexecuted acceptance items as passing. External-service work requires authorized account configuration; performance acceptance requires named physical hardware. The workflow publishes APK previews only after successful builds/checks.

Audius artist-enabled free-download discovery, permitted direct audio downloads and LRCLIB lyrics are included. Complete global catalogue coverage, universal lyric synchronization and unrestricted commercial downloads are not promised. Saz has no paid tier or advertising SDK.
