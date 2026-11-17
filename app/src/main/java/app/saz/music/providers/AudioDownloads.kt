package app.saz.music.providers

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import app.saz.music.library.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID
import javax.inject.Inject

data class AudioDownload(val id: Long, val title: String, val status: Int, val downloaded: Long, val total: Long, val detail: String)
interface DownloadProvider {
    suspend fun enqueue(url: String, permitted: Boolean, wifiOnly: Boolean): Long
    suspend fun downloads(): List<AudioDownload>
    suspend fun cancel(id: Long)
}

class AudioDownloads @Inject constructor(@ApplicationContext private val context: Context,
    private val library: LibraryRepository) : DownloadProvider {
    private val manager get() = context.getSystemService(DownloadManager::class.java)
    private val metadata get() = context.getSharedPreferences("download_metadata", Context.MODE_PRIVATE)
    override suspend fun enqueue(url: String, permitted: Boolean, wifiOnly: Boolean): Long = withContext(Dispatchers.IO) {
        require(permitted) { "Confirm the source permits you to download this audio." }
        val uri = validateAudioDownload(url)
        val extension = uri.path.substringAfterLast('.').lowercase()
        enqueueFile(uri.toASCIIString(), uri.path.substringAfterLast('/'), extension, wifiOnly)
    }
    internal suspend fun enqueueAuthorized(audio: AuthorizedAudio, wifiOnly: Boolean): Long = withContext(Dispatchers.IO) {
        val id = enqueueFile(audio.url, audio.title, audio.extension, wifiOnly)
        if (!metadata.edit().putString(id.toString(), org.json.JSONObject().put("title", audio.title).put("artist", audio.artist).toString()).commit()) {
            manager.remove(id)
            error("Could not save download metadata.")
        }
        id
    }
    private fun enqueueFile(url: String, title: String, extension: String, wifiOnly: Boolean): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title.take(100))
            .setDescription("Authorized audio download for Saz")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, "${UUID.randomUUID()}.$extension")
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
        if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        return manager.enqueue(request)
    }
    override suspend fun downloads(): List<AudioDownload> = withContext(Dispatchers.IO) {
        buildList {
            manager.query(DownloadManager.Query())?.use { cursor ->
                while (cursor.moveToNext()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    val detail = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> "Ready to import"
                        DownloadManager.STATUS_RUNNING -> "Downloading"
                        DownloadManager.STATUS_PENDING -> "Waiting to download"
                        DownloadManager.STATUS_PAUSED -> if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) "Waiting for Wi-Fi" else "Waiting for network or retry"
                        else -> when (reason) {
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage"
                            DownloadManager.ERROR_HTTP_DATA_ERROR, DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server or network error"
                            401, 403 -> "Source denied access"
                            404 -> "Audio file not found"
                            else -> "Download failed (code $reason)"
                        }
                    }
                    add(AudioDownload(cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)).orEmpty(), status,
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)), detail))
                }
            }
        }
    }
    suspend fun importCompleted(): Int = withContext(Dispatchers.IO) {
        var count = 0
        downloads().filter { it.status == DownloadManager.STATUS_SUCCESSFUL }.forEach { download ->
            val uri = manager.getUriForDownloadedFile(download.id) ?: return@forEach
            count += library.import(listOf(uri))
            val track = library.dao.track(uri.toString()) ?: return@forEach
            val saved = metadata.getString(download.id.toString(), null)?.let { org.json.JSONObject(it) }
            if (saved != null) library.dao.updateMetadata(track.uri, saved.getString("title"), saved.getString("artist"),
                track.album, track.durationMs, track.artworkUri)
        }
        count
    }
    override suspend fun cancel(id: Long) = withContext(Dispatchers.IO) { manager.remove(id); metadata.edit().remove(id.toString()).apply(); Unit }
}

internal fun validateAudioDownload(value: String): URI {
    val uri = try { URI(value.trim()) } catch (_: java.net.URISyntaxException) { throw IllegalArgumentException("Enter a valid HTTPS audio URL.") }
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) { "Use a direct HTTPS audio URL without embedded credentials." }
    val host = uri.host.lowercase()
    require(listOf("youtube.com", "youtu.be", "googlevideo.com", "spotify.com", "scdn.co").none { host == it || host.endsWith(".$it") }) {
        "YouTube and Spotify are played by their providers; their audio cannot be downloaded here."
    }
    require(uri.path.orEmpty().substringAfterLast('.').lowercase() in setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "aiff")) {
        "Use a direct audio-file link, such as .mp3, .m4a, .flac or .wav. Web pages and video links are not supported."
    }
    return uri
}
