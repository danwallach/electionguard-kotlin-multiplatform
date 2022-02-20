package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gciatto.kt.math.BigInteger

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64Production4096P.fromSafeBase64().toBigInteger()
        val q = b64Production4096Q.fromSafeBase64().toBigInteger()
        val qInv = b64Production4096P256MinusQ.fromSafeBase64().toBigInteger()
        val g = b64Production4096G.fromSafeBase64().toBigInteger()
        val r = b64Production4096R.fromSafeBase64().toBigInteger()

        val big1 = BigInteger.of(1)

        assertTrue(p > big1)
        assertTrue(q > big1)
        assertTrue(g > big1)
        assertTrue(r > big1)
        assertTrue(qInv > big1)
        assertTrue(q < p)
        assertTrue(g < p)
    }

    @Test
    fun saneConstantsSmall() {
        val p = b64TestP.fromSafeBase64().toBigInteger()
        val q = b64TestQ.fromSafeBase64().toBigInteger()
        val g = b64TestG.fromSafeBase64().toBigInteger()
        val r = b64TestR.fromSafeBase64().toBigInteger()

        assertEquals(BigInteger.of(intTestP), p)
        assertEquals(BigInteger.of(intTestQ), q)
        assertEquals(BigInteger.of(intTestG), g)
        assertEquals(BigInteger.of(intTestR), r)
    }
}