package app.saz.music.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saz.music.MainViewModel
import app.saz.music.R
import app.saz.music.integrations.MusicDestination
import app.saz.music.providers.ProviderViewModel
import app.saz.music.database.*
import app.saz.music.lyrics.Lrc
import app.saz.music.playback.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private val panelBorder get() = BorderStroke(1.dp, Outline)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SazApp(model: MainViewModel, providers: ProviderViewModel, onImportApiKey: () -> Unit,
    onImport: () -> Unit, onFolder: () -> Unit, onScan: () -> Unit, onLyrics: () -> Unit,
    onExport: () -> Unit, onRestore: () -> Unit,
    onMusicSearch: (MusicDestination, String) -> Unit) {
    val tracks by model.tracks.collectAsStateWithLifecycle()
    val playlists by model.playlists.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val playback by model.playback.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val lyrics by model.lyricText.collectAsStateWithLifecycle()
    val lyricsSource by model.lyricsSource.collectAsStateWithLifecycle()
    val onlineState by providers.state.collectAsStateWithLifecycle()
    val onlineNotice by providers.notice.collectAsStateWithLifecycle()
    val video by providers.selectedVideo.collectAsStateWithLifecycle()
    var onlineSearch by rememberSaveable { mutableStateOf(false) }
    var freeSearch by rememberSaveable { mutableStateOf(false) }
    val freeState by providers.freeState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val heroSize = minOf(320.dp, (configuration.screenWidthDp - 48).dp * .86f, (configuration.screenHeightDp * .30f).dp)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var full by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Songs") }
    var selectedPlaylist by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Track?>(null) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(query) { delay(250); debounced = query }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); model.message.value = null } }
    LaunchedEffect(onlineNotice) { onlineNotice?.let { snackbar.showSnackbar(it); providers.notice.value = null } }
    val play = { list: List<Track>, index: Int -> model.playback.play(list, index) }
    val openMood: (String) -> Unit = { name ->
        val existing = playlists.find { it.name == name }
        if (existing == null) model.createPlaylist(name)
        tab = 2; filter = "Playlists"; selectedPlaylist = existing?.id
    }
    BackHandler(enabled = tab == 4 && !full) { tab = 0 }
    MaterialTheme(colorScheme = WalnutPalette, typography = WalnutTypography, shapes = WalnutShapes) {
        Scaffold(containerColor = Mineral, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            Column(Modifier.background(Mineral).navigationBarsPadding()) {
                if (playback.uri != null) MiniPlayer(playback, model.playback, tracks.find { it.uri == playback.uri }) { full = true }
                HorizontalDivider(color = Outline)
                Row(Modifier.fillMaxWidth()) {
                    listOf("Home" to Icons.Rounded.Home, "Search" to Icons.Rounded.Search,
                        "Library" to Icons.Rounded.LibraryMusic, "Discover" to Icons.Rounded.Explore).forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index; selectedPlaylist = null },
                            icon = { Icon(item.second, null) }, label = { Text(item.first, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = SurfaceMuted,
                                selectedIconColor = Plum, selectedTextColor = Plum, unselectedIconColor = SecondaryText, unselectedTextColor = SecondaryText))
                    }
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = WalnutLayout.gutter, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (tab == 0) "saz" else listOf("Home", "Search", "Your library", "Discover", "Settings")[tab],
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                            if (tab == 4) IconButton(onClick = { tab = 0 }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to home") }
                            else IconButton(onClick = { tab = 4 }) { Icon(Icons.Rounded.Settings, "Settings") }
                        }
                    }
                    if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Plum) }
                    if (playback.error != null) item { Text(playback.error!!, color = MaterialTheme.colorScheme.error) }
                    when (tab) {
                        0 -> {
                            item { Text("Stay a little\nlonger.", style = MaterialTheme.typography.displaySmall) }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Artwork(null, Modifier.size(heroSize))
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Saz Flow", style = MaterialTheme.typography.titleLarge)
                                            Text(if (tracks.isEmpty()) "A new beginning" else "A fresh mix of your music", color = SecondaryText)
                                        }
                                        Button(enabled = tracks.isEmpty() || playback.connected,
                                            onClick = { if (tracks.isEmpty()) onImport() else play(tracks.shuffled(), 0) }) {
                                            Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp))
                                            Text(if (tracks.isEmpty()) "Add music" else "Play mix")
                                        }
                                    }
                                }
                            }
                            if (tracks.isEmpty()) item { ImportCard(onImport, onFolder, onScan) }
                            else {
                                val recent = tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(10)
                                if (recent.isNotEmpty()) {
                                    item { Heading("Recent tracks") }
                                    items(recent.take(4), key = { "recent" + it.uri }) { track -> TrackRow(track, playback.uri == track.uri, { play(recent, recent.indexOf(track)) }, { model.favorite(track) }, { adding = track }) }
                                }
                                val favorites = tracks.filter { it.favorite }
                                if (favorites.isNotEmpty()) {
                                    item { Heading("Close to your heart") }
                                    item { Shelf(favorites) { play(favorites, it) } }
                                }
                                item { Heading("Your collection", "${tracks.size} songs") }
                                items(tracks.take(8), key = { "home" + it.uri }) { track ->
                                    TrackRow(track, playback.uri == track.uri, { play(tracks, tracks.indexOf(track)) }, { model.favorite(track) }, { adding = track })
                                }
                            }
                            item { Heading("Find your mood") }
                            item { MoodGrid(openMood) }
                        }
                        1 -> {
                            item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), shape = WalnutLayout.panelShape,
                                label = { Text("Songs, artists, albums") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true) }
                            item { FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(!onlineSearch && !freeSearch, { onlineSearch = false; freeSearch = false }, { Text("Local") })
                                FilterChip(freeSearch, { freeSearch = true; onlineSearch = false }, { Text("Free downloads") })
                                FilterChip(onlineSearch, { onlineSearch = true; freeSearch = false }, { Text("YouTube") })
                                AssistChip(onClick = { onMusicSearch(MusicDestination.Spotify, query) }, label = { Text("Open Spotify") },
                                    trailingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(16.dp)) })
                            } }
                            if (freeSearch) {
                                item { FreeSearchControls(providers, query) { tab = 4 } }
                                items(freeState.items, key = { "audius" + it.key.value }) { item -> FreeDownloadRow(item, providers) }
                                if (freeState.nextPage != null) item { TextButton(enabled = !freeState.loading, onClick = { providers.searchFree(query, next = true) }) { Text("More results") } }
                            } else if (onlineSearch) {
                                item { OnlineSearchControls(providers, query, settings.region) { tab = 4 } }
                                items(onlineState.items, key = { "yt" + it.key.value }) { item -> YouTubeResultRow(item) { providers.openVideo(item) } }
                                if (onlineState.nextPage != null) item { TextButton(enabled = !onlineState.loading,
                                    onClick = { providers.search(query, false, settings.region, next = true) }) { Text("More YouTube videos") } }
                            } else {
                            item { Text("Your saved music, ready offline.", color = SecondaryText, fontSize = 13.sp) }
                            val results = tracks.filter { listOf(it.title, it.artist, it.album).any { value -> value.contains(debounced.trim(), true) } }
                            item { Text("On this device · ${results.size} results", fontSize = 13.sp, color = SecondaryText) }
                            if (results.isEmpty()) item { Empty("Nothing here yet", "Import your audio files, or try a different search.") }
                            items(results, key = { "search" + it.uri }) { track ->
                                TrackRow(track, playback.uri == track.uri, { play(results, results.indexOf(track)) }, { model.favorite(track) }, { adding = track })
                            }
                            }
                        }
                        2 -> {
                            item { Text("${tracks.size} ${if (tracks.size == 1) "song" else "songs"}, always with you", color = SecondaryText, fontSize = 13.sp) }
                            item {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Songs", "Favorites", "Albums", "Artists", "Playlists").forEach { label ->
                                        FilterChip(filter == label, { filter = label; selectedPlaylist = null }, { Text(label) })
                                    }
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = onImport) { Text("Import files") }
                                    TextButton(onClick = onScan) { Text("Scan device") }
                                }
                            }
                            if (filter == "Playlists") {
                                item { Button(onClick = { creating = true }) { Icon(Icons.Rounded.Add, null); Text("New playlist") } }
                                if (selectedPlaylist == null) {
                                    items(playlists, key = { "playlist${it.id}" }) { playlist ->
                                        Surface(onClick = { selectedPlaylist = playlist.id }, shape = WalnutLayout.panelShape, color = SurfaceMuted) {
                                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Plum)
                                                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                                    Text(playlist.name); Text("${entries.count { it.playlistId == playlist.id }} tracks", color = SecondaryText, fontSize = 13.sp)
                                                }
                                                IconButton(onClick = { editingPlaylist = playlist }) { Icon(Icons.Rounded.Edit, "Rename playlist") }
                                                IconButton(onClick = { deletingPlaylist = playlist }) { Icon(Icons.Rounded.DeleteOutline, "Delete playlist") }
                                            }
                                        }
                                    }
                                } else {
                                    val byUri = tracks.associateBy { it.uri }
                                    val list = entries.filter { it.playlistId == selectedPlaylist }.mapNotNull { byUri[it.trackUri] }
                                    item { TextButton(onClick = { selectedPlaylist = null }) { Text("All playlists") } }
                                    if (list.isEmpty()) item { Empty("Make it yours", "Use the + beside any song to add it to this playlist.") }
                                    items(list, key = { "pl" + it.uri }) { track -> TrackRow(track, playback.uri == track.uri, { play(list, list.indexOf(track)) }, { model.favorite(track) }, { adding = track }) }
                                }
                            } else if (filter == "Albums" || filter == "Artists") {
                                val groups = tracks.groupBy { if (filter == "Albums") it.album else it.artist }
                                groups.forEach { (name, list) ->
                                    item(key = "group$name") { Heading(name, "${list.size} songs") }
                                    items(list, key = { "group" + it.uri }) { track -> TrackRow(track, playback.uri == track.uri, { play(list, list.indexOf(track)) }, { model.favorite(track) }, { adding = track }) }
                                }
                            } else {
                                val list = if (filter == "Favorites") tracks.filter { it.favorite } else tracks
                                if (list.isEmpty()) item { Empty("Room for your favorites", "Import music, then tap a heart to save a favorite.") }
                                items(list, key = { "library" + it.uri }) { track -> TrackRow(track, playback.uri == track.uri, { play(list, list.indexOf(track)) }, { model.favorite(track) }, { adding = track }) }
                            }
                        }
                        3 -> {
                            item {
                                Button(onClick = { onlineSearch = true; freeSearch = false; tab = 1; query = ""; providers.search("", true, settings.region) }) { Text("Discover music on YouTube") }
                            }
                            item {
                                ScenicCollection("Kashmiri classics", "Build your own sounds of the Valley", Modifier.fillMaxWidth().heightIn(min = 160.dp)) { openMood("Kashmiri Classics") }
                            }
                            item { Heading("For your next moment") }
                            item { Text("Flow gives favorites and familiar artists a little more space. Every song comes from your library.", color = SecondaryText, fontSize = 13.sp) }
                            item {
                                Button(enabled = tracks.isNotEmpty() && playback.connected, onClick = {
                                    val favoriteArtists = tracks.filter { it.favorite }.map { it.artist }.toSet()
                                    val flow = tracks.shuffled().sortedByDescending { (if (it.favorite) 3 else 0) + (if (it.artist in favoriteArtists) 1 else 0) - (if (it.uri == playback.uri) 5 else 0) }
                                    play(flow, 0)
                                }) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Start Flow") }
                            }
                            item { MoodGrid(openMood) }
                            item { Heading("Collections") }
                            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(listOf("Kashmiri", "Sufi", "Folk"), listOf("Indie", "Pop", "Instrumental")).forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { label ->
                                        OutlinedButton(onClick = { openMood(label) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), border = panelBorder) { Text(label, fontSize = 13.sp) }
                                    } }
                                }
                            } }
                        }
                        4 -> {
                            item { ProviderSettings(providers, settings.region, onImportApiKey) }
                            item { DownloadsPanel(providers) }
                            item { ImportCard(onImport, onFolder, onScan) }
                            item {
                                WalnutPanel {
                                    Text("Listening history", fontWeight = FontWeight.SemiBold)
                                    Text("Stored only on this device. Turning it off clears saved listening times.", color = SecondaryText, fontSize = 13.sp)
                                    Switch(settings.history, { model.history(it) })
                                }
                            }
                            item {
                                WalnutPanel {
                                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Balanced", "Performance").forEach { mode -> FilterChip(settings.graphics == mode, { model.graphics(mode) }, { Text(mode) }) }
                                    }
                                    Text("Walnut uses opaque surfaces and static artwork. Both presets avoid live blur.", color = SecondaryText, fontSize = 13.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Reduced motion", Modifier.weight(1f)); Switch(settings.reducedMotion, { model.motion(it) }) }
                                }
                            }
                            item {
                                WalnutPanel {
                                    Text("Backup your collection", fontWeight = FontWeight.SemiBold)
                                    Text("Export songs metadata, favorites, playlists and enabled history. Import merges tracks and adds playlists; it does not copy audio or transfer file permissions.", color = SecondaryText, fontSize = 13.sp)
                                    Row { TextButton(onClick = onExport) { Text("Export") }; TextButton(onClick = onRestore) { Text("Import & merge") } }
                                }
                            }
                            item {
                                WalnutPanel {
                                    Text("Connected services", fontWeight = FontWeight.SemiBold)
                                    Text("Local audio uses Saz's player. YouTube discovery uses your API key and playback uses the visible YouTube player. Spotify opens externally. No provider account is signed in.", color = SecondaryText, fontSize = 13.sp)
                                }
                            }
                            item { Text("Saz 0.1.0 · Local-first preview\nNo Saz account or analytics. Online providers receive the requests you initiate.", color = SecondaryText, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
        if (creating) {
            var name by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { creating = false }, title = { Text("New playlist") },
                text = { OutlinedTextField(name, { if (it.length <= 100) name = it }, label = { Text("Name") }) },
                confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { model.createPlaylist(name); creating = false }) { Text("Create") } },
                dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } })
        }
        adding?.let { track ->
            AlertDialog(onDismissRequest = { adding = null }, title = { Text(track.title, maxLines = 2) }, text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { model.playback.enqueue(track); adding = null }) { Text("Add to playback queue") }
                    selectedPlaylist?.let { id -> TextButton(onClick = { model.remove(id, track); adding = null }) { Text("Remove from this playlist") } }
                    Text("Add to playlist", color = SecondaryText, fontSize = 13.sp)
                    if (playlists.isEmpty()) Text("Create a playlist in Library first.", color = SecondaryText)
                    playlists.forEach { playlist -> TextButton(onClick = { model.add(playlist, track); adding = null }) { Text(playlist.name) } }
                }
            }, confirmButton = { TextButton(onClick = { adding = null }) { Text("Done") } })
        }
        editingPlaylist?.let { playlist ->
            var name by remember(playlist.id) { mutableStateOf(playlist.name) }
            AlertDialog(onDismissRequest = { editingPlaylist = null }, title = { Text("Rename playlist") },
                text = { OutlinedTextField(name, { if (it.length <= 100) name = it }) },
                confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { model.rename(playlist, name); editingPlaylist = null }) { Text("Save") } },
                dismissButton = { TextButton(onClick = { editingPlaylist = null }) { Text("Cancel") } })
        }
        deletingPlaylist?.let { playlist ->
            AlertDialog(onDismissRequest = { deletingPlaylist = null }, title = { Text("Delete ${playlist.name}?") },
                text = { Text("This removes the playlist. Your audio files and favorites stay in your library.") },
                confirmButton = { TextButton(onClick = { model.delete(playlist); deletingPlaylist = null }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { deletingPlaylist = null }) { Text("Cancel") } })
        }
        if (full) FullPlayer(playback, model.playback, tracks.find { it.uri == playback.uri }, lyrics, lyricsSource, settings.reducedMotion, onLyrics, model::findLyrics, { tracks.find { it.uri == playback.uri }?.let(model::favorite) }) { full = false }
        video?.let { YouTubePlayer(it, providers::closeVideo) }
    }
}

