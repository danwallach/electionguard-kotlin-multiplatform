package electionguard.core

import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashTest {
    @Test
    fun sameAnswerTwiceInARow() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            forAll(propTestFastConfig, elementsModP(context), elementsModQ(context)) { p, q ->
                val h1 = hashFunction(p.byteArray(), q)
                val h2 = hashFunction(p.byteArray(), q)
                h1 == h2
            }
        }
    }

    @Test
    fun basicHashProperties() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            checkAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { q1, q2 ->
                val h1 = hashFunction(q1.byteArray(), q1)
                val h2 = hashFunction(q2.byteArray(), q2)
                if (q1 == q2) assertEquals(h1, h2) else assertNotEquals(h1, h2)
            }
        }
    }

    @Test
    fun testNonce() {
        runTest {
            val group = productionGroup()
            val contestDescriptionHashQ = "00C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromHexSafe()
                .toUInt256safe().toElementModQ(group)
            println(" contestDescriptionHashQ = $contestDescriptionHashQ hex = ${contestDescriptionHashQ}")
            val ballotNonce = "13E7A2F4253E6CCE42ED5576CF7B01A06BE07835227E7AFE5F538FB94E9A9B73".fromHexSafe()
                .toUInt256safe().toElementModQ(group)
            val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
            val nonce0: ElementModQ = nonceSequence[0]
            println(" nonce seed in hex = ${nonceSequence.internalSeed.toHex()}")
            println(" nonce0 in hex = ${nonce0}")
            val expect = "ACDE405F255D4C3101A895AE80863EA4639A889593D557EB5AD5B855684D5B50".fromHexSafe()
                .toUInt256safe().toElementModQ(group)
            assertEquals(expect, nonceSequence[0])
        }
    }

    @Test
    fun testHexString() {
        runTest {
            val group = productionGroup()
            test("A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("9A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("0C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("00C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            test("0000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
        }
    }

    fun test(s1 : String, group : GroupContext) {
        val s1u = s1.fromHexSafe().toUInt256safe().toString()
        val s1q = s1.fromHexSafe().toUInt256safe().toElementModQ(group).toString()
        println(" len = ${s1.length} s1u = ${s1u} s1q = ${s1q}")
        assertEquals(64, s1q.length)
    }

    @Test
    fun testElementModQ() {
        runTest {
            val group = productionGroup()
            val s1q = "C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromHexSafe()
                .toUInt256safe().toElementModQ(group).toHex()
            val s2q = "000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromHexSafe()
                .toUInt256safe().toElementModQ(group).toHex()
            assertEquals(s1q, s2q)
            assertEquals(s1q.encodeToByteArray().size, s2q.encodeToByteArray().size)
            assertEquals(hashFunction(s1q.encodeToByteArray(), s1q), hashFunction(s2q.encodeToByteArray(), s2q))
            println("  len = ${s1q.length} hex = ${s1q}")
            assertEquals(64, s1q.length)
        }
    }

    @Test
    fun testElementModQToHex() {
        runTest {
            val group = productionGroup()
            val subject = group.TWO_MOD_Q.toHex()
            println(" len = ${subject.length} hex = '${subject}'")
            assertEquals(64, subject.length)
        }
    }

    @Test
    fun testIterable() {
        runTest {
            val h1 = hashFunction("hay1".encodeToByteArray(), listOf("hey2", "hey3"))
            val h2 = hashFunction("hay1".encodeToByteArray(), "hey2", "hey3")
            println(" h1 = ${h1}")
            println(" h2 = ${h2}")
            assertEquals(h1, h2)
        }
    }

    @Test
    fun testCiphertext() {
        runTest {
            val group = productionGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            val ciphertext = 42.encrypt(keypair)
            val h1 = hashFunction("hay1".encodeToByteArray(), 0x42, ciphertext)
            val h2 = hashFunction("hay1".encodeToByteArray(), 0x42, ciphertext.pad, ciphertext.data)
            assertEquals(h1, h2)
        }
    }

    @Test
    fun testPublicKey() {
        runTest {
            val group = productionGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            val h1 = hashFunction("hay2".encodeToByteArray(), 0x422, keypair.publicKey)
            val h2 = hashFunction("hay2".encodeToByteArray(), 0x422, keypair.publicKey.key)
            assertEquals(h1, h2)
        }
    }

}