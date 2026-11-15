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

The UI uses original scenic art, translucent violet surfaces and platform blur in the full player and lyrics. Performance mode omits those blur effects. Automatic graphics calibration and measured frame-rate targets remain unverified.

## Release configuration

The release variant enables R8 and resource shrinking. It is intentionally unsigned until the owner supplies a signing key. Do not distribute a debug-signed APK as a production release or commit signing material. Configure signing outside tracked source using the Android Studio Generate Signed App Bundle/APK workflow or a private Gradle init script. The application has not been published.

## External integrations

No API keys or accounts are included. Spotify/YouTube are explicitly unavailable in this local-player build, not simulated.

- Spotify requires an eligible developer application, registered redirect URI, authorized users/scopes, PKCE implementation and installed Spotify for App Remote. Validate the current developer-mode limitations against the account before implementation. Saz does not extract Spotify audio.
- YouTube discovery requires an API project and appropriately restricted API configuration. Playback must use an approved visible player or official-app handoff; no background audio extraction.
- Licensed music and lyrics providers require authorization and attribution terms. Local user-supplied music/LRC files work without these services.

## Test boundaries

JVM tests exercise LRC parsing. Android tests create their own PCM WAV audio and use a real MediaController/ExoPlayer session; library tests verify Room merge behavior and backup rejection. They do not require a streaming account. Emulator results do not establish physical-device audio quality, Bluetooth behavior, battery life or 60 FPS performance.

