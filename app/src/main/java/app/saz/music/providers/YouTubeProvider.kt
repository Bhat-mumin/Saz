package app.saz.music.providers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.IOException
import java.time.Duration
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeProvider @Inject constructor(private val http: JsonTransport, private val credentials: ProviderCredentials,
    private val identity: AndroidClientIdentity) : SearchProvider, MetadataProvider, LibraryProvider, PlaybackProvider {
    override val capabilities = PlaybackCapabilities(PlaybackMode.EMBEDDED_VIDEO, background = false, seek = true)
    private val mutex = Mutex()
    private var blockedUntil = 0L
    private val cache = LinkedHashMap<SearchRequest, Pair<Long, SearchPage>>()
    suspend fun clearCache() = mutex.withLock { cache.clear(); blockedUntil = 0 }

    override suspend fun search(request: SearchRequest): ProviderResult<SearchPage> = mutex.withLock {
        val normalized = request.copy(query = request.query.trim(), region = request.region.uppercase(Locale.ROOT))
        if (normalized.query.length > 200 || (normalized.region.isNotEmpty() && normalized.region !in Locale.getISOCountries()))
            return@withLock problem(ProblemKind.INVALID_REQUEST, "Use a search of up to 200 characters and a valid two-letter country code.")
        cache[normalized]?.takeIf { System.currentTimeMillis() - it.first < 300_000 }?.let { return@withLock ProviderResult.Success(it.second) }
        val playlist = youtubePlaylistId(normalized.query)
        when (val response = request(if (playlist == null) "search" else "playlistItems", buildMap {
            if (playlist != null) {
                put("part", "snippet"); put("playlistId", playlist); put("maxResults", "20")
                normalized.pageToken?.let { put("pageToken", it) }
            } else {
            put("part", "snippet"); put("type", "video"); put("videoCategoryId", "10")
            put("videoEmbeddable", "true"); put("videoSyndicated", "true"); put("maxResults", "20")
            put("order", if (normalized.recent) "date" else "relevance")
            if (normalized.query.isNotBlank()) put("q", normalized.query)
            if (normalized.region.isNotBlank()) put("regionCode", normalized.region)
            normalized.pageToken?.let { put("pageToken", it) }
            }
        })) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> try {
                val json = response.value
                val array = json.getJSONArray("items")
                val items = (0 until array.length()).mapNotNull { index ->
                    val item = array.getJSONObject(index)
                    val id = if (playlist == null) item.optJSONObject("id")?.optString("videoId").orEmpty()
                        else item.optJSONObject("snippet")?.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                    if (!validVideoId(id)) null else parseItem(id, item)
                }
                val page = SearchPage(items, json.optString("nextPageToken").takeIf { it.isNotBlank() })
                if (cache.size >= 16) cache.remove(cache.keys.first())
                cache[normalized] = System.currentTimeMillis() to page
                ProviderResult.Success(page)
            } catch (_: org.json.JSONException) { problem(ProblemKind.UNAVAILABLE, "YouTube returned an unreadable response. Try again later.") }
        }
    }

    override suspend fun metadata(key: MediaKey): ProviderResult<MusicItem> = mutex.withLock {
        if (key.provider != ProviderId.YOUTUBE || !validVideoId(key.value))
            return@withLock problem(ProblemKind.INVALID_REQUEST, "Invalid YouTube video identifier.")
        when (val response = request("videos", mapOf("part" to "snippet,contentDetails,status", "id" to key.value))) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> try {
                val item = response.value.getJSONArray("items").optJSONObject(0)
                    ?: return@withLock problem(ProblemKind.NOT_FOUND, "This video is private, deleted, or unavailable.")
                ProviderResult.Success(parseItem(key.value, item))
            } catch (_: org.json.JSONException) { problem(ProblemKind.UNAVAILABLE, "YouTube returned unreadable metadata.") }
        }
    }
    override suspend fun prepare(key: MediaKey): ProviderResult<MusicItem> = when (val result = metadata(key)) {
        is ProviderResult.Success -> if (result.value.embeddable) result else problem(ProblemKind.UNAVAILABLE, "The owner does not allow embedded playback. Open this video on YouTube.")
        is ProviderResult.Failure -> result
    }
    override suspend fun library(): ProviderResult<List<MusicItem>> = problem(ProblemKind.UNAVAILABLE,
        "Private YouTube libraries require OAuth. An API key does not connect your account.")

    private suspend fun request(path: String, parameters: Map<String, String>): ProviderResult<JSONObject> {
        val remaining = (blockedUntil - System.currentTimeMillis() + 999) / 1000
        if (remaining > 0) return problem(ProblemKind.QUOTA, "YouTube requests are paused. Retry in $remaining seconds.", remaining)
        return try {
            val key = credentials.youtubeKey()
            if (key.isBlank()) return problem(ProblemKind.CONFIGURATION, "Add a YouTube Data API key in Settings to search online.")
            val reply = http.get("https://www.googleapis.com/youtube/v3/$path", parameters + ("key" to key), identity.headers())
            if (reply.status in 200..299) return ProviderResult.Success(JSONObject(reply.body))
            val error = youtubeFailure(reply)
            if (error.problem.kind == ProblemKind.QUOTA) blockedUntil = System.currentTimeMillis() + (error.problem.retryAfterSeconds ?: 60) * 1000
            error
        } catch (e: CancellationException) { throw e }
        catch (_: IOException) { problem(ProblemKind.NETWORK, "Could not reach YouTube. Check your connection and retry.") }
        catch (_: org.json.JSONException) { problem(ProblemKind.UNAVAILABLE, "YouTube returned an unreadable response.") }
        catch (_: java.security.GeneralSecurityException) { problem(ProblemKind.CONFIGURATION, "The saved API key cannot be opened. Import it again in Settings.") }
        catch (_: IllegalArgumentException) { problem(ProblemKind.CONFIGURATION, "The saved provider configuration is invalid. Import the key again in Settings.") }
    }
}

