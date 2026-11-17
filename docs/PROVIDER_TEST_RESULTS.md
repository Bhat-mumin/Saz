# Provider verification — 17 November 2026

Tested native debug build on the local Android emulator. No production or physical-device certification is claimed.

| Check | Observed result |
|---|---|
| assembleDebug + assembleDebugAndroidTest | Passed |
| JVM tests | 8 passed: LRC parsing, external-search encoding, download/embed boundaries, retry handling |
| Offline Android suite | 11 passed: provider contracts including public playlists/Audius gating, local library, handoff fallback, Media3 playback, Room persistence |
| Opt-in live Android suite | 4 passed: YouTube public search/metadata with supplied Android-restricted key; LRCLIB timestamped response; Audius free-search/download authorization; permitted sample download/import with title/artist preservation |
| lintDebug | 0 errors, 32 warnings |
| APK signature | Android Debug v2 signature verified; see APK_REVIEW.md |

Evidence: [build](evidence/providers-build.txt), [offline instrumentation](evidence/providers-offline.txt), [live instrumentation](evidence/providers-live.txt). The live suite does not print credentials or lyric text. Test audio is a permitted synthetic sample, not a copied commercial recording.

An initial live run failed while the emulator was in airplane mode; enabling networking resolved it. The first Audius live test exposed a metadata object/array mismatch, which was fixed and covered by the final tests. These failed runs were not counted as success.

Lint warnings include dependency updates, optional KTX migration, KAPT migration, unused earlier scenic resources, the exported Media3 service, and backup-version guidance. No dependency upgrade or broad architecture rewrite was introduced as part of this change. Backup is disabled in the manifest. See source and lint report for exact warnings.

Limitations: live Audius authorization was checked, while full file download/import used the permitted Samplelib fixture. Private YouTube/Spotify account OAuth, private playlists, listening-history personalization, full catalogue coverage, all-recording lyric alignment, physical Bluetooth/audio focus, battery use and 60 FPS targets remain unverified or unimplemented. Optional embedded YouTube playback can contain YouTube ads; local playback has no Saz ads. Public-playlist pagination is fixture-tested, not a guarantee that every playlist/video remains available.

Manual IFrame review caught a zero-height CSS iframe despite a ready callback. Native WebView MATCH_PARENT layout parameters and viewport-sized iframe CSS fixed the layout. The final native emulator screenshot shows the official YouTube poster, play control and attribution unobscured. Host GPU rendering remained black; the same APK rendered correctly after restarting this AVD with SwiftShader. This is an emulator-renderer limitation, not evidence that all device GPUs pass.

GitHub workflow syntax was validated with actionlint v1.7.12. No workflow validation errors were reported.

The final installed APK was retested on 17 November after a fresh emulator boot: 11 offline Android tests and 4 opt-in live provider tests passed. The prior overnight emulator session had lost ActivityManager; its failed test-launch attempts were infrastructure failures and were not counted as passing tests.

Manual YouTube playback reached official player state 1 after a tap, with native screenshots showing the video and controls. After pressing Android Home, runtime inspection returned an empty body, zero iframes and no player object. This verifies the playable document is removed when hidden.
