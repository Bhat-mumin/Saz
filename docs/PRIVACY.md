# Privacy and security

Updated 16 November 2026 for provider-aware integration.

- Saz has no analytics SDK, advertising SDK or Saz account. INTERNET permission is now required for user-initiated provider requests and the YouTube player.
- Local audio stays in its original location. Metadata, playlists, favorites, settings, queue position and imported lyrics are stored privately. Downloaded audio uses app-specific external Music storage.
- YouTube searches send the query, optional region, API key and Android app identity to Google. Video metadata requests send the selected video ID. The official embedded player contacts YouTube/Google, may use cookies and is governed by YouTube terms and Google privacy policies linked in Settings. It is not an audio extraction service.
- LRCLIB lookup sends song title, artist and duration only when requested. Returned lyrics are held in memory in this increment; user-imported lyric files are saved privately. Saz does not fabricate synchronized timestamps.
- Direct downloads send the supplied HTTPS URL to that source through Android DownloadManager. Downloads are user initiated and require confirmation that the source permits downloading. Failed or incomplete files do not enter the music library.
- API keys are encrypted with Android Keystore AES-GCM and saved atomically in app-private storage. The key is not embedded in source/APKs, logged or included in Saz's metadata export. Remove key in Settings revokes local use; revoke it in Google Cloud if necessary. The supplied credential file was treated as data only.
- There is no provider-account OAuth sign-in or token store. An API key does not grant access to private YouTube libraries. Spotify remains an external handoff.
- Listening history is local and can be disabled; disabling clears saved timestamps. No listening history is automatically uploaded.
- Audio permission is requested for scanning. File/folder pickers are narrower alternatives. Saz does not request all-files access.
- Android automatic backup is disabled. Explicit metadata export is documented in BACKUP.md. Downloads, credentials and fetched lyrics are not exported.
- YouTube playback stops when the embedded screen is hidden or backgrounds. WebView file/content access and mixed-content requests are disabled; no JavaScript-to-native bridge is exposed. Links leave the embedded page only through user actions.
- Removing Saz deletes its private data, encrypted credential file, saved lyrics and app-specific downloads. Original imported files outside Saz remain.

Production distribution still needs a published privacy policy, review of provider terms and credential restrictions, and release-device security testing. No trademark clearance or provider certification is claimed.

### Audius free downloads

Free-download searches send the query and app identifier to Audius. Save fetches fresh metadata, then downloads from the provider-supplied HTTPS content host. Download identifiers, title and artist are stored privately to retain metadata on import. Saz requires no account or payment and sends no listening history to Audius. YouTube playlist browsing sends the playlist identifier to Google; private playlist and listening-history access is not implemented.
