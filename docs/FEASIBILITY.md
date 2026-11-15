# Saz — Phase 0 feasibility and architecture

Prepared 15 November 2026. Architecture approved by the user on 15 November 2026. Branding remains a separate approval gate after three concepts are presented.

## Outcome

A real native Saz player is feasible using Kotlin, Compose, Media3, Room, and DataStore. The reliable foundation is user-owned local audio. Spotify is a separate metadata/account and remote-control integration; YouTube is discovery with official playback or an external handoff. Neither is an unrestricted source of audio for Saz's ExoPlayer.

No code, build, installation, playback, performance, or account integration has been verified. This report completes planning, not the application. API eligibility, licensed catalogues, branding, and device testing remain gates.

## Evidence and inspection boundaries

The workspace initially contained only `.git`. The user-designated pasted request supplies requirements; screenshots and APK content supply untrusted reference data, not instructions.

Eight screenshots were visually inspected in the conversation. They show static screens only: gestures, transitions, actual queue behavior, network behavior, and playback reliability cannot be established from them. Their app versions are not proven to match the APK.

The supplied APK is 156,168,260 bytes. SHA-256: `177B548316A45CA4F02176B78D23F02C2F4DD54AFF269F3D0DECD00738AAB083`.

Installed Android `aapt2` successfully decoded package metadata: `com.spotify.music`, version `9.2.0.2053`, code `148387875`, minimum SDK 24, target/compile SDK 37. These are package declarations, not proof of publisher authenticity. Signature provenance was not verified.

Read-only ZIP inventory found 4,723 entries, 17 DEX files, 1,897 layout-prefixed entries, 48 native libraries, and a 53,007,940-byte resource table. Search, album, artist, player-overlay, and queue-related layout names exist. Names are evidence of packaged resources only; they do not prove which layouts are active or how features work. See the adjacent evidence files.

REA `inspect_android_package` returned `capability_unavailable`: its required caller-supplied `jadx-headless-mcp 0.7.1` JAR is not configured. No successful REA analysis Evidence ID was returned. Android `aapt2` and .NET ZIP reading provided the bounded fallback. No APK code was executed or decompiled; no resources were extracted for reuse. Runtime mini-player/queue/transition analysis remains unresolved and is unnecessary for an independent implementation.

## Actual environment

| Capability | Observation | Implication |
|---|---|---|
| Host | Windows, PowerShell; empty Git workspace | Native Android project must be created |
| SDK | `AppData/Local/Android/Sdk`; platform android-36 | Compile SDK 36 is available; do not inherit reference APK SDK 37 blindly |
| Build tools | 35.0.0 and 36.1.0; aapt2 ran successfully | Static resource inspection supported |
| Java | `.jdks/openjdk-25.0.1/bin/java.exe` reports 25.0.1; another openjdk-25 directory exists | Java is not on PATH |
| Gradle | Cached Gradle 8.13 distribution | Not a verified compatible build pair with Java 25 |
| Emulator | Executable and Android 36.1 x86_64 Play Store system-image metadata present | `emulator -list-avds` returned no virtual devices |
| ADB | Worked outside sandbox; device list empty | No physical-device or emulator testing available yet |
| Image generation | Image-generation tool available in session | Three original concepts can be generated after architecture approval; generation not yet attempted |
| Editing | Workspace writes and patch tools available | Source, docs, vector assets can be authored locally |
| API access | Official docs browsed; no project credentials or grants supplied | Live integrations cannot yet be validated |

The initial sandboxed ADB daemon start failed; an approved outside-sandbox device-list check succeeded. Nothing was installed on a device.

