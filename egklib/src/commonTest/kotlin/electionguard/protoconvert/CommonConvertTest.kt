package electionguard.protoconvert

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonConvertTest {

    @Test
    fun convertElementMod() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                elementsModQ(productionGroup()),
            ) { fakeElementModP, fakeElementModQ ->
                    val context = productionGroup()

                    val protoP = fakeElementModP.publishElementModP()
                    val roundtripP = context.importElementModP(protoP)
                    assertEquals(roundtripP, fakeElementModP)

                    val protoQ = fakeElementModQ.publishElementModQ()
                    val roundtripQ = context.importElementModQ(protoQ)
                    assertEquals(roundtripQ, fakeElementModQ)
                }
        }
    }

    @Test
    fun convertCiphertext() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                validElementsModP(productionGroup()),
            ) { fakePad, fakeData ->
                    val context = productionGroup()
                    val ciphertext = ElGamalCiphertext(fakePad, fakeData)

                    val proto = ciphertext.publishCiphertext()
                    val roundtrip = context.importCiphertext(proto)
                    assertEquals(roundtrip, ciphertext)
                }
        }
    }

    @Test
    fun convertChaumPedersenProof() {
        runTest {
            checkAll(
                elementsModQ(productionGroup()),
                elementsModQ(productionGroup()),
            ) { c, r ->
                    val context = productionGroup()
                    val proof = GenericChaumPedersenProof(c, r)

                    val proto = proof.publishChaumPedersenProof()
                    val roundtrip = context.importChaumPedersenProof(proto)
                    assertEquals(roundtrip, proof)
                }
        }
    }

    @Test
    fun convertSchnorrProof() {
        runTest {
            checkAll(
                elementsModP(productionGroup()),
                elementsModQ(productionGroup()),
                elementsModQ(productionGroup()),
            ) { k, c, r ->
                val context = productionGroup()
                val proof = SchnorrProof(k, c, r)

                val proto = proof.publishSchnorrProof()
                val roundtrip = context.importSchnorrProof(proto)
                assertEquals(roundtrip, proof)
            }
        }
    }

    @Test
    fun convertHashedElGamalCiphertext() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                byteArrays(21),
                uint256s(),
            ) { p, c1, u ->
                val context = productionGroup()
                val target = HashedElGamalCiphertext(p, c1, u, 21)
                assertEquals(target.numBytes, target.c1.size)

                val proto = target.publishHashedCiphertext()
                val roundtrip = context.importHashedCiphertext(proto)
                assertEquals(target, roundtrip)
            }
        }
    }
}