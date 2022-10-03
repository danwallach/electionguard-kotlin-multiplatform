package electionguard.encrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.constantChaumPedersenProofKnownNonce
import electionguard.core.disjunctiveChaumPedersenProofKnownNonce
import electionguard.core.encrypt
import electionguard.core.encryptedSum
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The manifest is expected to have passed manifest validation (see ManifestInputValidation).
 * The input ballots are expected to have passed ballot validation.
 * See RunBatchEncryption and BallotInputValidation to validate ballots before passing them to this class.
 */
class Encryptor(
    val group: GroupContext,
    val manifest: Manifest,
    private val elgamalPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: UInt256,
) {
    private val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)

    /** Encrypt ballots in a chain with starting codeSeed, and random masterNonce */
    fun encrypt(ballots: Iterable<PlaintextBallot>, codeSeed: ElementModQ): List<CiphertextBallot> {
        var previousTrackingHash = codeSeed
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            val encryptedBallot = ballot.encryptBallot(previousTrackingHash, group.randomElementModQ())
            encryptedBallots.add(encryptedBallot)
            previousTrackingHash = encryptedBallot.code.toElementModQ(group)
        }
        return encryptedBallots
    }

    /** Encrypt ballots with fixed codeSeed, masterNonce, and timestamp. */
    fun encryptWithFixedNonces(
        ballots: Iterable<PlaintextBallot>,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ
    ): List<CiphertextBallot> {
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            encryptedBallots.add(ballot.encryptBallot(codeSeed, masterNonce, 0))
        }
        return encryptedBallots
    }

    /** Encrypt the ballot with the given codeSeed and master nonce and an optional timestamp override. */
    fun encrypt(
        ballot: PlaintextBallot,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ,
        timestampOverride: Long? = null // if null, use getSystemTimeInMillis()
    ): CiphertextBallot {
        return ballot.encryptBallot(codeSeed, masterNonce, timestampOverride)
    }

    private fun PlaintextBallot.encryptBallot(
        codeSeed: ElementModQ,
        masterNonce: ElementModQ, // usually random
        timestampOverride: Long? = null,
    ): CiphertextBallot {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHashUInt256(), this.ballotId, masterNonce)
        val plaintextContests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contests) {
            // If no contest on the ballot, create a well formed contest with all zeroes
            val pcontest: PlaintextBallot.Contest = plaintextContests[mcontest.contestId] ?: makeZeroContest(mcontest)
            encryptedContests.add(pcontest.encryptContest(mcontest, ballotNonce))
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }

        // see spec 1.51, section 3.3.6
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        val cryptoHash = hashElements(ballotId, manifest.cryptoHashUInt256(), sortedContests) // B_i
        val trackingCode = hashElements(codeSeed, timestamp, cryptoHash)

        return CiphertextBallot(
            ballotId,
            ballotStyleId,
            manifest.cryptoHashUInt256(),
            codeSeed.toUInt256(),
            trackingCode,
            sortedContests,
            timestamp,
            cryptoHash,
            masterNonce,
        )
    }

    private fun makeZeroContest(mcontest: Manifest.ContestDescription): PlaintextBallot.Contest {
        val selections = mcontest.selections.map { makeZeroSelection(it.selectionId, it.sequenceOrder, false) }
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    /**
     * Encrypt a PlaintextBallotContest into CiphertextBallot.Contest.
     * @param mcontest:    the corresponding Manifest.ContestDescription
     * @param ballotNonce: the seed for this contest.
     */
    private fun PlaintextBallot.Contest.encryptContest(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[mcontest.sequenceOrder]
        val chaumPedersenNonce = nonceSequence[0]

        val ballotSelections = this.selections.associateBy { it.selectionId }

        var votes = 0
        val votedFor = mutableListOf<Int>()
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {
            // Find the ballot selection matching the contest description.
            val plaintextSelection = ballotSelections[mselection.selectionId]
            if (plaintextSelection != null && plaintextSelection.vote > 0) {
                votedFor.add(plaintextSelection.sequenceOrder)
                votes += plaintextSelection.vote
            }
        }

        val totalVotedFor = votedFor.size + this.writeIns.size
        val status = if (totalVotedFor == 0) ContestDataStatus.null_vote
            else if (totalVotedFor < mcontest.votesAllowed)  ContestDataStatus.under_vote
            else if (totalVotedFor > mcontest.votesAllowed)  ContestDataStatus.over_vote
            else ContestDataStatus.normal

        val encryptedSelections = mutableListOf<CiphertextBallot.Selection>()
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {
            var plaintextSelection = ballotSelections[mselection.selectionId]

            // Set vote to zero if not in manifest or this contest is overvoted
            if (plaintextSelection == null || (status == ContestDataStatus.over_vote)) {
                plaintextSelection = makeZeroSelection(mselection.selectionId, mselection.sequenceOrder, false)
            }
            encryptedSelections.add(plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                false,
            ))
        }

        // TODO remove placeholders
        // Add a placeholder selection for each possible vote in the contest
        val limit = mcontest.votesAllowed
        val selectionSequenceOrderMax = mcontest.selections.maxOf { it.sequenceOrder }
        for (placeholder in 1..limit) {
            val sequenceNo = selectionSequenceOrderMax + placeholder
            val plaintextSelection = makeZeroSelection(
                "${mcontest.contestId}-$sequenceNo", sequenceNo, votes < limit
            )
            val mselection = Manifest.SelectionDescription(
                plaintextSelection.selectionId,
                plaintextSelection.sequenceOrder,
                "placeholder"
            )
            val encryptedPlaceholder = plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                true,
            )
            encryptedSelections.add(encryptedPlaceholder)
            votes++
        }

        val contestData = ContestData(
            if (status == ContestDataStatus.over_vote) votedFor else emptyList(),
            this.writeIns,
            status
        )

        // TODO: option to pass master nonce-derived nonce to contestData.encrypt(), see issue #168
        return mcontest.encryptContest(
            group,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            contestNonce,
            chaumPedersenNonce,
            encryptedSelections.sortedBy { it.sequenceOrder },
            contestData.encrypt(elgamalPublicKey, mcontest.votesAllowed),
        )
    }

    private fun makeZeroSelection(selectionId: String, sequenceOrder: Int, voteFor : Boolean): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            if (voteFor) 1 else 0,
        )
    }

    /**
     * Encrypt a PlaintextBallot.Selection into a CiphertextBallot.Selection
     *
     * @param selectionDescription:         the Manifest selection
     * @param contestNonce:                 aka "nonce seed"
     * @param isPlaceholder:                if this is a placeholder selection
     */
    private fun PlaintextBallot.Selection.encryptSelection(
        selectionDescription: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        isPlaceholder: Boolean = false,
    ): CiphertextBallot.Selection {
        val nonceSequence = Nonces(selectionDescription.cryptoHash.toElementModQ(group), contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence[0]
        val selectionNonce: ElementModQ = nonceSequence[selectionDescription.sequenceOrder]

        return selectionDescription.encryptSelection(
            this.vote,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            disjunctiveChaumPedersenNonce,
            selectionNonce,
            isPlaceholder,
        )
    }
}

