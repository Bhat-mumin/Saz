package app.saz.music.integrations

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/** Opens provider-owned UI; this does not connect an account or stream audio in Saz. */
fun Activity.openMusicSearch(destination: MusicDestination, query: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(destination.searchUrl(query)))
        .addCategory(Intent.CATEGORY_BROWSABLE)
    return try {
        startActivity(Intent(intent).setPackage(destination.packageName))
        true
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    } catch (_: SecurityException) {
        false
    }
}
