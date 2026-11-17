package app.saz.music.providers

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.saz.music.playback.PlaybackConnection
import app.saz.music.settings.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OnlineState(val loading: Boolean = false, val items: List<MusicItem> = emptyList(),
    val nextPage: String? = null, val problem: ProviderProblem? = null, val searched: Boolean = false)

@HiltViewModel
class ProviderViewModel @Inject constructor(private val youtube: YouTubeProvider, private val audius: AudiusProvider, private val keys: ApiKeyStore,
    private val preferences: Preferences, private val playback: PlaybackConnection,
    private val audioDownloads: AudioDownloads, val lyrics: LrclibProvider,
    @ApplicationContext private val context: Context) : ViewModel() {
    private val mutable = MutableStateFlow(OnlineState())
    val state = mutable.asStateFlow()
    private val freeMutable = MutableStateFlow(OnlineState())
    val freeState = freeMutable.asStateFlow()
    private var freeJob: Job? = null
    private var freeQuery = ""
    fun searchFree(query: String, next: Boolean = false) {
        if (next && (freeState.value.loading || freeState.value.nextPage == null)) return
        freeJob?.cancel()
        val request = SearchRequest(if (next) freeQuery else query, pageToken = if (next) freeState.value.nextPage else null)
        freeQuery = request.query
        val previous = if (next) freeState.value.items else emptyList()
        freeMutable.value = OnlineState(loading = true, items = previous, searched = true)
        freeJob = viewModelScope.launch {
            when (val result = audius.search(request)) {
                is ProviderResult.Success -> freeMutable.value = OnlineState(items = (previous + result.value.items).distinctBy { it.key }, nextPage = result.value.nextPageToken, searched = true)
                is ProviderResult.Failure -> freeMutable.value = OnlineState(items = previous, problem = result.problem, searched = true)
            }
        }
    }
    fun downloadFree(item: MusicItem) = action {
        when (val result = audius.download(item.key)) {
            is ProviderResult.Failure -> notice.value = result.problem.message
            is ProviderResult.Success -> {
                audioDownloads.enqueueAuthorized(result.value, wifiOnly = true)
                notice.value = "Download queued over Wi-Fi. Open Settings → Download audio to import it when complete."
            }
        }
    }
    private val selected = MutableStateFlow<MusicItem?>(null)
    val selectedVideo = selected.asStateFlow()
    val configured = MutableStateFlow(false)
    val notice = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    private var searchJob: Job? = null
    private var detailsJob: Job? = null
    private var lastRequest = SearchRequest("")
    val downloads = flow {
        while (currentCoroutineContext().isActive) {
            try { emit(audioDownloads.downloads()) }
            catch (_: SecurityException) { notice.value = "Download access is unavailable on this device."; emit(emptyList()) }
            delay(2000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch {
        try { configured.value = keys.youtubeKey().isNotBlank() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { notice.value = "The stored YouTube key cannot be opened. Import it again." }
    } }
    fun search(query: String, recent: Boolean, region: String, next: Boolean = false) {
        if (next && (state.value.loading || state.value.nextPage == null)) return
        searchJob?.cancel()
        detailsJob?.cancel()
        val request = if (next) lastRequest.copy(pageToken = state.value.nextPage) else SearchRequest(query, region, recent)
        lastRequest = request.copy(pageToken = null)
        val previous = if (next) state.value.items else emptyList()
        mutable.value = OnlineState(loading = true, items = previous, searched = true)
        searchJob = viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { youtube.search(request) }) {
                is ProviderResult.Success -> mutable.value = OnlineState(items = (previous + result.value.items).distinctBy { it.key }, nextPage = result.value.nextPageToken, searched = true)
                is ProviderResult.Failure -> mutable.value = OnlineState(items = previous, problem = result.problem, searched = true)
            }
        }
    }
    fun retry() = search(lastRequest.query, lastRequest.recent, lastRequest.region)
    fun openVideo(item: MusicItem) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            notice.value = "Checking video availability…"
            when (val result = withContext(Dispatchers.IO) { youtube.prepare(item.key) }) {
                is ProviderResult.Success -> { playback.pause(); selected.value = result.value; notice.value = null }
                is ProviderResult.Failure -> notice.value = result.problem.message
            }
        }
    }
    fun closeVideo() { selected.value = null }
    fun configure(value: String) = action {
        keys.save(value); searchJob?.cancelAndJoin(); youtube.clearCache()
        configured.value = true; mutable.value = OnlineState(); notice.value = "YouTube key saved on this device. Run a search to test access."
    }
    fun importKey(uri: Uri) = action {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {
                val bytes = it.readBytesLimited(4096)
                bytes.toString(Charsets.UTF_8)
            } ?: error("Could not read the key file.")
        }
        val matches = Regex("AIza[0-9A-Za-z_-]{35}").findAll(text).map { it.value }.toSet()
        require(matches.size == 1) { "The file must contain exactly one Google API key." }
        keys.save(matches.single()); searchJob?.cancelAndJoin(); youtube.clearCache()
        configured.value = true; mutable.value = OnlineState(); notice.value = "YouTube key imported securely."
    }
    fun clearKey() = action {
        searchJob?.cancelAndJoin(); detailsJob?.cancelAndJoin(); keys.clear(); youtube.clearCache()
        configured.value = false; selected.value = null; mutable.value = OnlineState(); notice.value = "YouTube key removed."
    }
    fun region(value: String) = action { preferences.region(value); notice.value = "Discovery region saved." }
    fun download(url: String, permitted: Boolean, wifiOnly: Boolean) = action {
        audioDownloads.enqueue(url, permitted, wifiOnly); notice.value = "Download queued. Import it after completion."
    }
    fun importDownloads() = action { notice.value = "Imported ${audioDownloads.importCompleted()} audio files. Unsupported or incomplete files were skipped." }
    fun cancelDownload(id: Long) = action { audioDownloads.cancel(id) }
    private fun action(block: suspend () -> Unit) { viewModelScope.launch {
        if (saving.value) return@launch
        saving.value = true
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: IllegalArgumentException) { notice.value = e.message ?: "Check the entered values." }
        catch (_: Exception) { notice.value = "Could not complete this action. Check the connection, storage and provider configuration." }
        finally { saving.value = false }
    } }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "Key file is too large." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
