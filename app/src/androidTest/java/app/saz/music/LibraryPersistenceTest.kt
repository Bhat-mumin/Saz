package app.saz.music

import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saz.music.backup.BackupRepository
import app.saz.music.database.*
import app.saz.music.settings.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryPersistenceTest {
    @Test fun rescanKeepsFavoritesAndInvalidBackupDoesNotMutate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "saz-test-backup.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
        })!!
        try {
            val dao = db.library()
            val track = Track("content://media/external/audio/media/123", "Original", favorite = true)
            dao.insert(track)
            dao.merge(listOf(track.copy(title = "Updated", favorite = false)))
            assertTrue(dao.track(track.uri)!!.favorite)
            assertEquals("Updated", dao.track(track.uri)!!.title)
            val playlist = dao.create(Playlist(name = "Night Drive"))
            dao.add(PlaylistEntry(playlist, track.uri))
            val backup = BackupRepository(context, db, Preferences(context))
            backup.export(uri)
            assertEquals(1, backup.restore(uri))
            assertEquals(1, dao.allTracks().size)
            assertEquals(2, dao.allPlaylists().size)
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write("{\"format\":\"saz-backup\",\"version\":99}".toByteArray()) }
            try { backup.restore(uri); fail("Unknown versions must be rejected") } catch (_: IllegalArgumentException) { }
            assertEquals(1, dao.allTracks().size)
            assertEquals(2, dao.allPlaylists().size)
        } finally { context.contentResolver.delete(uri, null, null); db.close() }
    }
}
