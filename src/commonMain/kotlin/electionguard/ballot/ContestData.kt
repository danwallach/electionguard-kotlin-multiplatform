package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.safeEnumValueOf
import mu.KotlinLogging
import pbandk.encodeToByteArray
import kotlin.math.max

private val logger = KotlinLogging.logger("ContestData")
private const val debug = false
private const val BLOCK_SIZE : Int = 32
private const val CHOP_WRITE_INS : Int = 30

enum class ContestDataStatus {
    normal, null_vote, over_vote, under_vote
}

data class ContestData(
    val overvotes: List<Int>,
    val writeIns: List<String>,
    val status: ContestDataStatus = if (overvotes.isNotEmpty()) ContestDataStatus.over_vote else ContestDataStatus.normal,
) {

    fun publish(filler: String = ""): electionguard.protogen.ContestData {
        return publish(
            this.status,
            this.overvotes,
            this.writeIns,
            filler,
        )
    }

    fun publish(
        status: ContestDataStatus,
        overvotes: List<Int>,
        writeIns: List<String>,
        filler: String = ""
    ): electionguard.protogen.ContestData {
        return electionguard.protogen.ContestData(
            status.publishContestDataStatus(),
            overvotes,
            writeIns,
            filler,
        )
    }

    // Make sure that the HashedElGamalCiphertext message is exactly (votesAllowed + 1) * BLOCK_SIZE
    // If too large, remove extra writeIns, add "*" to list to indicate some were removed
    // If still too large, truncate writeIns to CHOP_WRITE_INS characters, append "*" to string to indicate truncated
    // If still too large, truncate overVote to (votesAllowed + 1), append "-1" to list to indicate some were removed
    // If now too small, add a filler string to make it exactly (votesAllowed + 1) * BLOCK_SIZE
    fun encrypt(publicKey: ElGamalPublicKey, votesAllowed: Int, contestDataNonce: ElementModQ?): HashedElGamalCiphertext {
        val messageSize = (1 + votesAllowed) * BLOCK_SIZE

        var trialContestData = this
        var trialContestDataBA = trialContestData.publish().encodeToByteArray()
        var trialSize = trialContestDataBA.size
        val trialSizes = mutableListOf<Int>()
        trialSizes.add(trialSize)

        // remove extra write_ins, append a "*"
        if ((trialSize > messageSize) && trialContestData.writeIns.size > votesAllowed) {
            val truncateWriteIns = trialContestData.writeIns.subList(0, votesAllowed)
                .toMutableList()
            truncateWriteIns.add("*")
            trialContestData = trialContestData.copy(
                writeIns = truncateWriteIns,
            )
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // chop write in vote strings
        if ((trialSize > messageSize) && trialContestData.writeIns.isNotEmpty()) {
            val chop = max(CHOP_WRITE_INS, (messageSize - trialSize + votesAllowed - 1) / votesAllowed)
            val truncateWriteIns = trialContestData.writeIns.map {
                if (it.length <= CHOP_WRITE_INS) it else it.substring(0, chop) + "*"
            }
            trialContestData = trialContestData.copy(writeIns = truncateWriteIns)
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // chop overvote list
        while (trialSize > messageSize && (trialContestData.overvotes.size > votesAllowed + 1)) {
            val chopList = trialContestData.overvotes.subList(0, votesAllowed + 1) + (-1)
            trialContestData = trialContestData.copy(overvotes = chopList)
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // now fill it up so its a uniform message length, if needed
        if (trialSize < messageSize) {
            val filler = StringBuilder().apply {
                repeat(messageSize - trialSize - 2) { append("*") }
            }
            trialContestDataBA = trialContestData.publish(filler.toString()).encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }
        if (debug) println("encodedData = $trialContestData")
        if (debug) println(" trialSizes = $trialSizes")

        // HMAC encryption
        return if (contestDataNonce == null) trialContestDataBA.hashedElGamalEncrypt(publicKey)
               else trialContestDataBA.hashedElGamalEncrypt(publicKey, contestDataNonce)
    }
}

fun electionguard.protogen.ContestData.import(): ContestData {
    return ContestData(
        this.overVotes,
        this.writeIns,
        this.status.importContestDataStatus()?: ContestDataStatus.normal,
    )
}

private fun ContestDataStatus.publishContestDataStatus(): electionguard.protogen.ContestData.Status {
    return electionguard.protogen.ContestData.Status.fromName(this.name)
}

private fun electionguard.protogen.ContestData.Status.importContestDataStatus(): ContestDataStatus? {
    val result = safeEnumValueOf<ContestDataStatus>(this.name)
    if (result == null) {
        logger.error { "Vote election type $this has missing or incorrect name" }
    }
    return result
}