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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saz.music.MainViewModel
import app.saz.music.R
import app.saz.music.database.*
import app.saz.music.lyrics.Lrc
import app.saz.music.playback.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val Ink = Color(0xFF100E19)
private val Violet = Color(0xFFB9A2F6)
private val Ivory = Color(0xFFF8F6F0)
private val Muted = Color(0xFFA5A5B2)
private val Panel = Color(0xFF252031)
private val glassBorder get() = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFD8B7F7).copy(alpha = .65f), Violet.copy(alpha = .12f), Color(0xFFE5C8EE).copy(alpha = .35f))))
private val palette = darkColorScheme(primary = Violet, onPrimary = Ink, background = Ink, surface = Panel,
    onBackground = Ivory, onSurface = Ivory, secondary = Violet, surfaceVariant = Color(0xFF292733))

@Composable
fun SazApp(model: MainViewModel, onImport: () -> Unit, onFolder: () -> Unit, onScan: () -> Unit, onLyrics: () -> Unit,
    onExport: () -> Unit, onRestore: () -> Unit) {
    val tracks by model.tracks.collectAsStateWithLifecycle()
    val playlists by model.playlists.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val playback by model.playback.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val lyrics by model.lyricText.collectAsStateWithLifecycle()
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
    val play = { list: List<Track>, index: Int -> model.playback.play(list, index) }
    val openMood: (String) -> Unit = { name ->
        val existing = playlists.find { it.name == name }
        if (existing == null) model.createPlaylist(name)
        tab = 2; filter = "Playlists"; selectedPlaylist = existing?.id
    }
    MaterialTheme(colorScheme = palette) {
        Scaffold(containerColor = Ink, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp)) {
                if (playback.uri != null) MiniPlayer(playback, model.playback, tracks.find { it.uri == playback.uri }) { full = true }
                Surface(color = Panel.copy(alpha = if (settings.graphics == "Performance") 1f else .94f),
                    shape = RoundedCornerShape(28.dp), border = glassBorder,
                    modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Violet.copy(alpha = .14f), Color.Transparent, Violet.copy(alpha = .06f))))) {
                        listOf("Home" to Icons.Rounded.Home, "Search" to Icons.Rounded.Search, "Library" to Icons.Rounded.LibraryMusic,
                            "Discover" to Icons.Rounded.Explore, "Settings" to Icons.Rounded.Settings).forEachIndexed { index, item ->
                            NavigationBarItem(selected = tab == index, onClick = { tab = index; selectedPlaylist = null },
                                icon = { Icon(item.second, item.first) }, label = { Text(item.first, fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Violet.copy(alpha = .30f),
                                    selectedIconColor = Violet, selectedTextColor = Violet, unselectedIconColor = Muted, unselectedTextColor = Muted))
                        }
                    }
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AsyncImage(R.drawable.scene_valley, null, Modifier.fillMaxWidth().height(430.dp), contentScale = ContentScale.Crop, alpha = .13f)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF211B30).copy(alpha = .4f), Ink.copy(alpha = .93f), Ink))))
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                Text(if (tab == 0) when (hour) { in 5..11 -> "Good Morning"; in 12..16 -> "Good Afternoon"; else -> "Good Evening" }
                                    else listOf("Home", "Search", "Your Library", "Discover", "Settings")[tab], fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (tab == 0) "What are we listening to today?" else "Music beyond borders", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Image(painterResource(R.drawable.saz_icon), "Saz", Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)))
                        }
                    }
                    if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Violet) }
                    if (playback.error != null) item { Text(playback.error!!, color = MaterialTheme.colorScheme.error) }
                    when (tab) {
                        0 -> {
                            item {
                                Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, border = glassBorder) {
                                    Box(Modifier.fillMaxWidth().height(172.dp).background(Brush.linearGradient(listOf(Color(0xFF73548B), Color(0xFF37274E), Color(0xFF82617E))))) {
                                        AsyncImage(R.drawable.scene_rain, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .18f)
                                        Column(Modifier.padding(22.dp).align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.AllInclusive, null, tint = Color(0xFFE0B4FF), modifier = Modifier.size(36.dp))
                                            Text("Saz Flow", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                            Text(if (tracks.isEmpty()) "Your music, a new beginning" else "A continuous mix, just for you", color = Ivory.copy(alpha = .82f), fontSize = 12.sp)
                                        }
                                        GlassPlay(Modifier.align(Alignment.BottomEnd).padding(18.dp), false, "Start Saz Flow") { if (tracks.isEmpty()) onImport() else play(tracks.shuffled(), 0) }
                                    }
                                }
                            }
                            item { Heading("Mood Collections") }
                            item { MoodGrid(openMood) }
                            if (tracks.isEmpty()) item { ImportCard(onImport, onFolder, onScan) }
                            else {
                                val recent = tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(10)
                                if (recent.isNotEmpty()) {
                                    item { Heading("Recently Played") }
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
                        }
                        1 -> {
                            item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                                placeholder = { Text("Songs, artists, albums…") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true) }
                            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(true, {}, { Text("Local") })
                                AssistChip(onClick = { model.message.value = "Spotify requires account integration; this build searches local music." }, label = { Text("Spotify · not linked") })
                                AssistChip(onClick = { model.message.value = "YouTube discovery is not configured in this build." }, label = { Text("YouTube · not linked") })
                            } }
                            val results = tracks.filter { listOf(it.title, it.artist, it.album).any { value -> value.contains(debounced.trim(), true) } }
                            item { Text("ON THIS DEVICE · ${results.size} RESULTS", fontSize = 10.sp, letterSpacing = 1.sp, color = Muted) }
                            if (results.isEmpty()) item { Empty("Nothing here yet", "Import your audio files, or try a different search.") }
                            items(results, key = { "search" + it.uri }) { track ->
                                TrackRow(track, playback.uri == track.uri, { play(results, results.indexOf(track)) }, { model.favorite(track) }, { adding = track })
                            }
                        }
                        2 -> {
                            item { Text("${tracks.size} ${if (tracks.size == 1) "song" else "songs"}, always with you", color = Muted, fontSize = 13.sp) }
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
                                        Surface(onClick = { selectedPlaylist = playlist.id }, shape = RoundedCornerShape(18.dp), color = Panel) {
                                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Violet)
                                                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                                    Text(playlist.name); Text("${entries.count { it.playlistId == playlist.id }} tracks", color = Muted, fontSize = 12.sp)
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
                                ScenicCollection("Kashmiri Classics", "Build your own sounds of the Valley", R.drawable.scene_valley, Modifier.fillMaxWidth().height(190.dp)) { openMood("Kashmiri Classics") }
                            }
                            item { Heading("For your next moment") }
                            item { Text("Flow gives favorites and familiar artists a little more space. Every song comes from your library.", color = Muted, fontSize = 13.sp) }
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
                                        OutlinedButton(onClick = { openMood(label) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), border = glassBorder) { Text(label, fontSize = 11.sp) }
                                    } }
                                }
                            } }
                        }
                        4 -> {
                            item { ImportCard(onImport, onFolder, onScan) }
                            item {
                                GlassCard {
                                    Text("Listening history", fontWeight = FontWeight.SemiBold)
                                    Text("Stored only on this device. Turning it off clears saved listening times.", color = Muted, fontSize = 13.sp)
                                    Switch(settings.history, { model.history(it) })
                                }
                            }
                            item {
                                GlassCard {
                                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Balanced", "Performance").forEach { mode -> FilterChip(settings.graphics == mode, { model.graphics(mode) }, { Text(mode) }) }
                                    }
                                    Text("Static glass surfaces keep playback responsive. Real-time blur is not enabled in this build.", color = Muted, fontSize = 13.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Reduced motion", Modifier.weight(1f)); Switch(settings.reducedMotion, { model.motion(it) }) }
                                }
                            }
                            item {
                                GlassCard {
                                    Text("Backup your collection", fontWeight = FontWeight.SemiBold)
                                    Text("Export songs metadata, favorites, playlists and enabled history. Import merges tracks and adds playlists; it does not copy audio or transfer file permissions.", color = Muted, fontSize = 13.sp)
                                    Row { TextButton(onClick = onExport) { Text("Export") }; TextButton(onClick = onRestore) { Text("Import & merge") } }
                                }
                            }
                            item {
                                GlassCard {
                                    Text("Connected services", fontWeight = FontWeight.SemiBold)
                                    Text("Spotify and YouTube are not connected in this local-player build. No streaming or account data is simulated.", color = Muted, fontSize = 13.sp)
                                }
                            }
                            item { Text("Saz 0.1.0 · Local-first preview\nNo account. No analytics. Your music stays on your device.", color = Muted, fontSize = 12.sp) }
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
                    Text("ADD TO PLAYLIST", color = Muted, fontSize = 11.sp)
                    if (playlists.isEmpty()) Text("Create a playlist in Library first.", color = Muted)
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
        if (full) FullPlayer(playback, model.playback, tracks.find { it.uri == playback.uri }, lyrics, settings.reducedMotion, settings.graphics == "Performance", onLyrics, { tracks.find { it.uri == playback.uri }?.let(model::favorite) }) { full = false }
    }
}

