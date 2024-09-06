package electionguard.testvectors

import electionguard.model.EncryptedBallot.BallotState
import electionguard.model.EncryptedBallotIF
import electionguard.model.ManifestIF
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.encrypt.CiphertextBallot
import electionguard.json.*
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBallotJsonV(
    val ballotId: String,
    val ballotNonce: UInt256Json,
    val contests: List<EncryptedContestJsonV>,
)

@Serializable
data class EncryptedContestJsonV(
    val contestId: String,
    val sequenceOrder: Int,
    val selections: List<EncryptedSelectionJsonV>,
)

@Serializable
data class EncryptedSelectionJsonV(
    val selectionId: String,
    val sequenceOrder: Int,
    val encrypted_vote: ElGamalCiphertextJson,
)

fun CiphertextBallot.publishJsonE(): EncryptedBallotJsonV {
    val contests = this.contests.map { pcontest ->

        EncryptedContestJsonV(
            pcontest.contestId,
            pcontest.sequenceOrder,
            pcontest.selections.map {
                EncryptedSelectionJsonV(
                    it.selectionId,
                    it.sequenceOrder,
                    it.ciphertext.publishJson(),
                )
            })
    }
    return EncryptedBallotJsonV(this.ballotId, this.ballotNonce.publishJson(), contests)
}

fun EncryptedBallotJsonV.import(group: GroupContext, electionId: UInt256): EncryptedBallotFacade {
    val contests = this.contests.map { contest ->
        EncryptedContestFacade(contest.contestId, contest.sequenceOrder,
            contest.selections.map { EncryptedSelectionFacade(it.selectionId, it.sequenceOrder,
                it.encrypted_vote.import(group) ?: throw IllegalArgumentException("EncryptedBallotJsonV response encrypted_vote for ${it.selectionId}")) })
    }
    return EncryptedBallotFacade(this.ballotId, contests, BallotState.CAST, electionId)
}

// a simplified version of as EncryptedBallot, implementing EncryptedBallotIF
data class EncryptedBallotFacade(
    override val ballotId: String,
    override val contests: List<EncryptedContestFacade>,
    override val state: BallotState,
    override val electionId: UInt256
) : EncryptedBallotIF

data class EncryptedContestFacade(
    override val contestId: String,
    override val sequenceOrder: Int,
    override val selections: List<EncryptedSelectionFacade>,
) : EncryptedBallotIF.Contest

data class EncryptedSelectionFacade(
    override val selectionId: String,
    override val sequenceOrder: Int,
    override val encryptedVote: ElGamalCiphertext,
) : EncryptedBallotIF.Selection

// create a ManifestIF from EncryptedBallotJson
class EncryptedBallotJsonManifestFacade(ballot : EncryptedBallotJsonV) : ManifestIF {
    override val contests : List<ContestFacade>
    init {
        this.contests = ballot.contests.map { bc ->
            ContestFacade(
                bc.contestId,
                bc.sequenceOrder,
                bc.selections.map { SelectionFacade(it.selectionId, it.sequenceOrder)}
            )
        }
    }

    override fun contestsForBallotStyle(ballotStyle : String) = contests
    override fun findContest(contestId: String): ManifestIF.Contest? {
        return contests.find{ it.contestId == contestId }
    }

    override fun contestLimit(contestId: String): Int {
        return contests.find{ it.contestId == contestId }?.votesAllowed?: 1
    }
    override fun optionLimit(contestId : String) : Int {
        return contests.find{ it.contestId == contestId }?.optionLimit?: 1
    }

    class ContestFacade(
        override val contestId: String,
        override val sequenceOrder: Int,
        override val selections: List<ManifestIF.Selection>,
        val votesAllowed: Int = 1,
        val optionLimit: Int = 1,
    ) : ManifestIF.Contest

    class SelectionFacade(
        override val selectionId: String,
        override val sequenceOrder: Int
    ) : ManifestIF.Selection

}