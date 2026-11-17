# Walnut design system

Approved by the user on 15 November 2026 after comparing Walnut, Glacier and Afterglow.
See design-exploration/README.md for the original proposals and generated previews.

## Identity and foundations

A listening journal informed by walnut craftsmanship: mineral surfaces, walnut ink,
plum controls, literary headings and practical music lists. Avoid scenic backgrounds,
luminous borders and decorative blur. User album artwork keeps its own identity.
The existing launcher icon remains unchanged; the in-app saz wordmark uses Literata.

Source of truth: app/src/main/java/app/saz/music/designsystem/WalnutTheme.kt.

| Role | Value |
|---|---|
| Background | #F0EEE8 mineral |
| Primary text | #352B28 walnut |
| Secondary text | #625451 |
| Primary action | #703D57 plum, white text |
| Supporting surface | #E5DADD pale mauve |
| Panel | #FAF8F3 paper |
| Divider | #C9BABD |
| Error | #A42B32 |

Calculated contrast: primary/background 11.85:1, secondary/background 6.23:1,
secondary/mauve 5.30:1, white/plum 8.42:1, plum/background 7.26:1.
These checks cover the listed opaque color pairs, not every rendered state.

Bundled Literata medium (display, headings, player titles) and Source Sans 3
(controls, lists, metadata). Original variable fonts from google/fonts; SIL OFL
licenses are retained in docs/licenses. Android handles fallback for scripts outside
these fonts. Kashmiri shaping, RTL and full language coverage require device review.

Display 38/44sp; headline 26/33sp; section 22/29sp; body 17/25sp and 15/21sp;
supporting text 13/18sp. Use scalable sp. 24dp gutters, 8dp base rhythm, 8dp artwork
corners, 12dp panels, circular playback controls and 48dp minimum touch targets.
Large titles may wrap; track rows ellipsize while the player shows up to two lines.

## Composition and interaction

Home has a typographic opening, one original abstract Flow artwork and music rows.
Mood collections are quiet text rows below music. Four bottom destinations keep
navigation compact; Settings has a visible top action. The mini-player is integrated
above navigation. The player uses square artwork and opaque text/control surfaces.
Lyrics, queue, favorites, timer, speed, backup, import and collections remain accessible.

The full player enters with a 240ms opacity transition; reduced motion skips that
transition. Existing Material press/selection/sheet feedback is retained. This build
does not implement the concept's shared-artwork expansion. Quiet listening replaces
busy scenery with typography; lyrics retain follow/manual and text-size controls.

This approved iteration is a light theme. A separately designed dark variant is not
implemented. Existing graphics preferences are retained for stored-setting compatibility;
both now use opaque rendering without real-time blur.

## States and performance

Existing library import, loading indicator, playback errors, permission handling,
empty collections, playback restoration, confirmation dialogs and snackbar feedback
remain. Search distinguishes local results from external provider actions. Flow
waits for playback connectivity and offers import when the library is empty.

Coil loads artwork asynchronously; supplied covers use Fit rather than forced cropping.
Lists remain lazy. Original fallback art was generated for Saz, not licensed catalogue
content. No continuously animated background, refraction or blur remains. The existing
500ms playback-state update architecture remains; no 60 FPS or battery claim is made.

See WALNUT_REVIEW.md for executed checks and outstanding review limits.
