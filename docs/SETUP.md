# Build and run Saz

## Toolchain

- Windows, Linux or macOS with JDK 17 or 21 (this workspace uses Temurin 21.0.13).
- Android SDK platform 36 and Build Tools 35.0.0.
- Gradle wrapper 8.13; Android Gradle Plugin 8.13.2; Kotlin and Compose compiler 2.0.21.
- Android 8.0/API 26 or later to install. This is a native Kotlin/Compose application.

Set `JAVA_HOME` to your compatible JDK. Set `ANDROID_HOME` or create an untracked `local.properties` with your SDK path. Windows example: `sdk.dir=C\:/Users/YourName/AppData/Local/Android/Sdk`.

Run from the project root:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Use `./gradlew` on macOS/Linux. The first build needs Google Maven/Maven Central downloads. If the host has broken IPv6 routing, the verified workspace command includes `-Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"`.

Debug output: `app/build/outputs/apk/debug/app-debug.apk`. `adb install -r` installs it on an authorized device. Launch `app.saz.music/.MainActivity`.

## Using this build

1. Import audio files or select a folder, or grant audio access and scan the device.
2. Tap a song to play. The bottom strip opens the full player.
3. Use the heart to favorite; use + to add to queue or a playlist.
4. Create/rename/delete playlists in Library. Deleting a playlist does not delete audio.
5. Player includes queue, shuffle/repeat, speed, sleep timer, static ambient view and LRC/plain-text import.
6. Backup in Settings exports Saz metadata as JSON. Import merges tracks and creates imported playlists. Audio files and URI grants are not included; re-import files after moving devices.

The UI uses the approved Walnut design with bundled OFL fonts and opaque Compose surfaces. Automatic graphics calibration and measured frame-rate targets remain unverified.

## Release configuration

The release variant enables R8 and resource shrinking. It is intentionally unsigned until the owner supplies a signing key. Do not distribute a debug-signed APK as a production release or commit signing material. Configure signing outside tracked source using the Android Studio Generate Signed App Bundle/APK workflow or a private Gradle init script. The application has not been published.

## External integrations

No API keys or accounts are bundled. Public YouTube discovery/playlists and visible playback are implemented; Spotify is an external handoff. See the current provider setup below.

- Spotify requires an eligible developer application, registered redirect URI, authorized users/scopes, PKCE implementation and installed Spotify for App Remote. Validate the current developer-mode limitations against the account before implementation. Saz does not extract Spotify audio.
- YouTube discovery requires an API project and appropriately restricted API configuration. Playback must use an approved visible player or official-app handoff; no background audio extraction.
- Licensed music and lyrics providers require authorization and attribution terms. Local user-supplied music/LRC files work without these services.

## Test boundaries

JVM tests exercise LRC parsing. Android tests create their own PCM WAV audio and use a real MediaController/ExoPlayer session; library tests verify Room merge behavior and backup rejection. They do not require a streaming account. Emulator results do not establish physical-device audio quality, Bluetooth behavior, battery life or 60 FPS performance.


## Walnut provider preview (16 November 2026)

The current build uses the approved Walnut native Compose design. No API credentials are bundled. Settings > YouTube discovery > Import key file accepts the developer-provided key file and stores the key using Android Keystore encryption. Enable YouTube Data API v3 and restrict the key to app.saz.music and the preview certificate SHA-1 below. Search supports public music discovery and public/unlisted playlist links; private account OAuth is not connected.

Search > Free downloads queries Audius without a paid plan or bundled key. Only artist-enabled, ungated downloads are shown. Save uses Wi-Fi; then open Settings > Download audio > Import completed audio. Direct permitted HTTPS audio links are also supported. Local playback is ad-free. Optional YouTube playback may contain provider ads.

Local player lyrics can be requested from LRCLIB. Synchronized lines are used only when supplied timestamps match the recording duration; otherwise plain or missing lyrics are clearly indicated.

## Direct GitHub APK delivery

Successful branch builds publish Saz-debug.apk as a Release asset and upload the individual APK to Actions with archive wrapping disabled. Download the APK, not GitHub's automatic source archives. Publishing requires the configured encrypted SAZ_PREVIEW_KEYSTORE repository secret. Preview certificate SHA-1: 35:74:D1:BA:27:54:A3:29:E4:E8:8F:51:89:4E:C4:89:C6:A3:33:90. This is public certificate identity, not private signing material. Pull requests are built and checked but are not published.

Live checks are opt-in: install app and test APKs, configure the YouTube key on the test device, and run LiveProviderSmokeTest with instrumentation argument -e liveProviders true. Never put keys in source or command arguments. See PROVIDER_TEST_RESULTS.md for current results, and PROVIDERS.md for account credentials still needed.