internal fun validVideoId(id: String) = Regex("[A-Za-z0-9_-]{11}").matches(id)
internal fun youtubePlaylistId(query: String): String? {
    val uri = try { java.net.URI(query) } catch (_: java.net.URISyntaxException) { return null }
    if (uri.scheme != "https" || uri.host !in setOf("youtube.com", "www.youtube.com", "music.youtube.com", "m.youtube.com") || uri.userInfo != null) return null
    val list = uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("list=") }?.substringAfter('=') ?: return null
    return list.takeIf { Regex("[A-Za-z0-9_-]{10,100}").matches(it) }
}
internal fun parseItem(id: String, item: JSONObject): MusicItem {
    val snippet = item.getJSONObject("snippet")
    val thumbnails = snippet.optJSONObject("thumbnails")
    val thumbnail = thumbnails?.optJSONObject("medium")?.optString("url") ?: thumbnails?.optJSONObject("default")?.optString("url")
    val duration = try { Duration.parse(item.optJSONObject("contentDetails")?.optString("duration") ?: "PT0S").toMillis() }
        catch (_: java.time.format.DateTimeParseException) { 0L }
    return MusicItem(MediaKey(ProviderId.YOUTUBE, id), android.text.Html.fromHtml(snippet.getString("title"), 0).toString(),
        snippet.optString("videoOwnerChannelTitle", snippet.optString("channelTitle", "YouTube")), thumbnail?.takeIf { it.startsWith("https://") }, duration,
        snippet.optString("publishedAt").takeIf { it.isNotBlank() }, item.optJSONObject("status")?.optBoolean("embeddable", true) ?: true)
}

internal fun youtubeFailure(reply: HttpReply): ProviderResult.Failure {
    val reason = try {
        val error = JSONObject(reply.body).optJSONObject("error")
        error?.optJSONArray("details")?.optJSONObject(0)?.optString("reason")?.takeIf { it.isNotBlank() }
            ?: error?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
    }
        catch (_: org.json.JSONException) { "" }
    return when {
        reason == "playlistItemsNotAccessible" -> problem(ProblemKind.AUTHENTICATION, "This playlist is private. Private playlists need an account connection; paste a public or unlisted playlist link instead.")
        reply.status == 429 || reason in setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded", "userRateLimitExceeded") ->
            problem(ProblemKind.QUOTA, "YouTube quota or rate limit reached. Wait, or review the project's quota in Google Cloud.", reply.retryAfterSeconds ?: if (reason == "quotaExceeded" || reason == "dailyLimitExceeded") 3600 else 60)
        reply.status == 401 || reply.status == 403 || reason in setOf("keyInvalid", "accessNotConfigured", "ipRefererBlocked", "API_KEY_INVALID", "API_KEY_SERVICE_BLOCKED", "API_KEY_ANDROID_APP_BLOCKED") ->
            problem(ProblemKind.AUTHENTICATION, "YouTube rejected this key or app. Check API enablement and key restrictions in Google Cloud.")
        reply.status == 400 -> problem(ProblemKind.INVALID_REQUEST, "YouTube rejected the search options. Check the country code and try again.")
        reply.status == 404 -> problem(ProblemKind.NOT_FOUND, "This YouTube item is unavailable.")
        else -> problem(ProblemKind.UNAVAILABLE, "YouTube is unavailable right now. Try again later.", reply.retryAfterSeconds)
    }
}