////  share with Encryptor, BallotPrecompute, ContestPrecompute
fun Manifest.ContestDescription.encryptContest(
    group: GroupContext,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    contestNonce: ElementModQ,
    chaumPedersenNonce: ElementModQ,
    encryptedSelections: List<CiphertextBallot.Selection>,
    extendedDataCiphertext: HashedElGamalCiphertext,
): CiphertextBallot.Contest {

    val cryptoHash = hashElements(this.contestId, this.cryptoHash, encryptedSelections)
    val texts: List<ElGamalCiphertext> = encryptedSelections.map { it.ciphertext }
    val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.constantChaumPedersenProofKnownNonce(
        this.votesAllowed,
        aggNonce,
        elgamalPublicKey,
        chaumPedersenNonce,
        cryptoExtendedBaseHashQ,
    )

    return CiphertextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.cryptoHash, // manifest contest cryptohash
        encryptedSelections,
        cryptoHash,      // CiphertextBallot.Contest cryptohash
        proof,
        contestNonce,
        extendedDataCiphertext,
    )
}

fun Manifest.SelectionDescription.encryptSelection(
    vote: Int,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    disjunctiveChaumPedersenNonce: ElementModQ,
    selectionNonce: ElementModQ,
    isPlaceholder: Boolean = false,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(elgamalPublicKey, selectionNonce)

    val proof = elgamalEncryption.disjunctiveChaumPedersenProofKnownNonce(
        vote,
        selectionNonce,
        elgamalPublicKey,
        disjunctiveChaumPedersenNonce,
        cryptoExtendedBaseHashQ
    )

    val cryptoHash = hashElements(this.selectionId, this.cryptoHash, elgamalEncryption.cryptoHashUInt256())

    return CiphertextBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.cryptoHash,
        elgamalEncryption,
        cryptoHash,
        isPlaceholder,
        proof,
        selectionNonce,
    )
}