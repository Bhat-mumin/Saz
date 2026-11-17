# Provider integration status

16 November 2026. See PROVIDERS.md for architecture, credentials, service references and scope limits.

Implemented:
- LocalMusicProvider over the existing Room library and Media3 playback.
- Separate search, metadata, library, playback-capability, lyrics and download contracts.
- YouTube public search/recent-upload discovery, country filtering, public/unlisted playlist-link browsing, pagination and metadata with developer-key configuration.
- Audius free-download search, excluding paid/gated/unavailable tracks and rechecking permission on download; API title/artist retained on import.
- Android Keystore key storage, Android app/certificate request headers, explicit auth/quota/network/configuration states.
- Official visible YouTube IFrame Player through Android WebView; local audio pauses on entry and the playable document is removed on background/close.
- Authorized direct HTTPS audio downloads via DownloadManager, status/cancel and import into the local library.
- User-initiated LRCLIB lookup for local recordings; exact-duration checks and existing LRC synchronization when timestamps are returned.
- External Spotify search handoff with app/browser fallback.

Not implemented or not guaranteed:
- Private YouTube library OAuth, Spotify OAuth/App Remote, or a licensed complete global catalogue.
- Downloading YouTube/Spotify streams, bypassing restrictions, or background YouTube audio.
- Guaranteed lyric coverage, fabricated timestamps or alignment of independent lyrics with edited YouTube videos.
- A production release, hardware performance certification or provider certification.

Verification results are recorded in PROVIDER_TEST_RESULTS.md. No live success should be inferred solely from code or a generated design preview.
