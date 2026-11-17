package app.saz.music.providers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyStore @Inject constructor(@ApplicationContext private val context: Context) : ProviderCredentials {
    private val file get() = File(context.filesDir, "youtube-key.encrypted")
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("saz-youtube-key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("saz-youtube-key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun youtubeKey(): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        val bytes = AtomicFile(file).readFully()
        require(bytes.size >= 28) { "Stored API key is unreadable. Import it again." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }
    suspend fun save(value: String) = withContext(Dispatchers.IO) {
        require(Regex("AIza[0-9A-Za-z_-]{35}").matches(value.trim())) { "Enter a valid Google API key." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(cipher.iv + cipher.doFinal(value.trim().toByteArray())); atomic.finishWrite(output) }
        catch (e: Exception) { atomic.failWrite(output); throw e }
    }
    suspend fun clear() = withContext(Dispatchers.IO) { AtomicFile(file).delete() }
}

class AndroidClientIdentity @Inject constructor(@ApplicationContext private val context: Context) {
    @Suppress("DEPRECATION")
    fun headers(): Map<String, String> {
        val info = context.packageManager.getPackageInfo(context.packageName,
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        val signature = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners?.firstOrNull() else info.signatures?.firstOrNull()
        return buildMap {
            put("X-Android-Package", context.packageName)
            signature?.let { put("X-Android-Cert", MessageDigest.getInstance("SHA-1").digest(it.toByteArray()).joinToString("") { byte -> "%02X".format(byte) }) }
        }
    }
}
