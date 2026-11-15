# Privacy and security — local-player build

- No analytics, advertising SDK, account, backend or network permission is included.
- Audio stays in its original location. Saz stores metadata, playlists, favorites, settings, queue position and user-imported lyrics in app-private storage.
- Listening history is enabled initially and can be disabled in Settings; disabling clears stored listening timestamps. No history leaves the device automatically.
- Audio permission is requested only when scanning. System file/folder pickers provide a narrower alternative. Saz does not request all-files access.
- Android automatic backup is disabled. Explicit export contains only the data documented in BACKUP.md.
- Playback session accepts this application and trusted Android media controllers. Queued source URIs are limited to content URIs.
- Queue snapshots and lyric saves use AtomicFile; snapshot writes are serialized. Room handles structured transactions.
- No credentials are present. OAuth secure storage/revocation is not implemented because external account integrations are not yet part of this build.
- Removing the app deletes its private library data and saved lyric files; original audio files remain. System document grants may need to be requested again on reinstall or another device.

No claim is made that generated branding has undergone trademark clearance. The supplied Spotify APK is reference-only and is not part of the app or distribution.
