package electionguard.json

import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.elGamalKeypairs
import electionguard.core.elementsModQ
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.productionGroup
import electionguard.core.propTestFastConfig
import electionguard.core.runTest
import electionguard.keyceremony.SecretKeyShare
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class SecretKeyShareTest {

    @Test
    fun testRoundtrip() {
        runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.string(minSize = 3),
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 5),
                Arb.byteArray(Arb.int(min = 1, max = 5).map { it * 32 }, Arb.byte()),
                elGamalKeypairs(group),
                elementsModQ(group, minimum = 2)
            ) { g1, g2, x, bytes, kp, nonce ->
                val ciphertext = bytes.hashedElGamalEncrypt(kp, nonce)
                val sks = SecretKeyShare(g1, g2, x, ciphertext)

                assertEquals(sks, group.importSecretKeyShare(sks.publish()))
                assertEquals(sks, group.importSecretKeyShare(jsonRoundTrip(sks.publish())))
            }
        }
    }
}