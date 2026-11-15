package app.saz.music

import android.content.ComponentName
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saz.music.playback.PlaybackService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class PlaybackSmokeTest {
    @Test fun decodesLocalAudioAndControlsQueue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "Saz test tone.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SazTest")
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
        val sampleRate = 44100
        val samples = sampleRate * 12
        val wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { wav.putShort((sin(2 * Math.PI * 440 * it / sampleRate) * 1500).toInt().toShort()) }
        resolver.openOutputStream(uri)!!.use { it.write(wav.array()) }
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future!!.get(20, TimeUnit.SECONDS)
        try {
            // Foreground activity permits user-initiated media playback on current Android versions.
            context.startActivity(android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1000)
            instrumentation.runOnMainSync {
                val item = MediaItem.Builder().setMediaId(uri.toString()).setUri(uri).build()
                controller.setMediaItems(listOf(item, item))
                controller.prepare()
                controller.play()
            }
            var progressed = false
            repeat(40) {
                instrumentation.runOnMainSync { progressed = controller.isPlaying && controller.currentPosition > 500 }
                if (!progressed) Thread.sleep(250)
            }
            assertTrue("Audio must decode and position advance", progressed)
            instrumentation.runOnMainSync {
                assertNull(controller.playerError)
                assertEquals(2, controller.mediaItemCount)
                controller.pause()
                controller.seekTo(3000)
            }
            Thread.sleep(500)
            instrumentation.runOnMainSync {
                assertFalse(controller.playWhenReady)
                assertTrue(controller.currentPosition in 2800..3200)
                controller.seekToNextMediaItem()
            }
            Thread.sleep(300)
            instrumentation.runOnMainSync {
                assertEquals(1, controller.currentMediaItemIndex)
                controller.removeMediaItem(0)
                assertEquals(1, controller.mediaItemCount)
                controller.clearMediaItems()
            }
        } finally {
            instrumentation.runOnMainSync { controller.release() }
            resolver.delete(uri, null, null)
        }
    }
}
