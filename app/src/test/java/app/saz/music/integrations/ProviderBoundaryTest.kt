package app.saz.music.integrations

import app.saz.music.providers.*
import org.junit.Assert.*
import org.junit.Test

class ProviderBoundaryTest {
    @Test fun downloadsRequireDirectHttpsAudioAndRejectProviderStreams() {
        assertEquals("example.org", validateAudioDownload("https://example.org/music/song.mp3?token=abc").host)
        listOf("http://example.org/song.mp3", "https://user:password@example.org/song.mp3", "https://www.youtube.com/song.mp3",
            "https://r1.googlevideo.com/song.m4a", "https://open.spotify.com/song.mp3", "https://example.org/watch?v=abc",
            "file:///sdcard/song.mp3", "javascript:alert(1)").forEach { input ->
            try { validateAudioDownload(input); fail("Accepted unsafe or unsupported input") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun iframeUsesOfficialVisiblePlayerAndRejectsScriptInjection() {
        val html = youtubePlayerHtml("M7lc1UVf-VE", "app.saz.music")
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertTrue(html.contains("controls:1,autoplay:0"))
        assertTrue(html.contains("origin:'https://app.saz.music'"))
        assertFalse(html.contains("mute:"))
        try { youtubePlayerHtml("';alert(1);//", "app.saz.music"); fail("Unsafe ID accepted") } catch (_: IllegalArgumentException) { }
        assertTrue(playerError(153).contains("identity"))
        assertTrue(playerError(150).contains("embedded"))
    }
    @Test fun retryAfterSupportsSecondsAndDates() {
        assertEquals(60L, retryAfter("60"))
        assertEquals(60L, retryAfter("Thu, 01 Jan 1970 00:01:00 GMT", 0))
        assertNull(retryAfter("not a duration"))
    }
    @Test fun providerIdentifiersAreDistinct() {
        assertNotEquals(MediaKey(ProviderId.LOCAL, "same"), MediaKey(ProviderId.YOUTUBE, "same"))
    }
}
