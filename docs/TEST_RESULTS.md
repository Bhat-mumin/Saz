# Verification — Saz 0.1.0

Environment: Windows host (8 GB RAM), Temurin 21.0.13, Gradle 8.13, Android 36.1 x86_64 isolated emulator, display override 720 × 1600 / density 280. This is emulator evidence, not physical-device certification.

## Executed checks

- Debug assemble, JVM tests and Android lint: final command/result in `evidence/build-final.log`; lint report in `evidence/lint-results-debug.txt`.
- Two JVM LRC tests cover timestamp parsing, multiple timestamps, fractions and offsets. XML result is included in evidence.
- Two Android instrumentation tests passed (`evidence/instrumentation-final.txt`): actual PCM audio decoding with advancing playback position, pause, seek, next and queue removal; Room metadata merge preserving flags; backup round-trip merge and unknown-version rejection.
- Instrumentation was executed on the visual build before the final status-bar contrast and singular-song wording changes. Those final changes are covered by the final assemble/JVM/lint run and manual launch.
- Manual MediaStore scan found an original generated 30-second PCM test tone. It is an emulator fixture and is not bundled in the APK.
- Manual Home-button background check recorded PLAYING state at 3,629 ms with the original PCM fixture (`evidence/background-playback.txt`); media-button pause was exercised.
- Manual force-stop/relaunch restored one queued track at 29,999 ms in PAUSED state (`evidence/restored-session.txt`).
- Screenshots in `screenshots` are captures of the actual emulator app, not the supplied concept poster. Artwork without a local album cover is original decorative Saz scenery.

## Limits and observed environment issues

The first emulator run suffered System UI/startup ANRs while Google services updated and build processes competed for host memory. Subsequent tests passed with compilation finished first, host GPU rendering, Vulkan disabled, and unrelated Google apps disabled in this isolated AVD. Initial failure output and early screenshots are retained in evidence. A measured debug cold start of 4,366 ms does not meet the requested 2-second target; this loaded emulator is not a production benchmark.

Bluetooth, headset loss, real incoming-call interruption, battery, frame timing, long sessions, large libraries, rotation/font-scale/TalkBack/RTL matrix, multiple physical devices and release signing have not been accepted. Streaming providers are unimplemented and untested. Lint warnings remain; zero lint errors is not equivalent to full production QA.

