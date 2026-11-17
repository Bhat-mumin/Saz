package app.saz.music

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saz.music.providers.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ProviderContractTest {
    @Test fun youtubePlaylistLinkUsesPlaylistApiAndPreservesPagination() = runBlocking {
        val http = FakeHttp(HttpReply(200, """{"nextPageToken":"next","items":[{"snippet":{"resourceId":{"videoId":"M7lc1UVf-VE"},"title":"Fixture","videoOwnerChannelTitle":"Artist"}}]}"""))
        val result = youtube(http).search(SearchRequest("https://music.youtube.com/playlist?list=PL1234567890", pageToken = "page2")) as ProviderResult.Success
        assertEquals("PL1234567890", http.parameters["playlistId"])
        assertEquals("page2", http.parameters["pageToken"])
        assertEquals("Artist", result.value.items.single().artist)
        assertEquals("next", result.value.nextPageToken)
        assertNull(youtubePlaylistId("https://youtube.com.attacker.test/?list=PL1234567890"))
        assertNull(youtubePlaylistId("https://youtube.com/?list=../secret"))
    }
    @Test fun audiusFiltersGatesAndRechecksPermissionBeforeDownloading() = runBlocking {
        val free = """{"id":"abc12","title":"Fixture","duration":30,"user":{"name":"Artist"},"is_downloadable":true,"is_available":true,"is_download_gated":false,"is_stream_gated":false,"orig_filename":"song.wav","download":{"url":"https://example.org/song.wav"}}"""
        val gated = org.json.JSONObject(free).put("is_download_gated", true)
        val missing = org.json.JSONObject(free).apply { remove("is_download_gated") }
        assertFalse(downloadAllowed(gated))
        assertFalse(downloadAllowed(missing))
        val http = FakeHttp(HttpReply(200, """{"data":[$free,$gated]}"""))
        val provider = AudiusProvider(http)
        val page = (provider.search(SearchRequest("Fixture")) as ProviderResult.Success).value
        assertEquals(1, page.items.size)
        assertEquals(ProviderId.AUDIUS, page.items.single().key.provider)
        http.reply = HttpReply(200, """{"data":$gated}""")
        assertTrue(provider.download(page.items.single().key) is ProviderResult.Failure)
        http.reply = HttpReply(200, """{"data":$free}""")
        assertEquals("wav", (provider.download(page.items.single().key) as ProviderResult.Success).value.extension)
        assertTrue(provider.download(MediaKey(ProviderId.YOUTUBE, "abc12")) is ProviderResult.Failure)
        http.reply = HttpReply(429, "{}", 90)
        assertEquals(ProblemKind.QUOTA, (provider.search(SearchRequest("x")) as ProviderResult.Failure).problem.kind)
        val calls = http.calls
        provider.search(SearchRequest("y"))
        assertEquals(calls, http.calls)
    }
    @Test fun localProviderKeepsLibraryAndMetadataSeparateFromYouTube() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, app.saz.music.database.LibraryDatabase::class.java).build()
        try {
            val track = app.saz.music.database.Track("content://fixture/local", "Valley", "Test artist")
            db.library().insert(track)
            val local = LocalMusicProvider(app.saz.music.library.LibraryRepository(context, db.library()))
            assertEquals(1, (local.search(SearchRequest("valley")) as ProviderResult.Success).value.items.size)
            assertEquals("Valley", (local.metadata(MediaKey(ProviderId.LOCAL, track.uri)) as ProviderResult.Success).value.title)
            assertTrue(local.prepare(MediaKey(ProviderId.YOUTUBE, track.uri)) is ProviderResult.Failure)
            assertTrue(local.capabilities.background)
            assertEquals(PlaybackMode.LOCAL_AUDIO, local.capabilities.mode)
        } finally { db.close() }
    }
    private class FakeHttp(var reply: HttpReply) : JsonTransport {
        var calls = 0
        var parameters = emptyMap<String, String>()
        var failNetwork = false
        override suspend fun get(url: String, parameters: Map<String, String>, headers: Map<String, String>): HttpReply {
            calls++; this.parameters = parameters
            if (failNetwork) throw IOException("fixture network failure")
            return reply
        }
    }
    private fun youtube(http: JsonTransport, key: String = "fixture-key") = YouTubeProvider(http,
        object : ProviderCredentials { override suspend fun youtubeKey() = key },
        AndroidClientIdentity(InstrumentationRegistry.getInstrumentation().targetContext))

    @Test fun youtubeSearchParsesAndCachesWithoutMixingProviders() = runBlocking {
        val http = FakeHttp(HttpReply(200, """{"nextPageToken":"next","items":[{"id":{"videoId":"M7lc1UVf-VE"},"snippet":{"title":"A &amp; B","channelTitle":"Fixture channel","thumbnails":{"medium":{"url":"https://i.ytimg.com/example.jpg"}}}}]}"""))
        val provider = youtube(http)
        val request = SearchRequest("Kashmiri music", "IN", true)
        val first = provider.search(request) as ProviderResult.Success
        assertEquals(ProviderId.YOUTUBE, first.value.items.single().key.provider)
        assertEquals("A & B", first.value.items.single().title)
        assertEquals("next", first.value.nextPageToken)
        assertEquals("true", http.parameters["videoEmbeddable"])
        assertEquals("date", http.parameters["order"])
        provider.search(request)
        assertEquals(1, http.calls)
        assertTrue(provider.library() is ProviderResult.Failure)
        assertFalse(provider.capabilities.background)
        assertFalse(provider.capabilities.downloadable)
    }
    @Test fun youtubeDistinguishesCredentialsQuotaNetworkAndMissingItems() = runBlocking {
        val http = FakeHttp(HttpReply(403, """{"error":{"errors":[{"reason":"quotaExceeded"}]}}""", 90))
        val provider = youtube(http)
        assertEquals(ProblemKind.CONFIGURATION, (youtube(http, "").search(SearchRequest("x")) as ProviderResult.Failure).problem.kind)
        assertEquals(0, http.calls)
        assertEquals(ProblemKind.QUOTA, (provider.search(SearchRequest("x")) as ProviderResult.Failure).problem.kind)
        provider.search(SearchRequest("y")); assertEquals(1, http.calls)
        http.reply = HttpReply(403, "{}")
        assertEquals(ProblemKind.AUTHENTICATION, (youtube(http).search(SearchRequest("x")) as ProviderResult.Failure).problem.kind)
        http.failNetwork = true
        assertEquals(ProblemKind.NETWORK, (youtube(http).search(SearchRequest("x")) as ProviderResult.Failure).problem.kind)
        http.failNetwork = false; http.reply = HttpReply(200, """{"items":[]}""")
        assertEquals(ProblemKind.NOT_FOUND, (youtube(http).metadata(MediaKey(ProviderId.YOUTUBE, "M7lc1UVf-VE")) as ProviderResult.Failure).problem.kind)
    }
    @Test fun youtubeMetadataRejectsNonEmbeddableAndWrongProvider() = runBlocking {
        val http = FakeHttp(HttpReply(200, """{"items":[{"snippet":{"title":"Fixture","channelTitle":"Channel"},"contentDetails":{"duration":"PT3M30S"},"status":{"embeddable":false}}]}"""))
        val provider = youtube(http)
        val key = MediaKey(ProviderId.YOUTUBE, "M7lc1UVf-VE")
        assertEquals(210000L, (provider.metadata(key) as ProviderResult.Success).value.durationMs)
        assertEquals(ProblemKind.UNAVAILABLE, (provider.prepare(key) as ProviderResult.Failure).problem.kind)
        assertEquals(ProblemKind.INVALID_REQUEST, (provider.prepare(key.copy(provider = ProviderId.LOCAL)) as ProviderResult.Failure).problem.kind)
    }
    @Test fun lyricsUseRealTimestampsAndRejectMismatchedRecordings() = runBlocking {
        val http = FakeHttp(HttpReply(200, """{"id":1,"duration":30,"syncedLyrics":"[00:01.00]Fixture line","plainLyrics":"Fixture line"}"""))
        val provider = LrclibProvider(http)
        val match = (provider.find("Fixture", "Test artist", 30000) as ProviderResult.Success).value
        assertTrue(match.synchronized)
        provider.find("Fixture", "Test artist", 30000); assertEquals(1, http.calls)
        assertEquals(ProblemKind.NOT_FOUND, (provider.find("Other", "Test artist", 90000) as ProviderResult.Failure).problem.kind)
        http.reply = HttpReply(200, """{"id":2,"duration":30,"syncedLyrics":null,"plainLyrics":"Static fixture"}""")
        assertFalse((LrclibProvider(http).find("Static", "Test artist", 30000) as ProviderResult.Success).value.synchronized)
        http.reply = HttpReply(429, "not json", 60)
        val limited = LrclibProvider(http)
        assertEquals(ProblemKind.QUOTA, (limited.find("Fixture", "Test artist", 30000) as ProviderResult.Failure).problem.kind)
        val calls = http.calls
        limited.find("Fixture", "Test artist", 30000); assertEquals(calls, http.calls)
    }
}
