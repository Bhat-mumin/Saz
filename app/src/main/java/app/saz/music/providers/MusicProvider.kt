package app.saz.music.providers

enum class ProviderId { LOCAL, YOUTUBE, SPOTIFY, AUDIUS }
data class MediaKey(val provider: ProviderId, val value: String)
enum class PlaybackMode { LOCAL_AUDIO, EMBEDDED_VIDEO, EXTERNAL_APP }
data class PlaybackCapabilities(val mode: PlaybackMode, val background: Boolean, val seek: Boolean, val downloadable: Boolean = false)
data class MusicItem(val key: MediaKey, val title: String, val artist: String,
    val artwork: String? = null, val durationMs: Long = 0, val publishedAt: String? = null, val embeddable: Boolean = true)
data class SearchRequest(val query: String, val region: String = "", val recent: Boolean = false, val pageToken: String? = null)
data class SearchPage(val items: List<MusicItem>, val nextPageToken: String? = null)
enum class ProblemKind { CONFIGURATION, AUTHENTICATION, QUOTA, NETWORK, UNAVAILABLE, INVALID_REQUEST, NOT_FOUND }
data class ProviderProblem(val kind: ProblemKind, val message: String, val retryAfterSeconds: Long? = null)
sealed interface ProviderResult<out T> {
    data class Success<T>(val value: T) : ProviderResult<T>
    data class Failure(val problem: ProviderProblem) : ProviderResult<Nothing>
}
interface SearchProvider { suspend fun search(request: SearchRequest): ProviderResult<SearchPage> }
interface MetadataProvider { suspend fun metadata(key: MediaKey): ProviderResult<MusicItem> }
interface LibraryProvider { suspend fun library(): ProviderResult<List<MusicItem>> }
interface PlaybackProvider {
    val capabilities: PlaybackCapabilities
    suspend fun prepare(key: MediaKey): ProviderResult<MusicItem>
}
interface ProviderCredentials { suspend fun youtubeKey(): String }

internal fun problem(kind: ProblemKind, message: String, retry: Long? = null) =
    ProviderResult.Failure(ProviderProblem(kind, message, retry))
