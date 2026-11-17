package app.saz.music

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import app.saz.music.designsystem.SazApp
import app.saz.music.integrations.openMusicSearch
import app.saz.music.providers.ProviderViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    private val providers: ProviderViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent {
            val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                uris.forEach { uri -> runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                model.import(uris)
            }
            val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    model.folder(uri)
                }
            }
            val lyrics = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.lyrics(uri) }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) model.export(uri) }
            val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.restore(uri) }
            val apiKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) providers.importKey(uri) }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) model.scan() else model.message.value = "Permission declined. You can still import files or a folder."
            }
            SazApp(model, providers, onImportApiKey = { apiKey.launch(arrayOf("text/*", "application/octet-stream")) },
                onImport = { files.launch(arrayOf("audio/*")) }, onFolder = { folder.launch(null) },
                onScan = { permission.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) },
                onLyrics = { lyrics.launch(arrayOf("text/*", "application/octet-stream")) },
                onExport = { export.launch("saz-backup.json") }, onRestore = { restore.launch(arrayOf("application/json")) },
                onMusicSearch = { destination, query ->
                    if (!openMusicSearch(destination, query)) {
                        model.message.value = "Could not open ${destination.displayName}. Install its app or enable a browser, then try again."
                    }
                })
        }
    }
}
