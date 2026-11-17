package app.saz.music.providers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class AuthorizedAudio(val url: String, val title: String, val extension: String, val artist: String = "Unknown artist")

/** Only artist-enabled, ungated downloads; downloaded files enter the local Media3 library. */
@Singleton
class AudiusProvider @Inject constructor(private val http: JsonTransport) : SearchProvider, MetadataProvider {
    private val mutex = Mutex()
    private var blockedUntil = 0L

    override suspend fun search(request: SearchRequest): ProviderResult<SearchPage> = mutex.withLock {
        val offset = request.pageToken?.toIntOrNull() ?: 0
        if (request.query.isBlank() || request.query.length > 200 || offset !in 0..10000)
            return@withLock problem(ProblemKind.INVALID_REQUEST, "Enter a song or artist name of up to 200 characters.")
        when (val result = fetch("tracks/search", mapOf("query" to request.query.trim(), "limit" to "20", "offset" to offset.toString()))) {
            is ProviderResult.Failure -> result
            is ProviderResult.Success -> try {
                val array = result.value.getJSONArray("data")
                val items = (0 until array.length()).map { array.getJSONObject(it) }.filter(::downloadAllowed).map(::item)
                ProviderResult.Success(SearchPage(items, if (array.length() == 20 && offset < 10000) (offset + 20).toString() else null))
            } catch (_: org.json.JSONException) { unreadable() }
        }
    }

    override suspend fun metadata(key: MediaKey): ProviderResult<MusicItem> = mutex.withLock {
        when (val result = track(key)) {
            is ProviderResult.Failure -> result
            is ProviderResult.Success -> try { ProviderResult.Success(item(result.value)) }
                catch (_: org.json.JSONException) { unreadable() }
        }
    }

    suspend fun download(key: MediaKey): ProviderResult<AuthorizedAudio> = mutex.withLock {
        when (val result = track(key)) {
            is ProviderResult.Failure -> result
            is ProviderResult.Success -> {
                val track = result.value
                if (!downloadAllowed(track)) return@withLock problem(ProblemKind.UNAVAILABLE, "The artist no longer offers this track as a free download.")
                val url = track.optJSONObject("download")?.optString("url").orEmpty()
                val extension = track.optString("orig_filename").substringAfterLast('.').lowercase()
                val uri = try { URI(url) } catch (_: Exception) { null }
                if (uri?.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null || uri.fragment != null ||
                    extension !in setOf("mp3", "wav", "flac", "aiff", "m4a", "aac", "ogg", "opus"))
                    return@withLock problem(ProblemKind.UNAVAILABLE, "Audius has not supplied a supported audio download for this track.")
                ProviderResult.Success(AuthorizedAudio(url, track.optString("title", "Audius audio"), extension,
                    track.optJSONObject("user")?.optString("name") ?: "Unknown artist"))
            }
        }
    }

    private suspend fun track(key: MediaKey): ProviderResult<JSONObject> {
        if (key.provider != ProviderId.AUDIUS || !Regex("[A-Za-z0-9]{1,32}").matches(key.value))
            return problem(ProblemKind.INVALID_REQUEST, "Invalid Audius track.")
        return when (val result = fetch("tracks/${key.value}", emptyMap())) {
            is ProviderResult.Failure -> result
            is ProviderResult.Success -> try {
                val track = result.value.optJSONObject("data")
                if (track == null) problem(ProblemKind.NOT_FOUND, "This Audius track is unavailable.") else ProviderResult.Success(track)
            } catch (_: org.json.JSONException) { unreadable() }
        }
    }

    private suspend fun fetch(path: String, parameters: Map<String, String>): ProviderResult<JSONObject> {
        if (System.currentTimeMillis() < blockedUntil) return problem(ProblemKind.QUOTA, "Audius requests are paused. Try again shortly.")
        return try {
            val reply = http.get("https://api.audius.co/v1/$path", parameters + ("app_name" to "Saz"))
            when (reply.status) {
                in 200..299 -> ProviderResult.Success(JSONObject(reply.body))
                429 -> { val seconds = (reply.retryAfterSeconds ?: 60).coerceIn(1, 86400)
                    blockedUntil = System.currentTimeMillis() + seconds * 1000
                    problem(ProblemKind.QUOTA, "Audius is receiving too many requests. Retry later.", seconds) }
                401, 403 -> problem(ProblemKind.AUTHENTICATION, "Audius denied access. This source may be restricted or unavailable.")
                404 -> problem(ProblemKind.NOT_FOUND, "This Audius track is unavailable.")
                else -> problem(ProblemKind.UNAVAILABLE, "Audius is unavailable. Try again later.")
            }
        } catch (e: CancellationException) { throw e }
        catch (_: IOException) { problem(ProblemKind.NETWORK, "Could not reach Audius. Check your connection.") }
        catch (_: org.json.JSONException) { unreadable() }
    }

    private fun unreadable() = problem(ProblemKind.UNAVAILABLE, "Audius returned an unreadable response.")
    private fun item(track: JSONObject) = MusicItem(MediaKey(ProviderId.AUDIUS, track.getString("id")),
        track.getString("title"), track.optJSONObject("user")?.optString("name") ?: "Audius artist",
        track.optJSONObject("artwork")?.optString("150x150")?.takeIf { it.startsWith("https://") },
        track.optLong("duration").coerceAtLeast(0) * 1000, track.optString("release_date").takeIf { it.isNotBlank() })
}

internal fun downloadAllowed(track: JSONObject): Boolean = track.optBoolean("is_downloadable", false) &&
    track.optBoolean("is_available", false) && !track.optBoolean("is_delete", false) &&
    !track.optBoolean("is_download_gated", true) && !track.optBoolean("is_stream_gated", true) &&
    track.isNull("download_conditions") && track.isNull("stream_conditions") && track.isNull("allowed_api_keys")
