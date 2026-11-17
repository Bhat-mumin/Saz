package app.saz.music

import android.app.DownloadManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saz.music.database.LibraryDatabase
import app.saz.music.library.LibraryRepository
import app.saz.music.providers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in network checks, never required by the offline CI suite. No key belongs in this source. */
@RunWith(AndroidJUnit4::class)
class LiveProviderSmokeTest {
    @Test fun audiusSearchUsesFreePublicCatalogue() = runBlocking {
        enabled()
        val provider = AudiusProvider(ProviderHttp())
        val result = provider.search(SearchRequest("piano"))
        assertTrue("Live Audius search failed", result is ProviderResult.Success)
        val items = (result as ProviderResult.Success).value.items
        assertTrue("Expected an artist-enabled free download", items.isNotEmpty())
        assertTrue(provider.download(items.first().key) is ProviderResult.Success)
    }
    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun enabled() = assumeTrue(InstrumentationRegistry.getArguments().getString("liveProviders") == "true")

    @Test fun youtubeSearchAndMetadataWithConfiguredAndroidIdentity() = runBlocking {
        enabled()
        val context = context()
        val keys = ApiKeyStore(context)
        val input = File(context.filesDir, "provider-test-key")
        if (input.exists()) {
            try {
                val matches = Regex("AIza[0-9A-Za-z_-]{35}").findAll(input.readText()).map { it.value }.toSet()
                check(matches.size == 1) { "Expected one test credential" }
                keys.save(matches.single())
            } finally { input.delete() }
        }
        assumeTrue(keys.youtubeKey().isNotBlank())
        val provider = YouTubeProvider(ProviderHttp(), keys, AndroidClientIdentity(context))
        val metadata = provider.metadata(MediaKey(ProviderId.YOUTUBE, "M7lc1UVf-VE"))
        assertTrue("Live metadata failed: ${(metadata as? ProviderResult.Failure)?.problem?.kind}", metadata is ProviderResult.Success)
        val search = provider.search(SearchRequest("Kashmiri music", "IN"))
        assertTrue("Live search failed: ${(search as? ProviderResult.Failure)?.problem?.kind}", search is ProviderResult.Success)
        assertTrue((search as ProviderResult.Success).value.items.isNotEmpty())
    }

    @Test fun lrclibReturnsTimestampedLyricsForDocumentedExample() = runBlocking {
        enabled()
        val result = LrclibProvider(ProviderHttp()).find("I Want to Live", "Borislav Slavov", 233000)
        assertTrue("Live lyric lookup failed", result is ProviderResult.Success)
        assertTrue((result as ProviderResult.Success).value.synchronized)
        // Lyrics themselves are deliberately not printed or copied into test artifacts.
    }

    @Test fun authorizedSampleDownloadsAndImportsAsLocalAudio() = runBlocking {
        enabled()
        val context = context()
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        val downloads = AudioDownloads(context, LibraryRepository(context, db.library()))
        var id: Long? = null
        try {
            // Samplelib explicitly permits downloading these synthetic test samples without license restrictions.
            id = downloads.enqueueAuthorized(AuthorizedAudio("https://download.samplelib.com/mp3/sample-3s.mp3", "Download fixture", "mp3", "Saz test"), wifiOnly = false)
            var completed = false
            repeat(45) {
                if (!completed) {
                    val item = downloads.downloads().firstOrNull { it.id == id }
                    check(item?.status != DownloadManager.STATUS_FAILED) { "Sample download failed: ${item?.detail}" }
                    completed = item?.status == DownloadManager.STATUS_SUCCESSFUL
                    if (!completed) delay(1000)
                }
            }
            assertTrue("Sample download did not finish", completed)
            assertTrue(downloads.importCompleted() > 0)
            val provider = LocalMusicProvider(LibraryRepository(context, db.library()))
            val songs = (provider.library() as ProviderResult.Success).value
            assertTrue(songs.any { it.key.provider == ProviderId.LOCAL && it.durationMs > 0 })
            assertTrue(songs.any { it.title == "Download fixture" && it.artist == "Saz test" })
        } finally { id?.let { downloads.cancel(it) }; db.close() }
    }
}
