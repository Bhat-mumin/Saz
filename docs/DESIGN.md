# Violet glass visual direction

The user's final UI reference supplies the visual target: compact sans-serif screen hierarchy, floating translucent purple controls, fine lilac highlight borders, scenic Kashmir-inspired artwork, mood tiles, and an immersive player/lyrics experience. The reference is a presentation image, not evidence of implemented functionality.

Native implementation uses Compose surfaces, gradients, rounded clipping, subtle multicolor border strokes, asynchronously decoded/cached original illustrations, and platform blur for player backgrounds where supported. Performance mode omits player/lyrics blur. No continuously animated refraction shader is used.

The approved instrument-S glass tile is retained. Illustrations in `branding/scenes/README.md` were generated with the built-in image-generation tool. They are fictional decorative environments, not licensed recordings, artist photos or exact landmark claims. Album art from the user's library takes priority where available.

Implemented layouts:

- Home: time-aware greeting, Saz Flow glass card, two-column mood cards, real recents/favorites and local library.
- Search: search field and honest source labels; local results only until integrations exist.
- Library: songs/favorites/albums/artists/playlists; persistent actions and import controls.
- Discover: scenic regional collection card, local Flow and user-created collection chips.
- Mini-player: actual track metadata, play/pause/next and progress in a luminous floating surface.
- Full player: artwork, softly blurred scenery, seek and real playback controls, glass action strip.
- Lyrics: full-screen Song/Lyrics selector, user-imported content, active line/follow mode, size control, seek and playback.
- Ambient: scenic calm illustration and reduced controls.

Compared with the reference, source accounts, catalogue songs, equalizer and crossfade are not represented as working features. No fake users, biographies or copyrighted lyrics were inserted to fill screens. Actual device screenshots are recorded separately from the reference and image-generation previews.
