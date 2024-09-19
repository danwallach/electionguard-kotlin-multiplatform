package electionguard.json

import com.github.michaelbull.result.Ok
import electionguard.core.elGamalKeypairs
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.schnorrProof
import electionguard.jsonRoundTrip
import electionguard.runTest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchnorrProofTest {
    @Test
    fun testRoundtrip(): TestResult {
        return runTest {
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elGamalKeypairs(group),
                elementsModQ(group),
                Arb.int(min = 1, max = 10),
                Arb.int(min = 1, max = 10),
            ) { kp, nonce, i, j ->
                val goodProof = kp.schnorrProof(i, j, nonce)
                assertTrue(goodProof.validate(i, j) is Ok)

                assertEquals(goodProof, goodProof.publishJson().import(group))
                assertEquals(goodProof, jsonRoundTrip(goodProof.publishJson()).import(group))
            }
        }
    }
}
