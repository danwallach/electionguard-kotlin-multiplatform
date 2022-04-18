@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

import electionguard.ballot.ElectionRecord
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cli.ExperimentalCli

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptingMediatorTest {

    @Test
    fun testDecryptingMediator() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/workflow/runTallyAccumulation"
        val guardianDir = "src/commonTest/data/testJava/keyCeremony/election_private_data"
        val outputDir = "src/commonTest/data/testing/runDecryptingMediator"
        runDecryptingMediator(
            group,
            inputDir,
            outputDir,
            makeDecryptingTrustees(group, guardianDir)
        )
    }

    fun makeDecryptingTrustees(
        group: GroupContext,
        guardianDir: String
    ) : List<DecryptingTrusteeIF> {
        val consumer = Consumer(guardianDir, group)
        return consumer.readTrustees(guardianDir)
    }

    fun runDecryptingMediator(
        group: GroupContext,
        inputDir: String,
        outputDir: String,
        decryptingTrustees: List<DecryptingTrusteeIF>
    ) {
        val consumer = Consumer(inputDir, group)
        val electionRecord: ElectionRecord = consumer.readElectionRecord()
        val decryptor = DecryptingMediator(group, electionRecord.context!!, decryptingTrustees)
        val decryptedTally = with(decryptor) { electionRecord.encryptedTally!!.decrypt() }

        val pkeys: Iterable<ElementModP> = decryptingTrustees.map { it.electionPublicKey() }
        val pkey = with (group) { pkeys.multP() }
        assertEquals(electionRecord.context!!.jointPublicKey, pkey)

        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writeElectionRecordProto(
            electionRecord.manifest,
            electionRecord.constants,
            electionRecord.context,
            electionRecord.guardianRecords,
            electionRecord.devices,
            consumer.iterateSubmittedBallots(),
            electionRecord.encryptedTally,
            decryptedTally,
            null,
            decryptor.computeAvailableGuardians(),
        )
    }
}
