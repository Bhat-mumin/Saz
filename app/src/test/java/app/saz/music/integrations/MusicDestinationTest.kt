package app.saz.music.integrations

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicDestinationTest {
    @Test fun preservesUnicodeAndReservedCharactersWithoutChangingDestination() {
        val query = "کٲشُر / music?x=1&y=2+#"
        for (destination in MusicDestination.entries) {
            val uri = URI(destination.searchUrl("  $query  "))
            assertEquals("https", uri.scheme)
            assertEquals(null, uri.fragment)
            when (destination) {
                MusicDestination.Spotify -> {
                    assertEquals("open.spotify.com", uri.host)
                    assertEquals(null, uri.rawQuery)
                    assertEquals(query, URLDecoder.decode(uri.rawPath.removePrefix("/search/"), "UTF-8"))
                }
                MusicDestination.YouTube -> {
                    assertEquals("www.youtube.com", uri.host)
                    assertEquals(query, URLDecoder.decode(uri.rawQuery.removePrefix("search_query="), "UTF-8"))
                }
            }
        }
    }

    @Test fun blankQueriesOpenProviderLandingPages() {
        assertEquals("https://open.spotify.com/search", MusicDestination.Spotify.searchUrl(" \n "))
        assertEquals("https://www.youtube.com/", MusicDestination.YouTube.searchUrl(""))
    }
}
