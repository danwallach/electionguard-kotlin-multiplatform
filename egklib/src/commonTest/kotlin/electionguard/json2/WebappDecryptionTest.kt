package electionguard.json2

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.elementsModP
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.propTestFastConfig
import electionguard.core.runTest
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse
import electionguard.decrypt.PartialDecryption
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebappDecryptionTest {

    @Test
    fun testSetMissingRequest(): TestResult {
        return runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 5),
                elementsModQ(group, minimum = 2)
            ) { name, nmissing, lc ->
                val miss = List(nmissing) { name + it }
                val org = SetMissingRequest(lc, miss)

                assertEquals(org, org.publishJson().import(group))
            }
        }
    }

    @Test
    fun testDecryptRequest(): TestResult {
        return runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.int(min = 1, max = 11),
            ) {  nrequests ->
                val qList = List(nrequests) { elementsModP(group, minimum = 2).single() }
                val org = DecryptRequest(qList)
                val request = org.publishJson().import(group)
                assertTrue(request is Ok)
                assertEquals(org, request.unwrap())
            }
        }
    }

    @Test
    fun testDecryptResponse(): TestResult {
        return runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 11),
            ) {  name, nrequests ->
                val partials = List(nrequests) {
                    PartialDecryption(
                    name + it,
                        elementsModP(group, minimum = 2).single(),
                        elementsModQ(group, minimum = 2).single(),
                        elementsModP(group, minimum = 2).single(),
                        elementsModP(group, minimum = 2).single(),
                    )
                }
                val org = DecryptResponse(partials)
                val response = org.publishJson().import(group)
                assertTrue(response is Ok)
                assertEquals(org, response.unwrap())
            }
        }
    }

    @Test
    fun testChallengeRequests(): TestResult {
        return runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 11),
            ) {  name, nrequests ->
                val crs = List(nrequests) {
                    ChallengeRequest(
                        name + it,
                        elementsModQ(group, minimum = 2).single(),
                        elementsModQ(group, minimum = 2).single(),
                    )
                }
                val org = ChallengeRequests(crs)
                val challenges = org.publishJson().import(group)
                assertTrue(challenges is Ok)
                assertEquals(org, challenges.unwrap())
            }
        }
    }


    @Test
    fun testChallengeResponses(): TestResult {
        return runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 11),
            ) {  name, nrequests ->
                val crs = List(nrequests) {
                    ChallengeResponse(
                        name + it,
                        elementsModQ(group, minimum = 2).single(),
                    )
                }
                val org = ChallengeResponses(crs)
                val responses = org.publishJson().import(group)
                assertTrue(responses is Ok)
                assertEquals(org, responses.unwrap())
            }
        }
    }
}