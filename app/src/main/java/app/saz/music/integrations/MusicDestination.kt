package app.saz.music.integrations

import java.net.URLEncoder

enum class MusicDestination(val displayName: String, val packageName: String) {
    Spotify("Spotify", "com.spotify.music"),
    YouTube("YouTube", "com.google.android.youtube");

    fun searchUrl(query: String): String {
        val term = query.trim()
        val encoded = URLEncoder.encode(term, "UTF-8").replace("+", "%20")
        return when (this) {
            Spotify -> if (term.isEmpty()) "https://open.spotify.com/search" else "https://open.spotify.com/search/$encoded"
            YouTube -> if (term.isEmpty()) "https://www.youtube.com/" else "https://www.youtube.com/results?search_query=$encoded"
        }
    }
}
