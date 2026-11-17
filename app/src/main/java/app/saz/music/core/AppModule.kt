package app.saz.music.core

import android.content.Context
import androidx.room.Room
import app.saz.music.database.LibraryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides fun transport(http: app.saz.music.providers.ProviderHttp): app.saz.music.providers.JsonTransport = http
    @Provides fun credentials(store: app.saz.music.providers.ApiKeyStore): app.saz.music.providers.ProviderCredentials = store
    @Provides @Singleton fun database(@ApplicationContext context: Context): LibraryDatabase =
        Room.databaseBuilder(context, LibraryDatabase::class.java, "saz.db").build()
    @Provides fun library(database: LibraryDatabase) = database.library()
}
