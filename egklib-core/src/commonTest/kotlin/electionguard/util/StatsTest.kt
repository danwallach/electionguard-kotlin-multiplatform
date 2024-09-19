package electionguard.util

import electionguard.runTest
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {

    @Test
    fun testStatEmpty() {
        val stat = Stat("thing", "what")
        assertEquals("took   0 msecs = .0 msecs/thing (0 things) = .0 msecs/what for 0 whats", stat.show())
    }

    @Test
    fun testStat() {
        val stat = Stat("thing", "what")
        stat.accum(99, 2)
        stat.accum(101, 2)
        stat.accum(15, 11)
        assertEquals("took 215 msecs = 14.33 msecs/thing (15 things) = 71.666 msecs/what for 3 whats", stat.show())
    }

    @Test
    fun testStatsEmpty() {
        val stats = Stats()
        assertEquals(listOf("stats is empty"), stats.showLines())
    }

    @Test
    fun testStatsStatEmpty() {
        val stats = Stats()
        stats.of("widgets")
        assertEquals(
            listOf(
                "             widgets: took   0 msecs = .0 msecs/decryption (0 decryptions) = .0 msecs/ballot for 0 ballots",
                "               total: took   0 msecs = .0 msecs/decryption (0 decryptions) = .0 msecs/ballot for 0 ballots"
            ),
            stats.showLines()
        )
    }

    @Test
    fun testStats() {
        val stats = Stats()
        stats.of("widgets").accum(15, 11)
        stats.of("blivits").accum(11, 15)
        stats.of("widgets").accum(7, 7)

        assertEquals(
            listOf(
                "             widgets: took  22 msecs = 1.222 msecs/decryption (18 decryptions) = 11.0 msecs/ballot for 2 ballots",
                "             blivits: took  11 msecs = .7333 msecs/decryption (15 decryptions) = 11.0 msecs/ballot for 1 ballots",
                "               total: took  33 msecs = 1.0 msecs/decryption (33 decryptions) = 11.0 msecs/ballot for 3 ballots",
            ), stats.showLines()
        )

        assertEquals(
            listOf(
                "             widgets: took    22 msecs = 1.222 msecs/decryption (18 decryptions) = 11.0 msecs/ballot for 2 ballots",
                "             blivits: took    11 msecs = .7333 msecs/decryption (15 decryptions) = 11.0 msecs/ballot for 1 ballots",
                "               total: took    33 msecs = 1.0 msecs/decryption (33 decryptions) = 11.0 msecs/ballot for 3 ballots",
            ), stats.showLines(5)
        )
    }

    @Test
    fun testStatsAsync() = runTest {
        val stats = Stats()
        repeat(100) {
            launch {
                stats.of("widgets").accum(15, 11)
                stats.of("blivits").accum(11, 15)
                stats.of("widgets").accum(7, 7)
            }.join()
        }
        assertEquals(
            listOf(
                "             widgets: took 2200 msecs = 1.222 msecs/decryption (1800 decryptions) = 11.0 msecs/ballot for 200 ballots",
                "             blivits: took 1100 msecs = .7333 msecs/decryption (1500 decryptions) = 11.0 msecs/ballot for 100 ballots",
                "               total: took 3300 msecs = 1.0 msecs/decryption (3300 decryptions) = 11.0 msecs/ballot for 300 ballots",
            ), stats.showLines()
        )
    }

    @Test
    fun testSigfig() {
        assertEquals(".0909090", (1.0 / 11).sigfig(6))
        assertEquals(".090909", (1.0 / 11).sigfig(5))
        assertEquals(".09090", (1.0 / 11).sigfig(4))
        assertEquals(".0909", (1.0 / 11).sigfig(3))
        assertEquals(".090", (1.0 / 11).sigfig(2))
        assertEquals(".09", (1.0 / 11).sigfig(1))
        assertEquals(".0", (1.0 / 11).sigfig(0))
        assertEquals(".0", (0.0).sigfig(0))
    }

    @Test
    fun testSigfig2() {
        assertEquals("9.09090", (100.0 / 11).sigfig(6))
        assertEquals("100.090", (100.0 + 1.0 / 11).sigfig(6))
    }
}
