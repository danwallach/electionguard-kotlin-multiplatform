package electionguard.model

import electionguard.core.ElGamalCiphertext
import electionguard.core.UInt256

/** Interface used in the crypto routines for easy mocking. */
interface EncryptedBallotIF {

    val ballotId: String
    val electionId : UInt256
    val contests: List<Contest>
    val state: EncryptedBallot.BallotState

    interface Contest {
        val contestId: String
        val sequenceOrder: Int
        val selections: List<Selection>
    }

    interface Selection {
        val selectionId: String
        val sequenceOrder: Int
        val encryptedVote: ElGamalCiphertext
    }
}
