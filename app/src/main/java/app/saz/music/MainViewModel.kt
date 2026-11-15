package app.saz.music

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.saz.music.database.*
import app.saz.music.library.LibraryRepository
import app.saz.music.playback.PlaybackConnection
import app.saz.music.settings.Preferences
import app.saz.music.settings.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(val library: LibraryRepository, val playback: PlaybackConnection,
    val preferences: Preferences, private val backup: app.saz.music.backup.BackupRepository,
    private val lyricsRepository: app.saz.music.lyrics.LyricsRepository,
    @ApplicationContext private val context: Context) : ViewModel() {
    val tracks = library.dao.tracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = library.dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val entries = library.dao.entries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val lyricText = MutableStateFlow("")
    init {
        viewModelScope.launch {
            playback.state.map { it.uri }.distinctUntilChanged().collectLatest { uri ->
                lyricText.value = ""
                if (uri != null) lyricText.value = try { lyricsRepository.read(uri) } catch (_: java.io.IOException) { "" }
            }
        }
    }
    private fun work(block: suspend () -> Unit) { viewModelScope.launch {
        if (busy.value) return@launch
        busy.value = true
        try { block() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { message.value = e.message ?: "Something went wrong. Please try again." }
        finally { busy.value = false }
    } }
    fun scan() = work { message.value = "Found ${library.scan()} audio files" }
    fun import(uris: List<Uri>) = work { val count = library.import(uris); message.value = "Imported $count of ${uris.size} files" }
    fun folder(uri: Uri) = work { message.value = "Imported ${library.importFolder(uri)} files from folder" }
    fun favorite(track: Track) { viewModelScope.launch { library.dao.favorite(track.uri) } }
    fun createPlaylist(name: String) = work { require(name.trim().isNotEmpty() && name.length <= 100); library.dao.create(Playlist(name = name.trim())) }
    fun add(playlist: Playlist, track: Track) = work { library.dao.add(PlaylistEntry(playlist.id, track.uri)); message.value = "Added to ${playlist.name}" }
    fun delete(playlist: Playlist) = work { library.dao.deletePlaylist(playlist.id) }
    fun rename(playlist: Playlist, name: String) = work {
        require(name.trim().isNotEmpty() && name.length <= 100)
        library.dao.renamePlaylist(playlist.id, name.trim())
    }
    fun remove(playlistId: Long, track: Track) = work { library.dao.remove(playlistId, track.uri) }
    fun history(enabled: Boolean) = work { preferences.history(enabled); if (!enabled) library.dao.clearHistory() }
    fun graphics(value: String) = work { preferences.graphics(value) }
    fun motion(value: Boolean) = work { preferences.reducedMotion(value) }
    fun export(uri: Uri) = work { backup.export(uri); message.value = "Backup saved" }
    fun restore(uri: Uri) = work { message.value = "Merged ${backup.restore(uri)} tracks. Re-import files if access needs to be restored." }
    fun lyrics(uri: Uri) = work {
        val trackUri = playback.state.value.uri ?: error("Choose a song first")
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1_048_576) { "Lyrics file is too large (maximum 1 MB)." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray().toString(Charsets.UTF_8)
            } ?: error("Could not read lyrics")
        }
        lyricsRepository.save(trackUri, text)
        if (playback.state.value.uri == trackUri) lyricText.value = text
    }
}
