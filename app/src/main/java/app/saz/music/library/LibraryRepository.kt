package app.saz.music.library

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import app.saz.music.database.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class LibraryRepository @Inject constructor(@ApplicationContext private val context: Context, val dao: LibraryDao) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        context.contentResolver.query(base, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                ensureActive()
                tracks += Track(ContentUris.withAppendedId(base, c.getLong(0)).toString(), c.getString(1) ?: "Untitled",
                    c.getString(2)?.takeUnless { it == "<unknown>" } ?: "Unknown artist", c.getString(3) ?: "Unknown album",
                    c.getLong(4), ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.getLong(5)).toString())
            }
        } ?: error("The device media library is unavailable. Try importing files instead.")
        dao.merge(tracks)
        tracks.size
    }

    suspend fun import(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            ensureActive()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: DocumentFile.fromSingleUri(context, uri)?.name ?: "Untitled"
                dao.merge(listOf(Track(uri.toString(), title,
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist",
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown album",
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0)))
                count++
            } catch (_: IllegalArgumentException) { /* Unsupported files are counted in the import summary. */ }
            catch (_: SecurityException) { /* A revoked document grant does not abort the remaining files. */ }
            finally { retriever.release() }
        }
        count
    }

    suspend fun importFolder(uri: Uri): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, uri) ?: error("Cannot open folder")
        val pending = ArrayDeque<DocumentFile>().apply { add(root) }
        val files = mutableListOf<Uri>()
        val visited = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            ensureActive()
            val dir = pending.removeFirst()
            if (!visited.add(dir.uri.toString())) continue
            require(visited.size <= 10000 && files.size <= 50000) { "Folder is too large. Select a smaller music folder." }
            dir.listFiles().forEach { child ->
                if (child.isDirectory) pending.add(child)
                else if (child.type?.startsWith("audio/") == true) files.add(child.uri)
            }
        }
        import(files)
    }
}
