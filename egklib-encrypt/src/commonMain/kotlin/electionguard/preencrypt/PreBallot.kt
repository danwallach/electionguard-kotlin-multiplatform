package electionguard.preencrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.UInt256
import electionguard.core.toUInt256safe
import electionguard.model.PreEncryptedBallot
import electionguard.model.PreEncryptedSelection
import electionguard.util.ErrorMessages

/**
 * Intermediate working ballot to transform pre encrypted ballot to an Encrypted ballot.
 * Not externally visible.
 */
internal data class PreBallot(
    val ballotId: String,
    val contests: List<PreContest>,
) {
    fun show() {
        println("\nRecordPreBallot '$ballotId' ")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.selectedCodes()}")
            println("   contestHash = ${contest.preencryptionHash.toHex()}")
            println("   selectionHashes (${contest.allSelectionHashes.size}) = ${contest.allSelectionHashes}")
            println("   selectedVectors (${contest.selectedVectors.size}) =")
            contest.selectedVectors.forEach { println("    $it")}
        }
    }
}

internal data class PreContest(
    val contestId: String,
    val contestIndex: Int,
    val preencryptionHash: UInt256,  // (94)
    val allSelectionHashes: List<UInt256>, // nselections + limit, numerically sorted
    val selectedVectors: List<PreSelectionVector>, // limit number of them, sorted by selectionHash
    val votedFor: List<Boolean> // nselections, in order by sequence_order
) {
    init {
        require(votedFor.size == allSelectionHashes.size - selectedVectors.size) // TODO
    }
    fun selectedCodes() : List<String> = selectedVectors.map { it.shortCode }
    fun nselections() = votedFor.size
}

internal data class PreSelectionVector(
    val selectionId: String, // do not serialize
    val selectionHash: ElementModQ, // ψi (92)
    val shortCode: String,
    val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
    val nonces: List<ElementModQ>, // size = nselections, in order by sequence_order, do not serialize
) {
    override fun toString() =
        buildString {
            append(" shortCode=$shortCode")
            append(" selectionHash=$selectionHash\n")
            encryptions.forEach { append("       encryption $it\n") }
        }
}

internal fun MarkedPreEncryptedBallot.makePreBallot(preeBallot : PreEncryptedBallot, errs : ErrorMessages): PreBallot? {
    val contests = mutableListOf<PreContest>()
    preeBallot.contests.forEach { preeContest ->
        val markedContest = this.contests.find { it.contestId == preeContest.contestId }
        if (markedContest == null) {
            errs.add("Cant find PreContest ${preeContest.contestId}")
            return null
        }

        // find the selected selections by their shortCode
        val preSelections = mutableListOf<PreEncryptedSelection>()
        markedContest.selectedCodes.map { selectedShortCode ->
            val selection = preeContest.selections.find { it.shortCode == selectedShortCode }
            if (selection == null) {
                errs.add("Cant find PreEncryptedSelection $selectedShortCode")
            } else {
                preSelections.add(selection)
            }
        }
        if (errs.hasErrors()) return null

        val nselections = preeContest.selections.size - preeContest.votesAllowed
        val votedFor = mutableListOf<Boolean>()
        repeat(nselections) { idx ->
            val selection = preeContest.selections[idx]
            votedFor.add( preSelections.find { it.selectionId == selection.selectionId } != null)
        }

        // add null vectors on undervote
        val votesMissing = preeContest.votesAllowed - preSelections.size
        repeat (votesMissing) {
            val nullVector = findNullVectorNotSelected(preeContest.selections, preSelections)
            if (nullVector == null) {
                errs.add("Cant find NullVector idx=$it")
            } else {
                preSelections.add(nullVector)
            }
        }
        if (errs.hasErrors()) return null
        if (preSelections.size != preeContest.votesAllowed) {
            errs.add("preSelections.size ${preSelections.size } != preeContest.votesAllowed ${preeContest.votesAllowed}")
            return null
        }

        // The selectionVectors are sorted numerically by selectionHash, so cant be associated with a selection
        val sortedSelectedVectors = preSelections.sortedBy { it.selectionHash }
        val sortedRecordedVectors = sortedSelectedVectors.map { preeSelection ->
            PreSelectionVector(preeSelection.selectionId, preeSelection.selectionHash, preeSelection.shortCode,
                preeSelection.selectionVector, preeSelection.selectionNonces)
        }
        val allSortedSelectedHashes = preeContest.selections.sortedBy { it.selectionHash }.map { it.selectionHash.toUInt256safe() }

        contests.add(
             PreContest(
                 preeContest.contestId,
                 preeContest.sequenceOrder,
                 preeContest.preencryptionHash,
                 allSortedSelectedHashes,
                 sortedRecordedVectors,
                 votedFor,
            )
        )
    }

    return PreBallot(
        this.ballotId,
        contests,
    )
}

// find a null vector not already in selections
private fun findNullVectorNotSelected(allSelections : List<PreEncryptedSelection>, selections : List<PreEncryptedSelection>) : PreEncryptedSelection? {
    allSelections.forEach {
        if (it.selectionId.startsWith("null")) {
            if (null == selections.find{ have -> have.selectionId == it.selectionId }) {
                return it
            }
        }
    }
    return null
}