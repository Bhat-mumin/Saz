package app.saz.music

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.saz.music.integrations.MusicDestination
import app.saz.music.integrations.openMusicSearch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicHandoffTest {
    private class RecordingActivity(private val failCount: Int) : Activity() {
        val attempts = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            attempts.add(intent)
            if (attempts.size <= failCount) throw ActivityNotFoundException()
        }
    }

    @Test fun prefersProviderAppAndFallsBackToTheSameWebSearch() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        for (provider in MusicDestination.entries) {
            val installed = RecordingActivity(0)
            assertTrue(installed.openMusicSearch(provider, "Kashmir & folk"))
            assertEquals(1, installed.attempts.size)
            assertEquals(provider.packageName, installed.attempts.single().`package`)

            val missing = RecordingActivity(1)
            assertTrue(missing.openMusicSearch(provider, "Kashmir & folk"))
            assertEquals(2, missing.attempts.size)
            assertEquals(null, missing.attempts.last().`package`)
            assertEquals(missing.attempts.first().data, missing.attempts.last().data)
            assertEquals(Intent.ACTION_VIEW, missing.attempts.last().action)
            assertTrue(missing.attempts.last().hasCategory(Intent.CATEGORY_BROWSABLE))
        }
    }

    @Test fun reportsMissingHandlersAndSecurityRestrictions() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        assertFalse(RecordingActivity(2).openMusicSearch(MusicDestination.YouTube, ""))
        val restricted = object : Activity() {
            override fun startActivity(intent: Intent) { throw SecurityException() }
        }
        assertFalse(restricted.openMusicSearch(MusicDestination.Spotify, ""))
    }
}
