package app.saz.music.lyrics

data class LyricLine(val timeMs: Long, val text: String)
object Lrc {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    fun parse(text: String): List<LyricLine> {
        val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        return text.lineSequence().flatMap { line ->
            val words = line.replace(timestamp, "").trim()
            timestamp.findAll(line).mapNotNull { match ->
                val seconds = match.groupValues[2].toInt()
                if (seconds >= 60) null else {
                    val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
                    LyricLine((match.groupValues[1].toLong() * 60000 + seconds * 1000 + fraction + offset).coerceAtLeast(0), words)
                }
            }
        }.sortedBy { it.timeMs }.toList()
    }
    fun activeIndex(lines: List<LyricLine>, position: Long): Int = lines.indexOfLast { it.timeMs <= position }
}
