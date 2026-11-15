package app.saz.music.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val durationMs: Long = 0,
    val artworkUri: String? = null,
    val favorite: Boolean = false,
    val lastPlayed: Long = 0,
)

@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "playlist_entries", primaryKeys = ["playlistId", "trackUri"],
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Track::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trackUri")])
data class PlaylistEntry(val playlistId: Long, val trackUri: String, val added: Long = System.currentTimeMillis())

@Dao
abstract class LibraryDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE") abstract fun tracks(): Flow<List<Track>>
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") abstract fun playlists(): Flow<List<Playlist>>
    @Query("SELECT * FROM playlist_entries ORDER BY added") abstract fun entries(): Flow<List<PlaylistEntry>>
    @Query("SELECT * FROM tracks WHERE uri = :uri") abstract suspend fun track(uri: String): Track?
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insert(track: Track)
    @Query("UPDATE tracks SET title=:title, artist=:artist, album=:album, durationMs=:duration, artworkUri=:art WHERE uri=:uri")
    abstract suspend fun updateMetadata(uri: String, title: String, artist: String, album: String, duration: Long, art: String?)
    @Transaction open suspend fun merge(tracks: List<Track>) {
        tracks.forEach { insert(it); updateMetadata(it.uri, it.title, it.artist, it.album, it.durationMs, it.artworkUri) }
    }
    @Query("UPDATE tracks SET favorite = NOT favorite WHERE uri=:uri") abstract suspend fun favorite(uri: String)
    @Query("UPDATE tracks SET lastPlayed=:time WHERE uri=:uri") abstract suspend fun played(uri: String, time: Long)
    @Query("UPDATE tracks SET lastPlayed=0") abstract suspend fun clearHistory()
    @Insert abstract suspend fun create(playlist: Playlist): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun add(entry: PlaylistEntry)
    @Query("DELETE FROM playlist_entries WHERE playlistId=:id AND trackUri=:uri") abstract suspend fun remove(id: Long, uri: String)
    @Query("DELETE FROM playlists WHERE id=:id") abstract suspend fun deletePlaylist(id: Long)
    @Query("UPDATE playlists SET name=:name WHERE id=:id") abstract suspend fun renamePlaylist(id: Long, name: String)
    @Query("SELECT * FROM tracks") abstract suspend fun allTracks(): List<Track>
    @Query("SELECT * FROM playlists") abstract suspend fun allPlaylists(): List<Playlist>
    @Query("SELECT * FROM playlist_entries ORDER BY added") abstract suspend fun allEntries(): List<PlaylistEntry>
    @Query("UPDATE tracks SET favorite=:favorite, lastPlayed=:lastPlayed WHERE uri=:uri")
    abstract suspend fun restoreFlags(uri: String, favorite: Boolean, lastPlayed: Long)
}

@Database(entities = [Track::class, Playlist::class, PlaylistEntry::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() { abstract fun library(): LibraryDao }
