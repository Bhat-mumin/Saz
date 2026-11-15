package app.saz.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.*
import app.saz.music.database.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(val connected: Boolean = false, val uri: String? = null, val title: String = "", val artist: String = "",
    val playing: Boolean = false, val position: Long = 0, val duration: Long = 0, val shuffle: Boolean = false,
    val repeat: Int = 0, val queue: List<Pair<String, String>> = emptyList(), val index: Int = -1, val error: String? = null)

@Singleton class PlaybackConnection @Inject constructor(@ApplicationContext context: Context) {
    private val mutable = MutableStateFlow(PlaybackState())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    init {
        future.addListener({
            runCatching { future.get() }.onSuccess { c ->
                controller = c
                c.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = update()
                })
                update()
            }.onFailure { mutable.value = mutable.value.copy(error = "Playback service unavailable. Reopen Saz to retry.") }
        }, ContextCompat.getMainExecutor(context))
        scope.launch { while (isActive) { delay(500); if (controller?.isPlaying == true) update() } }
    }
    private fun update() {
        val c = controller ?: return
        mutable.value = PlaybackState(true, c.currentMediaItem?.mediaId, c.mediaMetadata.title?.toString().orEmpty(),
            c.mediaMetadata.artist?.toString().orEmpty(), c.isPlaying, c.currentPosition.coerceAtLeast(0), c.duration.coerceAtLeast(0),
            c.shuffleModeEnabled, c.repeatMode, (0 until c.mediaItemCount).map { c.getMediaItemAt(it).let { item -> item.mediaId to item.mediaMetadata.title.toString() } },
            c.currentMediaItemIndex, c.playerError?.let { "Cannot play this file. It may be missing, unsupported, or no longer accessible." })
    }
    fun play(tracks: List<Track>, index: Int = 0) {
        if (tracks.isEmpty()) return
        controller?.apply { setMediaItems(tracks.map { it.item() }, index.coerceIn(tracks.indices), 0); prepare(); play() }
    }
    fun toggle() { controller?.apply { if (isPlaying) pause() else { if (playerError != null) prepare(); play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun seek(position: Long) { controller?.seekTo(position) }
    fun shuffle() { controller?.apply { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.apply { repeatMode = (repeatMode + 1) % 3 } }
    fun jump(index: Int) { controller?.apply { seekToDefaultPosition(index); prepare(); play() } }
    fun remove(index: Int) { controller?.removeMediaItem(index) }
    fun move(from: Int, to: Int) { controller?.moveMediaItem(from, to) }
    fun enqueue(track: Track) { controller?.addMediaItem(track.item()) }
    fun speed(value: Float) { controller?.setPlaybackSpeed(value) }
    fun sleep(minutes: Int) { controller?.sendCustomCommand(SessionCommand(PlaybackService.SLEEP, Bundle.EMPTY), Bundle().apply { putInt("minutes", minutes) }) }
    private fun Track.item(): MediaItem = MediaItem.Builder().setMediaId(uri).setUri(uri).setMediaMetadata(MediaMetadata.Builder()
        .setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(artworkUri?.let(android.net.Uri::parse)).build()).build()
}
