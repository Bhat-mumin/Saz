package app.saz.music.lyrics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class LyricsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private fun file(trackUri: String): File {
        val key = MessageDigest.getInstance("SHA-256").digest(trackUri.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "lyrics").apply { mkdirs() }, "$key.lrc")
    }
    suspend fun read(trackUri: String): String = withContext(Dispatchers.IO) {
        val source = file(trackUri)
        if (source.exists()) source.readText() else ""
    }
    suspend fun save(trackUri: String, text: String) = withContext(Dispatchers.IO) {
        val atomic = android.util.AtomicFile(file(trackUri))
        val output = atomic.startWrite()
        try { output.write(text.toByteArray()); atomic.finishWrite(output) }
        catch (e: Exception) { atomic.failWrite(output); throw e }
    }
}
