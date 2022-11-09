package electionguard.decryptBallot

import electionguard.core.productionGroup
import electionguard.decrypt.readDecryptingTrustees
import electionguard.publish.Consumer

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test runDecryptBallots with in-process DecryptingTrustee's. Do not use this in production.
 * Note that when the election record changes, the test dataset must be regenerated. For this test, we need to:
 *   1. run showBallotIds() test to see what the possible ballot ids are
 *   2. modify inputDir/private_data/wantedBallots.txt and add 2 valid ballot ids
 *   3. modify testDecryptBallotsSome() and add 3 valid ballot ids to the command line argument
 */
class RunDecryptBallotsTest {
    @Test
    fun testDecryptBallotsAllSingleThreaded() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingBallotsAll"
        println("\ntestDecryptBallotsAll")
        val n = runDecryptBallots(
            group,
            inputDir,
            outputDir,
            readDecryptingTrustees(group, inputDir, trusteeDir),
            "ALL",
            1,
        )
        assertEquals(11, n)
    }

    @Test
    fun testDecryptBallotsSomeFromList() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingBallotsSome"
        println("\ntestDecryptBallotsSomeFromList")
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "5"),
            "ballot-id-710324727,ballot-id--1158743835,ballot-id-922600107",
            1,
        )
        assertEquals(3, n)
    }

    @Test
    fun testDecryptBallotsSomeFromFile() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val wantBallots = "src/commonTest/data/runWorkflowSomeAvailable/private_data/wantedBallots.txt"
        val outputDir = "testOut/testDecryptingBallotsSome"
        println("\ntestDecryptBallotsSomeFromFile")
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "4,5"),
            wantBallots,
            1,
        )
        assertEquals(2, n)
    }

    // decrypt all the ballots
    @Test
    fun testDecryptBallotsMainMultiThreaded() {
        println("\ntestDecryptBallotsMainMultiThreaded")
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-trustees",
                "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingBallotsSome",
                "-spoiled",
                "all",
                "-nthreads",
                "6"
            )
        )
    }

    // decrypt the ballots marked spoiled
    @Test
    fun testDecryptBallotsMarkedSpoiled() {
        println("\ntestDecryptBallotsMainDefault")
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-trustees",
                "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingBallotsSome",
                "-nthreads",
                "6"
            )
        )
    }

    @Test
    fun showBallotIds() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val ballotDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/input/"
        val consumerIn = Consumer(inputDir, group)

        consumerIn.iteratePlaintextBallots(ballotDir, null).forEach {
            println(it.ballotId)
        }
    }
}