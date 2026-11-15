# Saz JSON backup format v1

UTF-8 JSON object with `format: "saz-backup"`, `version: 1`, and arrays:

- `tracks`: local `content://` URI, title, artist, album, duration in milliseconds, favorite flag, lastPlayed epoch milliseconds (zero when history is disabled).
- `playlists`: original numeric ID and name.
- `entries`: playlist ID, track URI, added epoch milliseconds.

Export excludes audio bytes, artwork bytes, lyric files, settings, queue snapshots and URI permissions. Credentials are never exported. Those exclusions are current v1 limitations, not claims of full master-brief backup coverage.

Import limits: 10 MB, 50,000 tracks, 1,000 playlists, 100,000 entries, strings at most 4,096 characters and playlist names at most 100 characters. Version, URI scheme, non-negative times, identifier uniqueness and referential integrity are validated before database writes.

Import merges track identities without removing existing data. Existing favorites are retained; listening timestamps are merged only when history is enabled. Imported playlists get new IDs, so repeated imports create separate playlists. All database writes happen in one Room transaction. Missing URI grants cannot be reconstructed from JSON; the user must relink by importing accessible files. Export is through Android's document picker; storage-provider write errors are surfaced to the user.