@Composable private fun Heading(title: String, subtitle: String? = null) {
    Column { Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold); subtitle?.let { Text(it, color = Muted, fontSize = 12.sp) } }
}
@Composable private fun MoodGrid(open: (String) -> Unit) {
    val moods = listOf("Calm" to R.drawable.scene_calm, "Focus" to R.drawable.scene_focus,
        "Rainy Evening" to R.drawable.scene_rain, "Night Drive" to R.drawable.scene_night)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        moods.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { (name, art) ->
                ScenicCollection(name, null, art, Modifier.weight(1f).height(126.dp)) { open(name) }
            } }
        }
    }
}
@Composable private fun ScenicCollection(title: String, subtitle: String?, art: Int, modifier: Modifier, click: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).border(glassBorder, RoundedCornerShape(22.dp)).clickable(onClick = click)) {
        AsyncImage(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .18f), Ink.copy(alpha = .85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(title, fontSize = if (subtitle == null) 16.sp else 23.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = Ivory.copy(alpha = .8f), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
        }
    }
}
@Composable private fun GlassPlay(modifier: Modifier = Modifier, playing: Boolean, label: String, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier.size(50.dp), shape = CircleShape, color = Color.Transparent, border = glassBorder) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFFAC84B8).copy(alpha = .75f), Color(0xFF554365).copy(alpha = .8f)))), contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, label, tint = Ivory, modifier = Modifier.size(28.dp))
        }
    }
}
@Composable private fun Empty(title: String, detail: String) {
    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 22.sp, fontFamily = FontFamily.Serif); Text(detail, color = Muted)
    }
}
@Composable private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Panel.copy(alpha = .92f), border = glassBorder) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Violet.copy(alpha = .09f), Color.Transparent))).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable private fun ImportCard(onImport: () -> Unit, onFolder: () -> Unit, onScan: () -> Unit) {
    GlassCard {
        Text("Bring your collection", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text("Choose audio files, add a music folder, or scan this device. Your library works offline.", color = Muted, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImport) { Text("Files") }; OutlinedButton(onClick = onFolder) { Text("Folder") }; TextButton(onClick = onScan) { Text("Scan") }
        }
    }
}
@Composable private fun Artwork(track: Track?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF40315B), Color(0xFF201D2D)))), contentAlignment = Alignment.Center) {
        if (track?.artworkUri != null) AsyncImage(track.artworkUri, "Album artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, error = painterResource(R.drawable.scene_valley))
        else AsyncImage(R.drawable.scene_valley, "Saz scenic placeholder artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
@Composable private fun Shelf(tracks: List<Track>, onPlay: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(tracks, key = { _, t -> t.uri }) { index, track ->
            Column(Modifier.width(148.dp).clickable { onPlay(index) }) {
                Artwork(track, Modifier.size(148.dp)); Spacer(Modifier.height(8.dp))
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
@Composable private fun TrackRow(track: Track, current: Boolean, play: () -> Unit, favorite: () -> Unit, more: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel.copy(alpha = .64f))
        .border(1.dp, Violet.copy(alpha = .12f), RoundedCornerShape(18.dp)).clickable(onClick = play).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, Modifier.size(52.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = if (current) Violet else Ivory, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = favorite) { Icon(if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (track.favorite) "Remove favorite" else "Favorite", tint = if (track.favorite) Violet else Muted) }
        IconButton(onClick = more) { Icon(Icons.Rounded.Add, "Add to playlist or queue", tint = Muted) }
    }
}
@Composable private fun MiniPlayer(state: PlaybackState, connection: PlaybackConnection, track: Track?, expand: () -> Unit) {
    Surface(onClick = expand, color = Color(0xFF30283E), shape = RoundedCornerShape(24.dp), border = glassBorder) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF947399).copy(alpha = .5f), Color.Transparent, Color(0xFF71608F).copy(alpha = .35f))))) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(track, Modifier.size(46.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Text(state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Muted, fontSize = 11.sp)
                }
                GlassPlay(Modifier, state.playing, if (state.playing) "Pause" else "Play", connection::toggle)
                IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next track") }
            }
            LinearProgressIndicator(progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp), color = Violet, trackColor = Color.Transparent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FullPlayer(state: PlaybackState, connection: PlaybackConnection, track: Track?, lyrics: String, reducedMotion: Boolean, performance: Boolean, onLyrics: () -> Unit, favorite: () -> Unit, close: () -> Unit) {
    var queue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    var ambient by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<Float?>(null) }
    BackHandler(onBack = close)
    androidx.compose.ui.window.Dialog(onDismissRequest = close, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Box(Modifier.fillMaxSize()) {
            AsyncImage(if (ambient) R.drawable.scene_calm else R.drawable.scene_valley, null,
                Modifier.fillMaxSize().then(if (!ambient && !performance) Modifier.blur(38.dp) else Modifier), contentScale = ContentScale.Crop, alpha = if (ambient) .75f else .5f)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .35f), Color(0xFF39283D).copy(alpha = .32f), Ink.copy(alpha = .8f)))))
            Column(Modifier.fillMaxSize().systemBarsPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player") }
                    Text(if (ambient) "AMBIENT" else "NOW PLAYING · LOCAL", Modifier.weight(1f), fontSize = 10.sp, letterSpacing = 2.sp)
                    IconButton(onClick = { ambient = !ambient }) { Icon(Icons.Rounded.NightsStay, "Toggle ambient mode") }
                }
                Spacer(Modifier.height(24.dp))
                if (!ambient) Artwork(track, Modifier.fillMaxWidth().aspectRatio(1f).border(glassBorder, RoundedCornerShape(18.dp)))
                else Spacer(Modifier.height(220.dp))
                Spacer(Modifier.height(28.dp))
                Text(state.title, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Text(state.artist, color = Muted, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                if (!ambient) {
                    Slider(value = seek ?: state.position.toFloat().coerceAtMost(state.duration.toFloat()), onValueChange = { seek = it },
                        onValueChangeFinished = { seek?.let { connection.seek(it.toLong()) }; seek = null },
                        valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time(state.position), color = Muted, fontSize = 12.sp); Text(time(state.duration), color = Muted, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (!ambient) IconButton(onClick = connection::shuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle ${if (state.shuffle) "on" else "off"}", tint = if (state.shuffle) Violet else Muted) }
                    IconButton(onClick = connection::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(32.dp)) }
                    Surface(onClick = connection::toggle, modifier = Modifier.size(76.dp), shape = CircleShape, color = Color.Transparent, border = glassBorder) {
                        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFFB68FC2).copy(alpha = .8f), Color(0xFF5B456D).copy(alpha = .75f)))), contentAlignment = Alignment.Center) {
                            Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play", Modifier.size(40.dp))
                        }
                    }
                    IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(32.dp)) }
                    if (!ambient) IconButton(onClick = connection::repeat) { Icon(if (state.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat ${listOf("off", "one", "all")[state.repeat]}", tint = if (state.repeat > 0) Violet else Muted) }
                }
                if (!ambient) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel.copy(alpha = .7f)).border(glassBorder, RoundedCornerShape(28.dp)), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = favorite) { Icon(if (track?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = Violet) }
                    IconButton(onClick = { showLyrics = true }) { Icon(Icons.Rounded.Lyrics, "Lyrics") }
                    IconButton(onClick = { queue = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue") }
                    IconButton(onClick = { audio = true }) { Icon(Icons.Rounded.Tune, "Audio and sleep timer") }
                }
                if (ambient) TextButton(onClick = { audio = true }) { Text("Sleep timer") }
            }
            }
        }
    }
    if (queue) ModalBottomSheet(onDismissRequest = { queue = false }, containerColor = Panel) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 550.dp), contentPadding = PaddingValues(20.dp)) {
            item { Heading("Up next", "${state.queue.size} tracks") }
            itemsIndexed(state.queue, key = { index, _ -> index }) { index, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { connection.jump(index) }, modifier = Modifier.weight(1f)) {
                        Text(item.second, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (index == state.index) Violet else Ivory)
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
            Text("Use Android's media controls to choose an available audio output.", color = Muted, fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(onClick = { audio = false }) { Text("Done") } })
    if (showLyrics) androidx.compose.ui.window.Dialog(onDismissRequest = { showLyrics = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        val lines = remember(lyrics) { Lrc.parse(lyrics) }
        val active = Lrc.activeIndex(lines, state.position)
        val listState = rememberLazyListState()
        var follow by remember { mutableStateOf(true) }
        var fontSize by remember { mutableIntStateOf(22) }
        LaunchedEffect(active) { if (follow && active >= 0 && !listState.isScrollInProgress) {
            if (reducedMotion) listState.scrollToItem(active) else listState.animateScrollToItem(active)
        } }
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(R.drawable.scene_valley, null, Modifier.fillMaxSize().then(if (!performance) Modifier.blur(40.dp) else Modifier), contentScale = ContentScale.Crop, alpha = .4f)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .4f), Color(0xFF4F384C).copy(alpha = .35f), Ink.copy(alpha = .7f)))))
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 26.dp, vertical = 12.dp)) {
                    Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(false, { showLyrics = false }, { Text("Song") }); FilterChip(true, {}, { Text("Lyrics") })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onLyrics, modifier = Modifier.weight(1f)) { Text("Import lyrics", fontSize = 12.sp) }
                        TextButton(onClick = { follow = !follow }) { Text(if (follow) "Following" else "Manual", fontSize = 12.sp) }
                        TextButton(onClick = { fontSize = if (fontSize >= 32) 20 else fontSize + 2 }) { Text("Aa") }
                    }
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 34.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                        if (lyrics.isBlank()) item { Empty("Every word, closer.", "Import a lyric file you own or are authorized to use. Synchronized LRC lines follow the music.") }
                        else if (lines.isEmpty()) item { Text(lyrics, fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp) }
                        else itemsIndexed(lines) { index, line -> Text(line.text, fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp,
                            fontWeight = if (index == active) FontWeight.SemiBold else FontWeight.Normal, color = if (index == active) Ivory else Color(0xFFC5B6D0).copy(alpha = .65f)) }
                    }
                    Slider(value = state.position.toFloat().coerceAtMost(state.duration.toFloat()), onValueChange = { connection.seek(it.toLong()) },
                        valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(state.position), fontSize = 11.sp, color = Muted); Text(time(state.duration), fontSize = 11.sp, color = Muted) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = connection::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
                        GlassPlay(Modifier, state.playing, if (state.playing) "Pause" else "Play", connection::toggle)
                        IconButton(onClick = connection::next) { Icon(Icons.Rounded.SkipNext, "Next") }
                    }
                }
            }
        }
    }
}
private fun time(ms: Long): String { val seconds = ms / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }


