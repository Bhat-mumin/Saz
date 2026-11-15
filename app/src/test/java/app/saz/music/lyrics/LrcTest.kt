package app.saz.music.lyrics

import org.junit.Assert.*
import org.junit.Test

class LrcTest {
    @Test fun repeatedTimestampsOffsetsAndFractions() {
        val lines = Lrc.parse("[offset:-100]\n[00:02.50][00:04.005]Hello\n[00:01.2]First\n[00:99]Invalid\n[ar:Artist]")
        assertEquals(listOf(1100L, 2400L, 3905L), lines.map { it.timeMs })
        assertEquals(listOf("First", "Hello", "Hello"), lines.map { it.text })
        assertEquals(-1, Lrc.activeIndex(lines, 0))
        assertEquals(1, Lrc.activeIndex(lines, 2400))
    }
    @Test fun staticLyricsAndInvalidTextHaveNoTimestamps() {
        assertTrue(Lrc.parse("A line\nAnother line").isEmpty())
        assertTrue(Lrc.parse("[no:time]Text").isEmpty())
    }
}
