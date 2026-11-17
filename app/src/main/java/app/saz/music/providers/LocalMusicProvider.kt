package app.saz.music.providers

import app.saz.music.database.Track
import app.saz.music.library.LibraryRepository
import javax.inject.Inject

class LocalMusicProvider @Inject constructor(private val repository: LibraryRepository) :
    SearchProvider, MetadataProvider, LibraryProvider, PlaybackProvider {
    val tracks = repository.dao.tracks()
    override val capabilities = PlaybackCapabilities(PlaybackMode.LOCAL_AUDIO, background = true, seek = true)
    override suspend fun library() = ProviderResult.Success(repository.dao.allTracks().map { it.asMusicItem() })
    override suspend fun search(request: SearchRequest): ProviderResult<SearchPage> {
        val query = request.query.trim()
        return ProviderResult.Success(SearchPage(repository.dao.allTracks().filter {
            listOf(it.title, it.artist, it.album).any { field -> field.contains(query, true) }
        }.map { it.asMusicItem() }))
    }
    override suspend fun metadata(key: MediaKey): ProviderResult<MusicItem> {
        if (key.provider != ProviderId.LOCAL) return problem(ProblemKind.INVALID_REQUEST, "This item is not local audio.")
        return repository.dao.track(key.value)?.let { ProviderResult.Success(it.asMusicItem()) }
            ?: problem(ProblemKind.NOT_FOUND, "This song is no longer in the local library.")
    }
    override suspend fun prepare(key: MediaKey) = metadata(key)
}

fun Track.asMusicItem() = MusicItem(MediaKey(ProviderId.LOCAL, uri), title, artist, artworkUri, durationMs)
