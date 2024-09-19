package electionguard.decryptBallot

import com.github.michaelbull.result.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.model.*
import electionguard.preencrypt.PreEncryptor

/** Decryption of a preencrypted EncryptedBallot using the ballot nonce. */
class DecryptPreencryptWithNonce(
    val group: GroupContext,
    val manifest: ManifestIF,
    val publicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256,
    sigma : (UInt256) -> String, // hash trimming function Ω
) {
    private val preEncryptor = PreEncryptor( group, manifest, publicKey.key, extendedBaseHash, sigma)

    fun EncryptedBallot.decrypt(ballotNonce: UInt256): Result<PlaintextBallot, String> {
        require(this.isPreencrypt)

        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, ballotNonce)

        val results : List<Result<PlaintextBallot.Contest, String>> = this.contests.map {
            val pcontest = preEncryptedBallot.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (pcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContest(ballotNonce, it, pcontest)
            }
        }
        val (plaintextContests, cerrors) = results.partition()

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

    private fun decryptContest(
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest,
        pcontest: PreEncryptedContest,
    ): Result<PlaintextBallot.Contest, String> {

        val decryptions: List<PlaintextBallot.Selection> = decryptPreencryption(contest, pcontest)

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

    private fun decryptPreencryption (
        contest: EncryptedBallot.Contest,
        preeContest: PreEncryptedContest,
    ): List<PlaintextBallot.Selection> {
        val nselections = contest.selections.size
        val preEncryption = contest.preEncryption!!

        val combinedNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            val componentNonces = preEncryption.selectedVectors.map { selected ->
                val pv: PreEncryptedSelection = preeContest.selections.find { it.shortCode == selected.shortCode }!!
                pv.selectionNonces[idx]
            }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedNonces.add( aggNonce )
        }

        return contest.selections.mapIndexed { idx, selection ->
            val decodedVote = selection.encryptedVote.decryptWithNonce(publicKey, combinedNonces[idx])
            PlaintextBallot.Selection(selection.selectionId, selection.sequenceOrder, decodedVote!!)
        }
    }

}