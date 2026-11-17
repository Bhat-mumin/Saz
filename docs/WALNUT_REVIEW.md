# Walnut visual review — 17 November 2026

Approved direction: Walnut, selected by the owner after three distinct concept boards. The native implementation uses mineral surfaces, walnut text, plum actions, Literata headings and Source Sans 3 reading text. Existing launcher branding is retained. Generated concept boards are not screenshots.

Actual installed-app captures were inspected at 720 x 1600 pixels (about 411 x 914dp at density 280) and 630 x 1120 pixels (360 x 640dp), including 130% system text scaling.

| Screen | Evidence and observed result |
|---|---|
| Home | [Normal](screenshots/walnut/home-final.png), [compact/large text](screenshots/walnut/home-compact-final.png): editorial title, adaptive art size, Flow action and persistent navigation remain readable |
| Search | [Normal](screenshots/walnut/search-final.png), [compact/large text](screenshots/walnut/search-compact-final.png): provider choices wrap rather than clipping; long track titles ellipsize |
| Local player | [Normal](screenshots/walnut/player-final.png), [compact/large text](screenshots/walnut/player-compact-final.png): track, seek and transport controls fit; secondary controls remain scrollable on short screens |
| Free downloads | [Live Audius results](screenshots/walnut/free-results-final.png): source attribution, artist/title and Save action remain distinct |
| YouTube discovery | [Live results](screenshots/walnut/youtube-results-final.png): visible provider attribution, artwork and metadata |
| YouTube playback | [Cued player](screenshots/walnut/youtube-player-final.png), [playing](screenshots/walnut/youtube-playing-final.png): official controls/branding remain visible, separate from native audio controls |

Review refinements: adaptive artwork sizing on short screens; lighter seek-track proportions; wrapping source controls; opaque surfaces; reduced-motion-aware transitions; correction of zero-height embedded iframe through native MATCH_PARENT sizing and viewport-based CSS. IFrame runtime inspection observed a 379 x 213 CSS-pixel viewport and YouTube state 1 after tapping Play. No diagnostic DOM modification is required by the final APK.

The emulator host GPU produced black WebView content even after dimensions were corrected. SwiftShader rendered the same final APK visibly. System UI also intermittently hung after emulator configuration changes; captures with those dialogs are excluded. This environment cannot certify GPU compatibility, frame rate, startup time or battery life on real hardware.

Text contrast calculations against the main surface: primary text 11.85:1, secondary text 6.23:1, plum action 7.26:1. No claim of a complete accessibility audit is made. Tablet, landscape, RTL, TalkBack, all font sizes and physical-device audio/performance testing remain outstanding. Current design is light-only; no automatic artwork-derived palette or shared-artwork transition is claimed.

Test-track names represent locally generated audio fixtures, not bundled catalogue music. Online results and thumbnails belong to their attributed providers. Earlier/failed captures are retained locally outside the published screenshot set.
