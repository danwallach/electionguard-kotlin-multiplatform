package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.decryptWithNonce
import electionguard.core.get
import electionguard.core.hashElements
import electionguard.core.toElementModQ

/** Decryption of a EncryptedBallot using the master nonce. */
class DecryptionWithMasterNonce(val group : GroupContext, val manifest: Manifest, val publicKey: ElGamalPublicKey) {

    fun EncryptedBallot.decrypt(masterNonce: ElementModQ): Result<PlaintextBallot, String> {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHash, this.ballotId, masterNonce)

        val (plaintextContests, cerrors) = this.contests.map {
            val mcontest = manifest.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (mcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContestWithMasterNonce(mcontest, ballotNonce, it)
            }
        }.partition()

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

    private fun decryptContestWithMasterNonce(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest
    ): Result<PlaintextBallot.Contest, String> {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[0]

        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        val errors = mutableListOf<String>()
        for (selection in contest.selections.filter { !it.isPlaceholderSelection }) {
            val mselection = mcontest.selections.find { it.selectionId == selection.selectionId }
            if (mselection == null) {
                errors.add(" Cant find selection ${selection.selectionId} in contest ${mcontest.contestId}")
                continue
            }
            val plaintextSelection = decryptSelectionWithMasterNonce(mselection, contestNonce, selection)
            if (plaintextSelection == null) {
                errors.add(" decryption with master nonce failed for contest: ${contest.contestId} selection: ${selection.selectionId}")
            } else {
                plaintextSelections.add(plaintextSelection)
            }
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString("\n"))
        }
        return Ok(PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        ))
    }

    private fun decryptSelectionWithMasterNonce(
        mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        selection: EncryptedBallot.Selection
    ): PlaintextBallot.Selection? {
        val nonceSequence = Nonces(mselection.cryptoHash.toElementModQ(group), contestNonce)
        val selectionNonce: ElementModQ = nonceSequence[1]

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }
}