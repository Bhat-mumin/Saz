package app.saz.music.providers

internal fun youtubePlayerHtml(videoId: String, appId: String): String {
    require(validVideoId(videoId))
    require(Regex("[A-Za-z0-9_.]+").matches(appId))
    return """<!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>html,body{width:100%;height:100vh;margin:0;background:#000;overflow:hidden}
        #player,iframe{position:fixed;inset:0;display:block;width:100%;height:100vh;border:0}</style>
        </head><body><div id="player"></div><script>
        var player;window.sazReady=false;window.sazError=0;
        function onYouTubeIframeAPIReady(){player=new YT.Player('player',{
          width:'100%',height:'100%',videoId:'$videoId',
          playerVars:{playsinline:1,controls:1,autoplay:0,fs:0,origin:'https://$appId'},
          events:{onReady:function(){window.sazReady=true;},onError:function(e){window.sazError=e.data;}}
        });}
        </script><script src="https://www.youtube.com/iframe_api"></script></body></html>""".trimIndent()
}

internal fun playerError(code: Int): String = when (code) {
    2 -> "YouTube rejected this video identifier."
    5 -> "This WebView cannot play this video. Open YouTube instead."
    100 -> "This video is private, removed, or unavailable."
    101, 150 -> "The owner does not allow embedded playback. Open YouTube instead."
    153 -> "YouTube could not verify the app identity. Open YouTube and report this configuration issue."
    else -> "YouTube playback failed (code $code). Try again or open YouTube."
}
