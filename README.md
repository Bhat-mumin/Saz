# Saz

A native, offline-first Android music player with a Kashmir-inspired violet glass interface.

**Current delivery: 0.1.0 local-player preview.** The approved rounded glass icon and scenic UI direction are implemented. This is a working app, with real Media3 playback, a Room library, playlists, favorites, imported lyrics and metadata backup. Streaming integrations and production acceptance remain unfinished.

- [Installable debug APK](deliverables/Saz-0.1.0-debug.apk)
- [Actual app screenshots](docs/screenshots)
- [Build and API setup](docs/SETUP.md)
- [Implemented features and limitations](docs/IMPLEMENTATION.md)
- [Verification results](docs/TEST_RESULTS.md)
- [Roadmap](docs/ROADMAP.md)
- [Privacy](docs/PRIVACY.md) and [backup format](docs/BACKUP.md)
- [Architecture and reference analysis](docs/FEASIBILITY.md)

Requires Android 8.0 or later. Import your own audio or scan your device. No music catalogue or API credentials are bundled. The supplied Spotify APK/screenshots were reference inputs only; their code and assets are not included.

The debug APK under deliverables/ and proprietary reference-analysis artifacts are kept locally and excluded from Git. Build the APK using the setup instructions after cloning.

## GitHub APK builds

Every push and pull request runs the Android build, JVM tests and lint. You can also select **Actions → Build Android APK → Run workflow**. Open a successful run and download **Saz-debug-<run number>** from **Artifacts**; unzip it to get `Saz-debug.apk` and its SHA-256 checksum. Downloads require GitHub access to this private repository and expire after 30 days.

These are installable debug builds, not owner-signed production releases. APKs remain excluded from source control by `.gitignore`; the workflow uploads them separately as build artifacts. The `.gitignore` file itself is intentionally tracked so all clones use the same exclusions.