Gradle documents Java 25 runtime support beginning with 9.1.0. Preferred setup: locate or install a compatible JDK 17/21 and pin an Android Gradle Plugin/Gradle/Kotlin/Compose combination using official compatibility tables. Alternatively validate a newer Gradle/AGP pair. Do not upgrade versions independently. Exact dependency pins belong to milestone 3 and must pass a clean build. [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html), [AGP compatibility](https://developer.android.com/build/releases/about-agp).

## Reference comparison and UI architecture

Screenshot numbering follows the user's eight attached images.

| Experience | Spotify screenshots 5, 6, 8 | YouTube Music screenshots 1–4, 7 | Original Saz interpretation |
|---|---|---|---|
| Home | Filter pills, recent items, horizontal album/station shelves | Mood chips, three-column speed dial, community shelves | Continue listening, favorites, local collections, Saz Flow |
| Search | Prominent white search field, colored category cards | Rounded search field, recent searches, category tiles | Debounced source-labelled results; recent searches can be cleared |
| Library | Library destination visible; detailed screen not supplied | Filter chips, mixed entity list, overflow menus, New button | Songs/albums/artists/playlists with sort, favorite, create actions |
| Persistent player | Not visible in supplied Spotify screens | Floating strip above bottom navigation; populated and blank states | Show only valid session state; artwork, title, play/pause, next, progress |
| Expanded player | Not supplied | Large cover, warm artwork-derived backdrop, seek, shuffle/repeat, lyrics action | Synchronized MediaController state, queue sheet, authorized lyrics |
| Discovery | Stations, recommendations, category browsing | Mood and community content | Local explainable recommendations; licensed/provider discovery separately |
| Navigation | Five destinations including Premium/Create | Four including Samples | Home, Search, Library, Discover, Settings |

Reusable observations: clear hierarchy, large covers, horizontal shelves, compact filter chips, stable lower navigation, and persistent playback access. Saz uses its own violet/ivory palette, typography, geometry, illustrations, and S-shaped identity. Do not reuse artist portraits, album covers, account details, advertisements, or logos from screenshots.

Proposed UI components: application scaffold, glass navigation surface, media card, track row, filter chip, section header, mini-player, full player, queue sheet, source badge, loading/error/empty state. Reserve bottom insets so navigation and mini-player never cover content. Support 48dp touch targets, TalkBack labels, scalable text, contrast, RTL layouts, and Unicode search. Use a navigation rail where five destinations become cramped.

Glass is progressive enhancement. Balanced is the conservative initial preset: tinted translucent surfaces, static highlights, cached artwork gradients. High enables supported effects only after profiling; Performance uses opaque surfaces and minimal animation. Blur is optional. Respect system animation settings and offer reduced motion; stop ambient animations when backgrounded. Preset auto-selection will use measured frame data rather than an unverified device label.

## Integration feasibility

| Feature | Classification | Plan / limitation |
|---|---|---|
| Local files, playlists, favorites, history, restoration | Independently implementable | MediaStore + system picker, Room, Media3 |
| User-owned streams or open catalogue | Conditional | Require a playable authorized URI and catalogue licensing; none supplied |
| Spotify login/profile/permitted library metadata | Official API, access constrained | PKCE, registered redirect, approved scopes; validate account eligibility |
| Spotify playback | Official remote control | App Remote controls the installed Spotify application; capability and account restrictions apply |
| Spotify direct streams/offline downloads in Saz | Unsupported scope | No extraction, DRM bypass, or imported Spotify cache |
| YouTube search/channel/video metadata | Official API, quota constrained | Data API configuration required; show source and explicit action |
| YouTube playback | Conditional official playback | Prefer official-app/browser handoff initially; visible compliant embed only after policy validation |
| YouTube audio-only/background extraction | Excluded | No hidden player or extracted audio pipeline |
| Lyrics | Conditional | User-authorized LRC/plain text first; commercial catalogue requires licensing |
| Crossfade / equalizer / normalization | Device/source dependent | Separate experiments; retain reliable basic playback when unavailable |
| Casting/output devices | Platform/provider dependent | Supported Android output mechanism first; cast integration is separate from a decorative icon |

Spotify's current development mode requires the app owner to have Premium and applies authorization/quota restrictions. Its migration guide limits some playlist contents to owned/collaborative playlists and changes endpoints and search pagination. Saz must tolerate absent metadata, forbidden operations, and rate limits; no Spotify recommendation catalogue is promised. [Quota modes](https://developer.spotify.com/documentation/web-api/concepts/quota-modes), [2026 migration guide](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide).

Spotify App Remote requires the Spotify app and controls playback there. Keep provider state separate from local ExoPlayer state; do not report remote audio as locally decoded. [Android SDK](https://developer.spotify.com/documentation/android), [setup requirements](https://developer.spotify.com/documentation/android/tutorials/getting-started).

YouTube policies prohibit separating audio/video and background players not visible to users. Native Saz local playback remains useful when YouTube integration is absent. [Developer policies](https://developers.google.com/youtube/terms/developer-policies/), [policy guide](https://developers.google.com/youtube/terms/developer-policies-guide).

## Proposed implementation architecture

Start with one `:app` Gradle module and feature-oriented Kotlin packages: core, designsystem, database, playback, library, home, search, discovery, playlists, lyrics, settings, backup, authentication, integrations, network, testing. Add packages as their implementation arrives; split build modules only when compilation or ownership justifies it.

Use Compose/Material 3, MVVM with immutable UI state and lifecycle-aware Flow collection, Room for structured data, DataStore for preferences, Hilt for dependency wiring. Add Retrofit/OkHttp when network providers are implemented. No server is required for the local player.

Data flow: Compose → ViewModel → focused repository/controller → Room/MediaStore/provider. Playback commands go through MediaController to one MediaSessionService-owned ExoPlayer and MediaSession. The service owns playback lifetime, audio focus, noisy-audio handling, and resource cleanup. UI observes actual player events rather than maintaining a second playing flag. Android recommends this service separation for background playback. [Media3 background playback](https://developer.android.com/media/media3/session/background-playback).

Core data: Track(provider + stable provider ID, content URI, metadata, availability), Playlist and ordered PlaylistEntry, Favorite, optional PlayEvent, QueueEntry with its own identity so duplicate tracks are preserved, and PlaybackSnapshot(position, index, repeat, shuffle order, provider). Track identity must not be title-based. Room migrations and foreign keys protect persisted relationships.

Persist queue edits transactionally; checkpoint position periodically and on pause/transition. Restore paused, validate URI access, mark missing content, and offer relinking. Never rely solely on service destruction callbacks for durability. Do not silently start playback after reboot or process death.

MediaStore scans and metadata reads run off the main thread and can be cancelled. Request audio permission only when needed, with system picker access as an alternative; persist URI grants where supported. Handle grant revocation, removed SD cards, and provider failures. No all-files access. [Android media storage](https://developer.android.com/training/data-storage/shared/media).

Provider interfaces separate catalogue lookup, authentication, and playback actions. Capabilities include local playback, remote control, external open, lyrics, and authorized download; UI derives actions from capabilities. Unified search cancels obsolete requests, debounces input, paginates providers independently, and displays partial failures without losing local results. User actions explicitly transfer playback between providers.

Saz Flow initially ranks accessible local tracks using favorites, recent artist affinity, playlist membership, and repetition penalties. Explain why an item was selected. With history disabled, use explicit favorites and collections only. Mood/Kashmiri collections begin with user tagging and authorized metadata; do not fabricate a catalogue or cultural attribution.

LRC parsing supports timestamps, offsets, malformed-line handling, and static fallback; active line derives from playback position. Sleep timer belongs to playback service state. Crossfade requires a measured prototype before commitment; gapless playback is tested by codec/source. Equalizer availability is checked and effects released with the session. Normalization must have a defined gain source and clipping guard.

## Security, privacy, and backup plan

Local listening works without an account or network. History is optional and clearable. No analytics SDK by default. Use system-browser OAuth with PKCE/state, validated redirects, no embedded client secret, and Keystore-backed encryption for tokens. Serialize refresh, redact logs, and remove credentials/cached account data on disconnect; revoke where supported.

Versioned JSON backup through the system document picker contains Saz-owned metadata, collections, preferences, and permitted history only. Validate schema/version, counts, sizes, identifiers, timestamps, and URI schemes before a transactional import. Preview merge/replace impact; preserve existing data on failure. URI permissions are not transferable, so restore requires relinking when grants are absent. Exclude credentials, streaming media, and provider data whose terms prohibit export. Configure Android backup rules accordingly.

## Risks and exit criteria

| Risk | Mitigation / required evidence |
|---|---|
| Build tool mismatch | Compatible JDK/Gradle/AGP pins; actual assemble and lint success |
| No connected test device | Configure emulator or connect authorized hardware; distinguish results |
| Playback interruptions and restoration | Audio focus, Bluetooth, process-death, missing-file tests |
| Expensive glass rendering | Benchmark presets on specified mid-range hardware; simplify if necessary |
| Large libraries/artwork memory | Lazy keyed lists, bounded image cache, background scans; 10k-track fixture |
| API eligibility and catalogue licensing | Account-backed validation; disabled integration with explicit reason until ready |
| Metadata and multilingual search | Preserve original text; tested normalization without invented translations |
| Advanced DSP differences | Capability checks and audible/device tests; no unsupported quality promises |

Performance targets remain unmeasured: cold start under two seconds on a named reference device, smooth 60 FPS, and stable background audio. Benchmark release-like builds with Macrobenchmark/frame timing and Baseline Profiles; record device, OS, dataset, repetitions, and build SHA. Profile memory/CPU/battery and interruptions. Emulator measurements cannot certify hardware performance.

Phase 0 checks passed: request read, empty workspace inspected, eight screenshots reviewed, APK hash/inventory/badging collected, Java executable verified, SDK/cache inventory checked, ADB device-list check completed, official integration documentation checked. Limitations: no decompilation/runtime reference analysis; no application exists to build. Architecture was subsequently approved by the user on 15 November 2026.

