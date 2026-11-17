package app.saz.music.providers

import app.saz.music.lyrics.Lrc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class LyricMatch(val text: String, val synchronized: Boolean, val source: String, val sourceUrl: String, val instrumental: Boolean = false)
interface LyricsProvider { suspend fun find(title: String, artist: String, durationMs: Long): ProviderResult<LyricMatch> }

@Singleton
class LrclibProvider @Inject constructor(private val http: JsonTransport) : LyricsProvider {
    private val mutex = Mutex()
    private var nextRequestAt = 0L
    private val cache = LinkedHashMap<Triple<String, String, Long>, LyricMatch>()
    override suspend fun find(title: String, artist: String, durationMs: Long): ProviderResult<LyricMatch> = mutex.withLock {
        if (title.isBlank() || artist.isBlank() || artist == "Unknown artist" || durationMs !in 1000..3_600_000)
            return@withLock problem(ProblemKind.INVALID_REQUEST, "Lyrics need a song title, artist and duration between 1 second and 1 hour. Imported LRC files also work.")
        val key = Triple(title.trim(), artist.trim(), durationMs)
        cache[key]?.let { return@withLock ProviderResult.Success(it) }
        val wait = nextRequestAt - System.currentTimeMillis()
        if (wait > 500) return@withLock problem(ProblemKind.QUOTA, "Lyric lookup is rate limited. Retry later.", (wait + 999) / 1000)
        if (wait > 0) delay(wait)
        try {
            val reply = http.get("https://lrclib.net/api/get", mapOf("track_name" to key.first, "artist_name" to key.second, "duration" to (durationMs / 1000.0).toString()))
            nextRequestAt = System.currentTimeMillis() + 350
            when (reply.status) {
                200 -> {
                    val json = JSONObject(reply.body)
                    // Do not assign timestamps to a different recording or to unsynchronized text.
                    val matchedDuration = json.optDouble("duration", 0.0)
                    if (!matchedDuration.isFinite() || kotlin.math.abs(matchedDuration * 1000 - durationMs) > 2000)
                        return@withLock problem(ProblemKind.NOT_FOUND, "No lyrics matched this recording's duration.")
                    val synced = json.optString("syncedLyrics").takeUnless { it == "null" }.orEmpty()
                    val plain = json.optString("plainLyrics").takeUnless { it == "null" }.orEmpty()
                    val timed = Lrc.parse(synced).isNotEmpty()
                    val instrumental = json.optBoolean("instrumental")
                    if (!timed && plain.isBlank() && !instrumental) return@withLock problem(ProblemKind.NOT_FOUND, "No lyrics are available for this recording.")
                    val match = LyricMatch(if (timed) synced else plain, timed, "LRCLIB", "https://lrclib.net/api/get/${json.getLong("id")}", instrumental)
                    if (cache.size >= 20) cache.remove(cache.keys.first())
                    cache[key] = match
                    ProviderResult.Success(match)
                }
                404 -> problem(ProblemKind.NOT_FOUND, "No lyrics matched this song. Import a lyric file you are authorized to use.")
                429 -> {
                    val seconds = reply.retryAfterSeconds ?: 60
                    nextRequestAt = System.currentTimeMillis() + seconds * 1000 + Random.nextLong(1, (seconds * 300).coerceAtLeast(2))
                    problem(ProblemKind.QUOTA, "LRCLIB is rate limiting requests. Retry later.", seconds)
                }
                401, 403 -> problem(ProblemKind.AUTHENTICATION, "LRCLIB rejected the request. Try an authorized local lyric file.")
                else -> problem(ProblemKind.UNAVAILABLE, "LRCLIB is unavailable right now.")
            }
        } catch (e: CancellationException) { throw e }
        catch (_: IOException) { problem(ProblemKind.NETWORK, "Could not reach LRCLIB. Check your connection.") }
        catch (_: org.json.JSONException) { problem(ProblemKind.UNAVAILABLE, "LRCLIB returned unreadable lyrics.") }
    }
}
