package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.model.*
import electionguard.util.ErrorMessages
import electionguard.util.Stats
import electionguard.util.getSystemTimeInMillis

private const val doVerifierSelectionProof = true

/** Turn an EncryptedTally into a DecryptedTallyOrBallot, after all the partial decryptions have been done. */
internal class TallyDecryptor(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val publicKey: ElGamalPublicKey,
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>,
    val guardians: Guardians, // all the guardians
    ) {
    /** Called after gathering the shares and challenge responses for all available trustees. */
    fun decryptTally(
        tally: EncryptedTally,
        decryptions: AllDecryptions,
        stats: Stats,
        errs : ErrorMessages,
    ): DecryptedTallyOrBallot? {
        val contests = tally.contests.map { decryptContest(it, decryptions, stats, errs.nested("Contest ${it.contestId}")) }
        return if (errs.hasErrors()) null else DecryptedTallyOrBallot(tally.tallyId, contests.filterNotNull(), tally.electionId)
    }

    private fun decryptContest(
        contest: EncryptedTally.Contest,
        decryptions: AllDecryptions,
        stats: Stats,
        errs : ErrorMessages,
    ): DecryptedTallyOrBallot.Contest? {
        val results = decryptions.contestData[contest.contestId]
        val decryptedContestData = decryptContestData(contest.contestId, results, errs.nested("decryptContestData"))

        val selections = contest.selections.map {
            val id = "${contest.contestId}#@${it.selectionId}"
            val shares = decryptions.shares[id]
            if (shares == null) errs.addNull("'$id' share not found") as DecryptedTallyOrBallot.Selection?
            else decryptSelection(it, shares, contest.contestId, stats, errs.nested("Selection ${it.selectionId}"))
        }
        return if (errs.hasErrors()) null else DecryptedTallyOrBallot.Contest(contest.contestId, selections.filterNotNull(), decryptedContestData)
    }

    private fun decryptContestData(
        contestId: String,
        contestDataDecryptions: ContestDataResults?, // results for this selection
        errs : ErrorMessages,
    ): DecryptedTallyOrBallot.DecryptedContestData? {
        return contestDataDecryptions?.let {
            // v = Sum(v_i mod q); spec 2.0.0 eq (85)
            val response: ElementModQ = with(group) { contestDataDecryptions.responses.values.map { it }.addQ() }

            // finally we can create the proof
            if (contestDataDecryptions.collectiveChallenge == null) {
                errs.add("missing challenge")
                return null
            }
            val challenge = contestDataDecryptions.collectiveChallenge!!.toElementModQ(group)
            val proof = ChaumPedersenProof(challenge, response)

            if (contestDataDecryptions.beta == null) {
                errs.add("missing beta")
                return null
            }

            // use beta to do the decryption
            val contestData =
                contestDataDecryptions.hashedCiphertext.decryptWithBetaToContestData(
                    publicKey,
                    extendedBaseHash,
                    contestId,
                    contestDataDecryptions.beta!!
                )
            if (contestData is Err) {
                return null
            }

            val decryptedContestData = DecryptedTallyOrBallot.DecryptedContestData(
                contestData.unwrap(),
                contestDataDecryptions.hashedCiphertext,
                proof,
                contestDataDecryptions.beta!!,
            )

            if (doVerifierSelectionProof) {
                if (!decryptedContestData.verifyContestData()) {
                    errs.add("verifyContestData error on $contestId")
                    contestDataDecryptions.checkIndividualResponses(errs.nested("checkIndividualResponses"))
                }
            } else { // Otherwise do the individual guardian verifications, which costs 4*n exponents
                contestDataDecryptions.checkIndividualResponses(errs.nested("checkIndividualResponses"))
            }

            decryptedContestData
        }
    }

    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        selectionDecryptions: DecryptionResults, // results for this selection
        contestId: String,
        stats: Stats,
        errs : ErrorMessages,
    ): DecryptedTallyOrBallot.Selection? {
        // v = Sum(v_i mod q); spec 2.0.0 eq 76
        val response: ElementModQ = with(group) { selectionDecryptions.responses.values.addQ() }
        // finally we can create the proof
        val proof = ChaumPedersenProof(selectionDecryptions.collectiveChallenge!!.toElementModQ(group), response)
        val T = selection.encryptedVote.data / selectionDecryptions.M!! // "decrypted value" T = B · M −1, spec 2.0 eq 64

        val decrypytedSelection = DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selectionDecryptions.tally!!,
            T,
            selection.encryptedVote,
            proof
        )

        val startVerify = getSystemTimeInMillis()

        // check the proof verification, whose cost is 4 exponents (3 powP and 1 accPowp)
        // If it fails, then do the individual guardian verification, hopefully to pinpoint the culprit.
        // If it succeeds, dont have to do the individual verification
        if (doVerifierSelectionProof) {
            if (!decrypytedSelection.verifySelection()) {
                errs.add("verifySelection error on $contestId/${selection.selectionId}")
                selectionDecryptions.checkIndividualResponses(errs.nested("checkIndividualResponses"))
            }
        } else { // Otherwise do the individual guardian verifications, which costs 4*n exponents
            selectionDecryptions.checkIndividualResponses(errs.nested("checkIndividualResponses"))
        }

        stats.of("verifySelection", "selection", "selections").accum(getSystemTimeInMillis() - startVerify, 1)
        return if (errs.hasErrors()) null else decrypytedSelection
    }

    // this is the verifier proof (box 8)
    private fun DecryptedTallyOrBallot.Selection.verifySelection(): Boolean {
        return this.proof.verifyDecryption(extendedBaseHash, publicKey.key, this.encryptedVote, this.bOverM)
    }

    // Verify with spec 2.0.0 eq 74, 75
    private fun DecryptionResults.checkIndividualResponses(errs : ErrorMessages) {
        for (partialDecryption in this.shares.values) {
            val guardianId = partialDecryption.guardianId
            val vi = this.responses[guardianId]
            if (vi == null) {
                errs.add("*** response not found for ${guardianId}")
                continue
            }
            val lagrangeCoordinate = lagrangeCoordinates[guardianId]
            if (lagrangeCoordinate == null) {
                errs.add("*** lagrangeCoordinate not found for ${guardianId}")
                continue
            }
            val wi = lagrangeCoordinate.lagrangeCoefficient
            val ci = wi * this.collectiveChallenge!!.toElementModQ(group) // spec 2.0.0 eq 72 (recalc)

            val inner = guardians.getGexpP(guardianId) // inner factor
            val ap = group.gPowP(vi) * (inner powP ci) // eq 74
            if (partialDecryption.a != ap) {
                errs.add("ai != ai' dont match for ${guardianId}")
            }

            val bp = (this.ciphertext.pad powP vi) * (partialDecryption.Mi powP ci) // eq 75
            if (partialDecryption.b != bp) {
                errs.add("bi != bi' dont match for ${guardianId}")
            }
        }
    }

    private fun DecryptedTallyOrBallot.DecryptedContestData.verifyContestData(): Boolean {
        return this.proof.verifyContestDataDecryption(publicKey.key, extendedBaseHash, this.beta, this.encryptedContestData)
    }

    // HashedElGamalCiphertext instead of ElGamalCiphertext
    private fun ContestDataResults.checkIndividualResponses(errs : ErrorMessages) {
        for (partialDecryption in this.shares.values) {
            val guardianId = partialDecryption.guardianId
            val vi = this.responses[guardianId]
            if (vi == null) {
                errs.add("*** response not found for ${guardianId}")
                continue
            }
            val lagrangeCoordinate = lagrangeCoordinates[guardianId]
            if (lagrangeCoordinate == null) {
                errs.add("*** lagrangeCoordinate not found for ${guardianId}")
                continue
            }
            val wi = lagrangeCoordinate.lagrangeCoefficient
            val ci = wi * this.collectiveChallenge!!.toElementModQ(group) // spec 2.0.0 eq 81.5, p 42

            val inner = guardians.getGexpP(guardianId) // inner factor
            val ap = group.gPowP(vi) * (inner powP ci) // eq 83
            if (partialDecryption.a != ap) {
                errs.add("ai != ai' dont match for i=${guardianId}")
            }

            // this.ciphertext.pad -> this.hashedCiphertext.c0 is the only difference
            val bp = (this.hashedCiphertext.c0 powP vi) * (partialDecryption.Mi powP ci) // eq 84
            if (partialDecryption.b != bp) {
                errs.add("bi != bi' dont match for i=${guardianId}")
            }
        }
    }
}