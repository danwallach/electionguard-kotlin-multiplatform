package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.ElectionInitialized
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.input.ManifestInputValidation
import electionguard.protoconvert.generateElectionConfig
import electionguard.protoconvert.generateElementModP
import electionguard.protoconvert.generateGuardian
import electionguard.protoconvert.generateUInt256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublisherProtoTest {
    private val input = "src/commonTest/data/runWorkflowSomeAvailable"
    private val output = "testOut/PublisherProtoTest"

    val group = productionGroup()
    val publisher = makePublisher(output, true)
    val consumerIn = makeConsumer(input, group)
    val consumerOut = makeConsumer(output, group)

    @Test
    fun testRoundtripElectionConfig() {
        val config = generateElectionConfig(3, 3)

        // ManifestInputValidation
        val manifestValidator = ManifestInputValidation(config.manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            println("*** ManifestInputValidation FAILED on generated electionConfig")
            println("$errors")
            return
        }

        publisher.writeElectionConfig(config)

        val roundtripResult = consumerOut.readElectionConfig()
        assertNotNull(roundtripResult)
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertEquals(config.constants, roundtrip.constants)
        assertEquals(config.manifest, roundtrip.manifest)
        assertEquals(config.numberOfGuardians, roundtrip.numberOfGuardians)
        assertEquals(config.quorum, roundtrip.quorum)
        assertEquals(config.metadata, roundtrip.metadata)

        assertEquals(config, roundtrip)
    }

    @Test
    fun testRoundtripElectionInit() {
        val config = generateElectionConfig(6, 4)
        publisher.writeElectionConfig(config)

        val init = ElectionInitialized(
            config,
            generateElementModP(group),
            config.manifest.cryptoHash,
            generateUInt256(group),
            generateUInt256(group),
            List(6) { generateGuardian(it, group) },
        )
        publisher.writeElectionInitialized(init)

        val roundtripResult = consumerOut.readElectionInitialized()
        assertNotNull(roundtripResult)
        if (roundtripResult is Err) {
            println("readElectionInitialized = $roundtripResult")
        }
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertEquals(init, roundtrip)
    }

    @Test
    fun testWriteEncryptions() {
        runTest {
            val initResult = consumerIn.readElectionInitialized()
            assertTrue(initResult is Ok)
            val init = initResult.unwrap()

            publisher.writeEncryptions(init, consumerIn.iterateEncryptedBallots { true })

            val rtResult = consumerOut.readElectionInitialized()
            if (rtResult is Err) {
                println("testWriteEncryptions = $rtResult")
            }
            assertTrue(rtResult is Ok)
            val rtInit = rtResult.unwrap()
            assertEquals(init, rtInit)

            val inBallots = consumerIn.iterateEncryptedBallots{ true }.associateBy { it.ballotId }
            consumerOut.iterateEncryptedBallots{ true }.forEach {
                val inBallot = inBallots[it.ballotId] ?: RuntimeException("Cant find ${it.ballotId}")
                assertEquals(it, inBallot)
                println(" Ballot ${it.ballotId} OK")
            }
        }
    }

    @Test
    fun testWriteSpoiledBallots() {
        val sink: DecryptedTallyOrBallotSinkIF = publisher.decryptedTallyOrBallotSink()
        consumerIn.iterateDecryptedBallots().forEach {
            sink.writeDecryptedTallyOrBallot(it)
        }
        sink.close()

        val inBallots = consumerIn.iterateDecryptedBallots().associateBy { it.id }
        consumerOut.iterateDecryptedBallots().forEach {
            val inBallot = inBallots[it.id] ?: throw RuntimeException("Cant find ${it.id}")
            it.contests.forEach{
                val inContest = inBallot.contests.find { c -> it.contestId == c.contestId } ?:
                throw RuntimeException("Cant find ${it.contestId}")
                it.selections.forEach {
                    val inSelection = inContest.selections.find { s -> it.selectionId == s.selectionId } ?:
                    throw RuntimeException("Cant find ${it.selectionId}")
                    assertEquals(inSelection.selectionId, it.selectionId)
                    assertEquals(inSelection.tally, it.tally)
                    assertEquals(inSelection.value, it.value)
                    assertEquals(inSelection.message, it.message)
                    assertEquals(inSelection.proof, it.proof)
                    assertEquals(inSelection, it)
                }
                // decryptedContestData not yet done
            }
            println(" Spoiled Ballot ${it.id} OK")
        }
    }

}