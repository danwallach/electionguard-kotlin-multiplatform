package electionguard.decryptBallot

import com.github.michaelbull.result.*
import electionguard.core.*
import electionguard.model.ContestDataStatus
import electionguard.model.EncryptedBallot
import electionguard.model.PlaintextBallot
import electionguard.model.decryptWithNonceToContestData

/** Decryption of an EncryptedBallot using the ballot nonce. */
class DecryptWithNonce(val group : GroupContext, val publicKey: ElGamalPublicKey, val extendedBaseHash: UInt256) {

    fun EncryptedBallot.decrypt(ballotNonce: UInt256): Result<PlaintextBallot, String> {
        require(!this.isPreencrypt)

        val (plaintextContests, cerrors) = this.contests.map { decryptContestWithPrimaryNonce(ballotNonce, it) }.partition()
        if (cerrors.isNotEmpty()) {
            return Err(cerrors.joinToString("\n"))
        }

        return Ok(PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintextContests,
            null
        ))
    }

    private fun decryptContestWithPrimaryNonce(
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest,
    ): Result<PlaintextBallot.Contest, String> {

        val decryptions = mutableListOf<PlaintextBallot.Selection>()
        val errors = mutableListOf<String>()
        for (selection in contest.selections) {
            val dSelection = decryptSelectionWithPrimaryNonce(ballotNonce, contest.sequenceOrder, selection)
            if (dSelection == null) {
                errors.add(" decryption with nonce error for contest: '${contest.contestId}' selection: '${selection.selectionId}'")
            } else {
                decryptions.add(dSelection)
            }
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString("\n"))
        }

        // contest data
        val contestDataResult = contest.contestData.decryptWithNonceToContestData(
            publicKey,
            extendedBaseHash,
            contest.contestId,
            contest.sequenceOrder,
            ballotNonce)

        if (contestDataResult is Err) {
            return contestDataResult
        }
        val contestData = contestDataResult.unwrap()

        // on overvote, modify selections to use original votes
        val useSelections = if (contestData.status == ContestDataStatus.over_vote) {
            // set the selections to the original
            decryptions.map { dselection ->
                if (contestData.overvotes.find { it == dselection.sequenceOrder } == null) dselection
                else dselection.copy(vote = 1)
            }
        } else {
            decryptions
        }

        return Ok(PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            useSelections,
            contestData.writeIns,
        ))
    }

    private fun decryptSelectionWithPrimaryNonce(
        ballotNonce: UInt256,
        contestIndex: Int,
        selection: EncryptedBallot.Selection
    ): PlaintextBallot.Selection? {
        // spec 2.0, eq 25
        val selectionNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, selection.sequenceOrder)
        val decodedVote: Int? = selection.encryptedVote.decryptWithNonce(publicKey, selectionNonce.toElementModQ(group))

        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }

}