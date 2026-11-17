package app.saz.music.designsystem

import android.app.DownloadManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saz.music.providers.*
import coil.compose.AsyncImage

@Composable internal fun FreeSearchControls(model: ProviderViewModel, query: String, downloads: () -> Unit) {
    val state by model.freeState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Independent music, yours offline", style = MaterialTheme.typography.titleLarge)
        Text("Artist-enabled free downloads from Audius. Save over Wi-Fi, then import into your library for ad-free local playback. This catalogue does not include every commercial release.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        Button(enabled = !state.loading && query.isNotBlank() && query.length <= 200, onClick = { model.searchFree(query) }) { Text("Find free downloads") }
        TextButton(onClick = downloads) { Text("Manage downloads") }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.problem?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
        if (state.searched && !state.loading && state.problem == null && state.items.isEmpty())
            Text("No free downloads on this page. Try more results or another artist.")
    }
}

@Composable internal fun FreeDownloadRow(item: MusicItem, model: ProviderViewModel) {
    val saving by model.saving.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(item.artwork, null, Modifier.size(56.dp), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text("Audius · ${item.artist}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        TextButton(enabled = !saving, onClick = { model.downloadFree(item) }) { Text("Save") }
    }
}

@Composable internal fun OnlineSearchControls(model: ProviderViewModel, query: String, region: String, settings: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var recent by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!recent, { recent = false }, { Text("Relevant") })
            FilterChip(recent, { recent = true }, { Text("Recent uploads") })
        }
        TextButton(onClick = settings) { Text(if (region.isBlank()) "All available regions · change" else "Region: $region · change") }
        Button(enabled = !state.loading && query.length <= 200, onClick = { model.search(query, recent, region) }) { Text("Search YouTube") }
        Text("Search music or paste a public YouTube playlist link. Video availability varies by country; recent uploads do not include every new release.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.problem?.let {
            Text(it.message, color = MaterialTheme.colorScheme.error)
            if (it.kind == ProblemKind.CONFIGURATION || it.kind == ProblemKind.AUTHENTICATION) TextButton(onClick = settings) { Text("Provider settings") }
            else TextButton(onClick = model::retry, enabled = !state.loading) { Text("Retry search") }
        }
        if (!state.loading && state.searched && state.problem == null && state.items.isEmpty()) Text("No YouTube videos matched. Try another search or region.")
        if (!state.searched) Text("Search runs when you tap the button, conserving your API quota.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
    }
}

@Composable internal fun YouTubeResultRow(item: MusicItem, play: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = play).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(item.artwork, null, Modifier.size(width = 96.dp, height = 64.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text("YouTube · ${item.artist}", maxLines = 2, overflow = TextOverflow.Ellipsis, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            item.publishedAt?.take(10)?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable internal fun ProviderSettings(model: ProviderViewModel, region: String, importKey: () -> Unit) {
    val links = LocalUriHandler.current
    val configured by model.configured.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    var key by remember { mutableStateOf("") }
    var country by remember(region) { mutableStateOf(region) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("YouTube discovery", style = MaterialTheme.typography.titleLarge)
        Text(if (configured) "API key stored securely on this device" else "API key needed for discovery", color = SecondaryText)
        Text("Use a developer key with YouTube Data API v3 enabled. This does not sign in to a YouTube account.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(key, { if (it.length <= 100) key = it }, label = { Text("YouTube API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(enabled = !saving && key.isNotBlank(), onClick = { model.configure(key); key = "" }) { Text("Save key") }
            TextButton(enabled = !saving, onClick = importKey) { Text("Import key file") }
            if (configured) TextButton(enabled = !saving, onClick = model::clearKey) { Text("Remove key") }
        }
        OutlinedTextField(country, { if (it.length <= 2) country = it.uppercase() }, label = { Text("Country code (optional)") },
            supportingText = { Text("For example IN, US or JP. Blank searches all available regions.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(enabled = !saving, onClick = { model.region(country) }) { Text("Save region") }
        Text("Spotify currently opens in its own app or browser. It is not connected to Saz.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        Text("Online searches share your query and selected region with YouTube. Embedded playback uses YouTube's network and cookies. Lyric lookup sends song metadata to LRCLIB.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick = { try { links.openUri("https://www.youtube.com/t/terms") } catch (_: IllegalArgumentException) { model.notice.value = "No browser is available." } }) { Text("YouTube terms") }
            TextButton(onClick = { try { links.openUri("https://policies.google.com/privacy") } catch (_: IllegalArgumentException) { model.notice.value = "No browser is available." } }) { Text("Google privacy") }
        }
    }
}

@Composable internal fun DownloadsPanel(model: ProviderViewModel) {
    val downloads by model.downloads.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }
    var permitted by remember { mutableStateOf(false) }
    var wifiOnly by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Download audio", style = MaterialTheme.typography.titleLarge)
        Text("Use a direct audio link supplied by an artist, label or another source that permits downloading. YouTube and Spotify streams are not download sources.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(url, { if (it.length <= 4096) url = it }, label = { Text("HTTPS audio-file URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(permitted, { permitted = it }); Text("The source permits me to download this audio", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(wifiOnly, { wifiOnly = it }); Text("Wi-Fi only") }
        Button(enabled = permitted && url.isNotBlank() && !saving, onClick = { model.download(url, permitted, wifiOnly) }) { Text("Download") }
        if (downloads.isNotEmpty()) {
            TextButton(enabled = !saving && downloads.any { it.status == DownloadManager.STATUS_SUCCESSFUL }, onClick = model::importDownloads) { Text("Import completed audio") }
            downloads.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.detail, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        if (entry.status == DownloadManager.STATUS_RUNNING && entry.total > 0)
                            LinearProgressIndicator(progress = { (entry.downloaded.toFloat() / entry.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (entry.status != DownloadManager.STATUS_SUCCESSFUL) TextButton(onClick = { model.cancelDownload(entry.id) }) { Text("Cancel") }
                }
            }
        }
    }
}
