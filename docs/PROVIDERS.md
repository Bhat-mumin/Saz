# Provider architecture and implementation plan

16 November 2026. Continues the user-approved Walnut UI; no Room schema migration.

## Incremental plan

1. Preserve Media3 local playback and Room data; expose provider-specific identifiers and independent search, metadata, library and playback capability interfaces.
2. Add YouTube Data API v3 search, recent-upload discovery, metadata, pagination and bounded in-memory caching. Handle missing/rejected keys, quota/rate limits, unavailable media and network failures separately.
3. Host the official YouTube IFrame Player inside Android WebView. Keep the player visible with controls and attribution. Pause local audio on entry; destroy the playable document when the screen backgrounds or closes.
4. Add authorized direct HTTPS audio downloads through Android DownloadManager, with status/cancel and explicit import into the local library. Add LRCLIB metadata/duration matching and existing LRC synchronization for local songs.
5. Run offline provider contract tests, opt-in live provider smoke tests, Android playback/persistence regression tests and rendered-screen reviews.

## Contracts and ownership

`providers/MusicProvider.kt` separates SearchProvider, MetadataProvider, LibraryProvider and PlaybackProvider. MusicItem uses a MediaKey containing ProviderId. The playback mode is local audio, embedded video or external app; seek/background/download capabilities are explicit.

LocalMusicProvider adapts the existing Room-backed LibraryRepository and supplies the MainViewModel track flow. Existing file import, local queue, favorites, playlists and Media3 service/controller remain intact. YouTubeProvider implements public search/metadata and embedded playback preparation; its private-library method returns an explicit OAuth-required result. YouTube items are never converted to local Track records or passed to Media3.

ProviderViewModel owns online search jobs, pagination, selection and provider configuration actions. ProviderHttp performs bounded HTTPS requests on Dispatchers.IO, with timeouts, response limits and no automatic retries. YouTube searches run only on explicit submission, retain at most 16 cached pages for five minutes, and use a separate rate-limit cooldown. LRCLIB serializes requests, spaces uncached lookups and honors Retry-After with jitter.

## Credentials

YouTube discovery requires a Google Cloud API key with YouTube Data API v3 enabled. Settings accepts a key or a key file. ApiKeyStore encrypts it using an Android Keystore AES-GCM key and AtomicFile in app-private storage. The value is not embedded in the APK, printed, included in backups or committed. Google requests include X-Android-Package and X-Android-Cert for the installed build. Register each allowed signing certificate; a release build uses a different certificate from the debug build.

The supplied developer key initially returned 403 without Android identity headers; a metadata request and search returned 200 with Saz's debug identity. This is a check of that specific configuration, not a promise that all deployment credentials will work.

The IFrame Player needs no Data API key. Its local HTML is loaded with the application-ID HTTPS base URL, which supplies client identity/Referer as specified by Google. Private YouTube account libraries would require OAuth; this increment does not claim account sign-in.

LRCLIB anonymous lookup needs no secret. Saz sends a required identifying User-Agent with its project URL. A registered public LRCLIB app ID may be useful at larger scale but is not required for this implementation. Spotify remains external handoff; OAuth/App Remote still requires a separate developer configuration.

## Scope limits

- Regional YouTube search is discovery, not an owned or licensed global catalogue. Recent uploads are not guaranteed to include every latest release. Country restrictions, indexing and owner embedding settings apply.
- No YouTube/Spotify audio extraction or downloading. DownloadManager accepts direct HTTPS audio links from sources the user confirms permit downloading. No subscription or DRM bypass. Audio must finish downloading and pass Android metadata import before it enters the local library.
- Downloads go to app-specific external Music storage and are removed on uninstall. Failed/cancelled jobs do not become local tracks. Completed imports merge by URI; a completed but invalid audio file reports zero imports rather than fake success.
- LRCLIB may return synchronized LRC, plain text, instrumental status or no match. Only supplied timestamps are synchronized. Recording duration must match within two seconds. Lookup is user initiated and sends title, artist and duration. This iteration does not align independent song lyrics with YouTube video intros/edits; videos retain YouTube's own caption controls where available.
- Embedded playback is foreground-only. Returning to the screen cues the video again without autoplay. No YouTube background audio, minimized video or combined local/YouTube queue.

## Primary references checked

- https://developers.google.com/youtube/v3/docs/search/list
- https://developers.google.com/youtube/v3/docs/videos/list
- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/terms/required-minimum-functionality
- https://lrclib.net/docs
- https://samplelib.com/sample-mp3.html (license-free synthetic download test sample)

## Free catalogue and playlists

Audius public search currently requires no paid plan or credential. Saz filters downloadable/available flags, gated conditions and API-key restrictions, then fetches fresh metadata before using the provider download URL. DownloadManager stores the file; private metadata preserves title and artist on local-library import. Revoked permission produces an unavailable state. This is a subset of Audius, not a complete global commercial catalogue. [Audius API](https://docs.audius.co/api/) and [artist download controls](https://help.audius.co/product/editing-your-release).

YouTube search accepts HTTPS public/unlisted playlist links and uses playlistItems with pagination. These items remain YouTube videos, not downloadable local tracks. Private playlists need OAuth; listening-history/taste import is not implemented. [YouTube authorization](https://developers.google.com/youtube/v3/guides/authentication).

Credentials still required for private accounts: a Google OAuth client and consent configuration for YouTube, and a Spotify developer client with approved redirect URI/scopes for Spotify. Neither private account integration is claimed complete. No client secret belongs in an APK.
