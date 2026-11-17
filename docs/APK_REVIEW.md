# APK package review

17 November 2026. This review is bound to the local debug APK below, not to a future GitHub build. Rebuilding can change the digest and requires a new package check.

## Artifact and evidence

- Target: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `c5d736f2c9a192fb0729df3db22676748f2389d87cee1629ee35483d48c64165`
- Size: 39,170,603 bytes.
- REA archive inspection: `ev_0fcc7a01884e1e919932f83fcbc20e13ae0ca6810c7a9a594ef56f8f3d4be190`.
- REA summary: `ev_7104e5bbb21ea1886a00cdd234f26ad72b4fdff67e3d20f04a6c8ba892cfd91f`.
- REA inventory: 297 graph occurrences (296 ZIP members), zero reported integrity contradictions. This is a central-directory inventory; it does not verify Android signatures or runtime behavior.

## SDK verification

Android SDK Build Tools 35.0.0 `apksigner verify --verbose --print-certs` succeeded. The APK has one Android Debug RSA-2048 signer and a valid APK Signature Scheme v2 signature. It is not an owner-signed production release.

- Certificate SHA-256: `81403240940f0c1397eb3500a3c08502b577a2bdb91b005b0a6bfd17b5ec694d`.
- Certificate SHA-1: `3574d1ba2754a329e4e88f51894ec489c6a33390`.

`aapt dump badging` and `aapt dump xmltree ... AndroidManifest.xml` observed:

- Package `app.saz.music`; version `0.1.0`, version code `1`; minimum API 26, target API 36.
- Launchable `app.saz.music.MainActivity`; application label Saz; debug mode enabled; automatic Android backup disabled.
- Permissions: Internet, audio/media read access, legacy external-storage read through API 32, foreground service, media-playback foreground service, wake lock, network-state access, and the AndroidX dynamic-receiver signature permission.
- Exported launcher activity and Media3 playback service. The debug variant also contains Compose PreviewActivity. AndroidX initialization provider and Room invalidation service are not exported.
- Packaged native libraries include arm64-v8a, armeabi-v7a, x86 and x86_64 variants.

## Packaged visual assets and credential scan

Python ZIP inventory independently observed `walnut_art.png`, `literata.ttf`, `source_sans.ttf`, and both OFL license files in the APK. A byte-pattern scan of every decompressed archive member found no Google API-key-shaped `AIza` credential. This bounded scan is not a general secret audit and did not read the user's credential file.

## Limits

REA Android decompilation could not run because the server has no caller-supplied `REA_JADX_MCP_JAR`. No engine or agent configuration was changed. The archive inspection and SDK checks above succeeded independently; no decompiled-code coverage is claimed.

This inspection did not launch the app, assess visual quality, establish playback behavior, certify platform policy compliance, or measure frame rate/battery use. Runtime and screenshot results belong in the separate provider and Walnut review reports.

YouTube API restrictions are signing-certificate-specific. The branch-build workflow is configured to restore a stable preview keystore from the GitHub Actions secret `SAZ_PREVIEW_KEYSTORE`. This avoids intentionally changing the preview signer on each branch build; it still requires verification of the published APK certificate. No production signing claim is made. Debug APKs signed with different certificates cannot update one another in place.
