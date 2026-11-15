package app.saz.music.playback

import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import app.saz.music.database.LibraryDao
import app.saz.music.settings.Preferences
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    @Inject lateinit var library: LibraryDao
    @Inject lateinit var preferences: Preferences
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timer: Job? = null
    private var ready = false
    private var historyEnabled = true
    private val snapshots = Channel<String>(Channel.CONFLATED)
    private val file get() = File(filesDir, "playback.json")

    override fun onCreate() {
        super.onCreate()
        scope.launch(Dispatchers.IO) {
            for (value in snapshots) {
                val atomic = android.util.AtomicFile(file)
                var output: java.io.FileOutputStream? = null
                try { output = atomic.startWrite(); output.write(value.toByteArray()); atomic.finishWrite(output) }
                catch (_: java.io.IOException) { atomic.failWrite(output) }
            }
        }
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        session = MediaSession.Builder(this, player).setCallback(object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                if (controller.packageName != packageName && !controller.isTrusted) return MediaSession.ConnectionResult.reject()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(SLEEP, Bundle.EMPTY)).build()).build()
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                if (command.customAction != SLEEP) return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                val minutes = args.getInt("minutes").coerceIn(0, 180)
                timer?.cancel()
                if (minutes > 0) timer = scope.launch { delay(minutes * 60000L); player.pause() }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
                val valid = mediaItems.filter { it.localConfiguration?.uri?.scheme == "content" }.toMutableList()
                return Futures.immediateFuture(valid)
            }
        }).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { if (ready) persist() }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                if (ready && player.playWhenReady && historyEnabled && item != null) scope.launch { library.played(item.mediaId, System.currentTimeMillis()) }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && historyEnabled) player.currentMediaItem?.let { item -> scope.launch { library.played(item.mediaId, System.currentTimeMillis()) } }
            }
        })
        scope.launch { preferences.settings.collect { historyEnabled = it.history } }
        // Restore paused. URI readability is checked by the player; failures are shown in the UI.
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { android.util.AtomicFile(file).openRead().use { JSONObject(it.bufferedReader().readText()) } }.getOrNull() }
            if (player.mediaItemCount == 0 && saved != null) runCatching {
                val entries = saved.getJSONArray("queue")
                val items = (0 until entries.length()).mapNotNull { i ->
                    val obj = entries.getJSONObject(i)
                    val uri = android.net.Uri.parse(obj.getString("uri"))
                    if (uri.scheme != "content") null else MediaItem.Builder().setMediaId(uri.toString()).setUri(uri)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(obj.optString("title")).setArtist(obj.optString("artist")).build()).build()
                }
                if (items.isNotEmpty()) {
                    player.setMediaItems(items, saved.optInt("index").coerceIn(items.indices), saved.optLong("position").coerceAtLeast(0))
                    player.repeatMode = saved.optInt("repeat").coerceIn(0, 2)
                    player.shuffleModeEnabled = saved.optBoolean("shuffle")
                    player.prepare()
                }
            }
            ready = true
            while (isActive) { delay(5000); persist() }
        }
    }

    private fun persist() {
        val entries = JSONArray()
        for (index in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(index)
            entries.put(JSONObject().put("uri", item.localConfiguration?.uri.toString())
                .put("title", item.mediaMetadata.title?.toString()).put("artist", item.mediaMetadata.artist?.toString()))
        }
        val value = JSONObject().put("queue", entries).put("index", player.currentMediaItemIndex)
            .put("position", player.currentPosition).put("repeat", player.repeatMode).put("shuffle", player.shuffleModeEnabled).toString()
        // A single writer prevents concurrent AtomicFile writes during rapid seeks/queue edits.
        snapshots.trySend(value)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        scope.cancel()
        session?.release()
        player.release()
        super.onDestroy()
    }
    companion object { const val SLEEP = "app.saz.music.SLEEP" }
}
