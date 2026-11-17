package app.saz.music.designsystem

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.saz.music.providers.MusicItem
import app.saz.music.providers.youtubePlayerHtml
import app.saz.music.providers.playerError
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener

/** An official visible YouTube player; never participates in the Media3 audio queue. */
@SuppressLint("SetJavaScriptEnabled")
@Composable internal fun YouTubePlayer(item: MusicItem, close: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var web by remember { mutableStateOf<WebView?>(null) }
    var error by remember(item.key) { mutableStateOf<String?>(null) }
    var ready by remember(item.key) { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val html = remember(item.key) { youtubePlayerHtml(item.key.value, context.packageName) }
    fun openExternally() {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${item.key.value}"))) }
        catch (_: ActivityNotFoundException) { error = "No app or browser can open YouTube." }
    }
    DisposableEffect(owner, web) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                foreground = false
                // Destroy the playable document when hidden, rather than relying on a delayed JS pause.
                web?.stopLoading(); web?.loadUrl("about:blank"); web?.onPause(); ready = false
            } else if (event == Lifecycle.Event.ON_RESUME) { foreground = true; web?.onResume(); reload++ }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(web, reload, foreground) {
        if (!foreground) return@LaunchedEffect
        error = null; ready = false
        web?.loadDataWithBaseURL("https://${context.packageName}/", html, "text/html", "UTF-8", null)
        val started = System.currentTimeMillis()
        while (true) {
            delay(1000)
            web?.evaluateJavascript("JSON.stringify({ready:window.sazReady===true,error:window.sazError||0})") { value ->
                try {
                    val json = JSONObject(JSONTokener(value).nextValue() as? String ?: "{}")
                    ready = json.optBoolean("ready")
                    val code = json.optInt("error")
                    if (code != 0) error = playerError(code)
                } catch (_: org.json.JSONException) { /* The document may have been disposed while polling. */ }
            }
            if (!ready && System.currentTimeMillis() - started > 25_000 && error == null)
                error = "The player has not loaded. Check your connection, retry, or open YouTube."
        }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = Mineral) {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                val enoughRoom = maxWidth >= 200.dp && maxHeight >= 320.dp
                val playerHeight = maxOf(200.dp, minOf(maxWidth * 9f / 16f, 280.dp))
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = close) { Text("Close") }
                        TextButton(onClick = ::openExternally) { Text("Open YouTube") }
                    }
                    if (enoughRoom) key(item.key, enoughRoom) {
                        AndroidView(modifier = Modifier.fillMaxWidth().height(playerHeight), factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                                WebView.setWebContentsDebuggingEnabled((ctx.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                                webChromeClient = WebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.mediaPlaybackRequiresUserGesture = true
                                settings.safeBrowsingEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                        if (request.isForMainFrame) error = "Could not load YouTube. Check your connection."
                                    }
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (!request.isForMainFrame) return false
                                        if (request.url.toString() == "about:blank") return false
                                        if (request.hasGesture() && request.url.scheme == "https") {
                                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                                            catch (_: ActivityNotFoundException) { error = "No browser is available." }
                                        }
                                        return true
                                    }
                                }
                                web = this
                            }
                        }, onRelease = { view ->
                            view.stopLoading(); view.loadUrl("about:blank"); view.onPause(); view.removeAllViews(); view.destroy()
                            if (web === view) web = null
                        })
                    } else Text("The window is too small for a visible YouTube player. Enlarge it or open YouTube.")
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge)
                        Text("YouTube · ${item.artist}", color = SecondaryText)
                        Text("Embedded video · playback stops when this screen is hidden", style = MaterialTheme.typography.bodySmall)
                        if (!ready && error == null) Text("Loading YouTube player…")
                        error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { reload++ }) { Text("Retry player") }
                        }
                        Text("YouTube controls this playback and may show ads. Some videos require viewing in YouTube or are unavailable in your region.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }
            }
        }
    }
}
