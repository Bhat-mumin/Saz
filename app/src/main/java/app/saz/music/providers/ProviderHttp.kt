package app.saz.music.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HttpReply(val status: Int, val body: String, val retryAfterSeconds: Long? = null)
interface JsonTransport {
    suspend fun get(url: String, parameters: Map<String, String>, headers: Map<String, String> = emptyMap()): HttpReply
}

class ProviderHttp @Inject constructor() : JsonTransport {
    override suspend fun get(url: String, parameters: Map<String, String>, headers: Map<String, String>): HttpReply = withContext(Dispatchers.IO) {
        val endpoint = URI(url)
        require(endpoint.scheme == "https" && endpoint.host in setOf("www.googleapis.com", "lrclib.net", "api.audius.co"))
        val query = parameters.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val connection = URI("$url?$query").toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Saz/0.1.0 (https://github.com/Bhat-mumin/Saz)")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val output = ByteArrayOutputStream()
            stream?.use {
                val buffer = ByteArray(8192)
                while (true) {
                    ensureActive()
                    val size = it.read(buffer)
                    if (size < 0) break
                    if (output.size() + size > 2_097_152) throw IOException("Provider response is too large")
                    output.write(buffer, 0, size)
                }
            }
            HttpReply(status, output.toString("UTF-8"), retryAfter(connection.getHeaderField("Retry-After")))
        } finally { connection.disconnect() }
    }
}

internal fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
internal fun retryAfter(value: String?, nowMs: Long = System.currentTimeMillis()): Long? {
    if (value == null) return null
    value.toLongOrNull()?.let { return it.coerceAtLeast(1) }
    return try { ((ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - nowMs) / 1000).coerceAtLeast(1) }
    catch (_: java.time.format.DateTimeParseException) { null }
}
