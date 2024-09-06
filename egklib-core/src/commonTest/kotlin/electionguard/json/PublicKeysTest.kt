package electionguard.json

import electionguard.core.*
import electionguard.jsonRoundTrip
import electionguard.model.PublicKeys
import electionguard.util.ErrorMessages
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicKeysTest {
    @Test
    fun testRoundtrip(): TestResult {
        return runTest {
            val group = productionGroup()
            checkAll(
                iterations = 33,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 10),
                Arb.int(min = 1, max = 10),
                ) { id, xcoord, quota,  ->

                val proofs = mutableListOf<SchnorrProof>()
                repeat(quota) {
                    val kp = elGamalKeypairs(group).single()
                    val nonce = elementsModQ(group).single()
                    proofs.add(kp.schnorrProof(xcoord, it, nonce))
                }
                val publicKey = PublicKeys(id, xcoord, proofs)
                assertEquals(publicKey, publicKey.publishJson().import(group, ErrorMessages("round")))
                assertEquals(publicKey, jsonRoundTrip(publicKey.publishJson()).import(group, ErrorMessages("trip")))
            }
        }
    }
}