@Composable private fun Heading(title: String, subtitle: String? = null) {
    Column { Text(title, style = MaterialTheme.typography.titleLarge); subtitle?.let { Text(it, color = SecondaryText, fontSize = 13.sp) } }
}
@Composable private fun MoodGrid(open: (String) -> Unit) {
    Column {
        listOf("Calm", "Focus", "Rainy evening", "Night drive").forEachIndexed { index, name ->
            val existingName = listOf("Calm", "Focus", "Rainy Evening", "Night Drive")[index]
            Row(Modifier.fillMaxWidth().clickable { open(existingName) }.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Open $name", tint = Plum)
            }
            HorizontalDivider(color = Outline)
        }
    }
}
@Composable private fun ScenicCollection(title: String, subtitle: String?, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, color = SurfaceMuted, shape = WalnutLayout.panelShape) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                subtitle?.let { Text(it, color = SecondaryText) }
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Plum)
        }
    }
}
@Composable private fun PlaybackButton(modifier: Modifier = Modifier, playing: Boolean, label: String, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier.size(50.dp), shape = CircleShape, color = Plum, contentColor = Color.White) {
        Box(contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, label, modifier = Modifier.size(28.dp))
        }
    }
}
@Composable private fun Empty(title: String, detail: String) {
    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 22.sp, fontFamily = Editorial); Text(detail, color = SecondaryText)
    }
}
@Composable private fun WalnutPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = WalnutLayout.panelShape, color = Paper) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable private fun ImportCard(onImport: () -> Unit, onFolder: () -> Unit, onScan: () -> Unit) {
    WalnutPanel {
        Text("Bring your collection", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text("Choose audio files, add a music folder, or scan this device. Your library works offline.", color = SecondaryText, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImport) { Text("Files") }; OutlinedButton(onClick = onFolder) { Text("Folder") }; TextButton(onClick = onScan) { Text("Scan") }
        }
    }
}
@Composable private fun Artwork(track: Track?, modifier: Modifier = Modifier) {
    Box(modifier.clip(WalnutLayout.artworkShape).background(SurfaceMuted), contentAlignment = Alignment.Center) {
        AsyncImage(track?.artworkUri ?: R.drawable.walnut_art, null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit, error = painterResource(R.drawable.walnut_art))
    }
}
@Composable private fun Shelf(tracks: List<Track>, onPlay: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(tracks, key = { _, t -> t.uri }) { index, track ->
            Column(Modifier.width(148.dp).clickable { onPlay(index) }) {
                Artwork(track, Modifier.size(148.dp)); Spacer(Modifier.height(8.dp))
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(track.artist, color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
@Composable private fun TrackRow(track: Track, current: Boolean, play: () -> Unit, favorite: () -> Unit, more: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(WalnutLayout.artworkShape).background(if (current) SurfaceMuted else Color.Transparent)
        .clickable(onClick = play).padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, Modifier.size(52.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = if (current) Plum else Walnut, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = favorite) { Icon(if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (track.favorite) "Remove favorite" else "Favorite", tint = if (track.favorite) Plum else SecondaryText) }
        IconButton(onClick = more) { Icon(Icons.Rounded.Add, "Add to playlist or queue", tint = SecondaryText) }
    }
}
@Composable private fun MiniPlayer(state: PlaybackState, connection: PlaybackConnection, track: Track?, expand: () -> Unit) {
    Surface(onClick = expand, color = SurfaceMuted, contentColor = Walnut) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(track, Modifier.size(46.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Text(state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SecondaryText, fontSize = 13.sp)
                }
                PlaybackButton(Modifier, state.playing, if (state.playing) "Pause" else "Play", connection::toggle)
                IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next track") }
            }
            LinearProgressIndicator(progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp), color = Plum, trackColor = Color.Transparent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FullPlayer(state: PlaybackState, connection: PlaybackConnection, track: Track?, lyrics: String, lyricsSource: String, reducedMotion: Boolean, onLyrics: () -> Unit, findLyrics: () -> Unit, favorite: () -> Unit, close: () -> Unit) {
    var queue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    var ambient by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<Float?>(null) }
    val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) reveal.snapTo(1f) else reveal.animateTo(1f, tween(WalnutLayout.transitionMs))
    }
    BackHandler(onBack = close)
    androidx.compose.ui.window.Dialog(onDismissRequest = close, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WalnutDialogBars()
        Surface(Modifier.fillMaxSize(), color = Mineral, contentColor = Walnut) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val artworkSize = minOf(maxWidth - 48.dp, if (maxHeight < 720.dp) maxHeight * .28f else 380.dp)
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = reveal.value }.systemBarsPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player") }
                    Text(if (ambient) "Quiet listening" else "Now playing", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { ambient = !ambient }) { Icon(Icons.Rounded.NightsStay, "Toggle ambient mode") }
                }
                Spacer(Modifier.height(16.dp))
                if (!ambient) Artwork(track, Modifier.size(artworkSize))
                else Text("Just you,\nand the music.", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 64.dp))
                Spacer(Modifier.height(20.dp))
                Text(state.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Text(state.artist, color = SecondaryText, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                if (!ambient) {
                    Slider(modifier = Modifier.semantics { contentDescription = "Playback position" }, value = seek ?: state.position.toFloat().coerceAtMost(state.duration.toFloat()), onValueChange = { seek = it },
                        thumb = { Box(Modifier.size(18.dp).background(Plum, CircleShape)) },
                        track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp)) },
                        onValueChangeFinished = { seek?.let { connection.seek(it.toLong()) }; seek = null },
                        valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time(state.position), color = SecondaryText, fontSize = 13.sp); Text(time(state.duration), color = SecondaryText, fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (!ambient) IconButton(onClick = connection::shuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle ${if (state.shuffle) "on" else "off"}", tint = if (state.shuffle) Plum else SecondaryText) }
                    IconButton(onClick = connection::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(32.dp)) }
                    Surface(onClick = connection::toggle, modifier = Modifier.size(76.dp), shape = CircleShape, color = Plum, contentColor = Color.White) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play", Modifier.size(40.dp))
                        }
                    }
                    IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(32.dp)) }
                    if (!ambient) IconButton(onClick = connection::repeat) { Icon(if (state.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat ${listOf("off", "one", "all")[state.repeat]}", tint = if (state.repeat > 0) Plum else SecondaryText) }
                }
                if (!ambient) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = favorite) { Icon(if (track?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = Plum) }
                    TextButton(onClick = { showLyrics = true }) { Text("Lyrics") }
                    TextButton(onClick = { queue = true }) { Text("Queue") }
                    IconButton(onClick = { audio = true }) { Icon(Icons.Rounded.Tune, "Audio and sleep timer") }
                }
                if (ambient) TextButton(onClick = { audio = true }) { Text("Sleep timer") }
            }
            }
        }
    }
    if (queue) ModalBottomSheet(onDismissRequest = { queue = false }, containerColor = SurfaceMuted) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 550.dp), contentPadding = PaddingValues(20.dp)) {
            item { Heading("Up next", "${state.queue.size} tracks") }
            itemsIndexed(state.queue, key = { index, _ -> index }) { index, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { connection.jump(index) }, modifier = Modifier.weight(1f)) {
                        Text(item.second, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (index == state.index) Plum else Walnut)
                    }
                    if (index > 0) IconButton(onClick = { connection.move(index, index - 1) }) { Icon(Icons.Rounded.ArrowUpward, "Move up") }
                    IconButton(onClick = { connection.remove(index) }) { Icon(Icons.Rounded.Close, "Remove from queue") }
                }
            }
        }
    }
    if (audio) AlertDialog(onDismissRequest = { audio = false }, title = { Text("Listening settings") }, text = {
        Column {
            Text("Playback speed")
            Row { listOf(.75f, 1f, 1.25f, 1.5f).forEach { speed -> TextButton(onClick = { connection.speed(speed) }) { Text("${speed}×") } } }
            Text("Sleep timer")
            listOf(0, 15, 30, 60).forEach { minutes -> TextButton(onClick = { connection.sleep(minutes); audio = false }) { Text(if (minutes == 0) "Cancel timer" else "$minutes minutes") } }
            Text("Use Android's media controls to choose an available audio output.", color = SecondaryText, fontSize = 13.sp)
        }
    }, confirmButton = { TextButton(onClick = { audio = false }) { Text("Done") } })
    if (showLyrics) androidx.compose.ui.window.Dialog(onDismissRequest = { showLyrics = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WalnutDialogBars()
        val lines = remember(lyrics) { Lrc.parse(lyrics) }
        val active = Lrc.activeIndex(lines, state.position)
        val listState = rememberLazyListState()
        var follow by remember { mutableStateOf(true) }
        var fontSize by remember { mutableIntStateOf(22) }
        LaunchedEffect(active) { if (follow && active >= 0 && !listState.isScrollInProgress) {
            if (reducedMotion) listState.scrollToItem(active) else listState.animateScrollToItem(active)
        } }
        Surface(Modifier.fillMaxSize(), color = Mineral, contentColor = Walnut) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 26.dp, vertical = 12.dp)) {
                    Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(false, { showLyrics = false }, { Text("Song") }); FilterChip(true, {}, { Text("Lyrics") })
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = findLyrics) { Text("Find on LRCLIB", fontSize = 13.sp) }
                        TextButton(onClick = onLyrics) { Text("Import lyrics", fontSize = 13.sp) }
                        TextButton(onClick = { follow = !follow }) { Text(if (follow) "Following" else "Manual", fontSize = 13.sp) }
                        TextButton(onClick = { fontSize = if (fontSize >= 32) 20 else fontSize + 2 }) { Text("Aa") }
                    }
                    if (lyricsSource.isNotBlank()) Text(lyricsSource, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 34.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                        if (lyrics.isBlank()) item { Empty("Every word, closer.", "Find on LRCLIB sends the song title, artist and duration for matching. You can also import a lyric file you are authorized to use. Timestamped lines follow the music.") }
                        else if (lines.isEmpty()) item { Text(lyrics, fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp) }
                        else itemsIndexed(lines) { index, line -> Text(line.text, fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp,
                            fontWeight = if (index == active) FontWeight.SemiBold else FontWeight.Normal, color = if (index == active) Walnut else SecondaryText) }
                    }
                    Slider(value = state.position.toFloat().coerceAtMost(state.duration.toFloat()), onValueChange = { connection.seek(it.toLong()) },
                        modifier = Modifier.semantics { contentDescription = "Playback position" },
                        thumb = { Box(Modifier.size(18.dp).background(Plum, CircleShape)) },
                        track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp)) },
                        valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(state.position), fontSize = 13.sp, color = SecondaryText); Text(time(state.duration), fontSize = 13.sp, color = SecondaryText) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = connection::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
                        PlaybackButton(Modifier, state.playing, if (state.playing) "Pause" else "Play", connection::toggle)
                        IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next") }
                    }
                }
            }
        }
    }
}
@Composable private fun WalnutDialogBars() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
}
private fun time(ms: Long): String { val seconds = ms / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }


