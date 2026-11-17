# Saz

A native, offline-first Android music player with the approved Walnut visual direction.

**Current delivery: 0.1.0 provider-integration preview.** The Walnut Compose interface is implemented; the existing launcher icon is retained. This is a working app, with real Media3 playback, a Room library, playlists, favorites, imported lyrics and metadata backup. YouTube public discovery/playlist browsing and visible playback, Audius free-download discovery, authorized downloads and on-demand lyrics are implemented. Private account connections and production acceptance remain unfinished.

- [Download the APK from Releases](https://github.com/Bhat-mumin/Saz/releases) — choose `Saz-debug.apk`, no ZIP extraction
- [Actual app screenshots](docs/screenshots)
- [Build and API setup](docs/SETUP.md)
- [Implemented features and limitations](docs/IMPLEMENTATION.md)
- [Provider verification](docs/PROVIDER_TEST_RESULTS.md) and [Walnut visual review](docs/WALNUT_REVIEW.md)
- [Roadmap](docs/ROADMAP.md)
- [New visual direction proposals](docs/design-exploration/README.md) — Walnut approved
- [Provider integration progress](docs/INTEGRATIONS.md) — provider capabilities, free downloads and remaining account work
- [Privacy](docs/PRIVACY.md) and [backup format](docs/BACKUP.md)
- [Architecture and reference analysis](docs/FEASIBILITY.md)

Requires Android 8.0 or later. Import your own audio or scan your device. No music catalogue or API credentials are bundled. The supplied Spotify APK/screenshots were reference inputs only; their code and assets are not included.

The debug APK under deliverables/ and proprietary reference-analysis artifacts are kept locally and excluded from Git. Build the APK using the setup instructions after cloning.

## GitHub APK builds

Every push and pull request runs the Android build, JVM tests and lint. Successful branch pushes and manual runs then publish **Saz-debug.apk** as a GitHub Release asset. Open [Releases](https://github.com/Bhat-mumin/Saz/releases) and select **Saz-debug.apk**. The APK is also uploaded directly to Actions with archive wrapping disabled. GitHub's automatically generated source ZIP files are not the app.

The publishing job runs only after the build/check job succeeds and never for pull requests. `SAZ_PREVIEW_KEYSTORE` is an encrypted Actions secret holding a stable debug signing identity, so subsequent previews can update the same app. It is not a production signing key. API credentials are never embedded in the APK or committed. This private repository requires appropriate GitHub access to download releases.

Saz adds no advertising and has no paid feature tier. Local and downloaded audio plays ad-free. Optional YouTube playback is controlled by YouTube and may show its ads; Saz does not remove them. No free supported provider guarantees every commercial release or unrestricted downloads.
