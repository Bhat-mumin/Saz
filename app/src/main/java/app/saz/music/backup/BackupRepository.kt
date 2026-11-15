package app.saz.music.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import app.saz.music.database.*
import app.saz.music.settings.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class BackupRepository @Inject constructor(@ApplicationContext private val context: Context,
    private val database: LibraryDatabase, private val preferences: Preferences) {
    private val dao get() = database.library()
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val settings = preferences.settings.first()
        val root = database.withTransaction {
            JSONObject().put("format", "saz-backup").put("version", 1)
                .put("tracks", JSONArray().apply { dao.allTracks().forEach { t -> put(JSONObject()
                    .put("uri", t.uri).put("title", t.title).put("artist", t.artist).put("album", t.album)
                    .put("duration", t.durationMs).put("favorite", t.favorite).put("lastPlayed", if (settings.history) t.lastPlayed else 0)) } })
                .put("playlists", JSONArray().apply { dao.allPlaylists().forEach { p -> put(JSONObject().put("id", p.id).put("name", p.name)) } })
                .put("entries", JSONArray().apply { dao.allEntries().forEach { e -> put(JSONObject().put("playlist", e.playlistId).put("uri", e.trackUri).put("added", e.added)) } })
        }
        // Preferences and provider credentials are deliberately excluded in format v1.
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString(2).toByteArray()) }
            ?: error("Could not open backup destination")
    }
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 10 * 1024 * 1024) { "Backup exceeds 10 MB limit" }
                output.write(buffer, 0, count)
            }
            output.toByteArray().toString(Charsets.UTF_8)
        } ?: error("Cannot read backup")
        val root = JSONObject(text)
        require(root.optString("format") == "saz-backup" && root.optInt("version") == 1) { "Unsupported Saz backup format" }
        val tracksJson = root.getJSONArray("tracks")
        val playlistsJson = root.getJSONArray("playlists")
        val entriesJson = root.getJSONArray("entries")
        require(tracksJson.length() <= 50000 && playlistsJson.length() <= 1000 && entriesJson.length() <= 100000) { "Backup is too large" }
        fun JSONObject.bounded(key: String): String = getString(key).also { require(it.length <= 4096) { "Invalid backup field" } }
        val tracks = (0 until tracksJson.length()).map { i ->
            val t = tracksJson.getJSONObject(i)
            val trackUri = t.bounded("uri")
            require(Uri.parse(trackUri).scheme == "content") { "Backup contains a non-local URI" }
            val duration = t.getLong("duration"); val played = t.getLong("lastPlayed")
            require(duration >= 0 && played >= 0) { "Invalid track times" }
            Track(trackUri, t.bounded("title"), t.bounded("artist"), t.bounded("album"), duration,
                favorite = t.getBoolean("favorite"), lastPlayed = played)
        }
        val lists = (0 until playlistsJson.length()).map { i ->
            val p = playlistsJson.getJSONObject(i)
            Playlist(p.getLong("id"), p.bounded("name").also { require(it.isNotBlank() && it.length <= 100) })
        }
        require(tracks.map { it.uri }.distinct().size == tracks.size && lists.map { it.id }.distinct().size == lists.size) { "Duplicate backup identifiers" }
        val trackIds = tracks.map { it.uri }.toSet(); val listIds = lists.map { it.id }.toSet()
        val entries = (0 until entriesJson.length()).map { i ->
            val e = entriesJson.getJSONObject(i)
            PlaylistEntry(e.getLong("playlist"), e.bounded("uri"), e.getLong("added")).also {
                require(it.trackUri in trackIds && it.playlistId in listIds && it.added >= 0) { "Broken playlist entry" }
            }
        }
        val keepHistory = preferences.settings.first().history
        database.withTransaction {
            // Merge preserves existing tracks and favorites; imported playlists get new identities.
            tracks.forEach { track ->
                val old = dao.track(track.uri)
                dao.insert(track.copy(lastPlayed = if (keepHistory) track.lastPlayed else 0))
                if (old != null) dao.restoreFlags(track.uri, old.favorite || track.favorite, if (keepHistory) maxOf(old.lastPlayed, track.lastPlayed) else 0)
            }
            val newIds = lists.associate { it.id to dao.create(Playlist(name = it.name)) }
            entries.forEach { dao.add(it.copy(playlistId = newIds.getValue(it.playlistId))) }
        }
        tracks.size
    }
}
