package electionguard.preencrypt

import electionguard.core.*

/**
 * Intermediate working ballot to transform pre encrypted ballot to an Encrypted ballot.
 * Not externally visable
 */
internal data class PreBallot(
    val ballotId: String,
    val contests: List<PreContest>,
) {
    fun show() {
        println("\nRecordPreBallot '$ballotId' ")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.selectedCodes()}")
            println("   contestHash = ${contest.contestHash.toHex()}")
            println("   selectionHashes (${contest.allSelectionHashes.size}) = ${contest.allSelectionHashes}")
            println("   selectedVectors (${contest.selectedVectors.size}) =")
            contest.selectedVectors.forEach { println("    $it")}
        }
    }
}

data class PreContest(
    val contestId: String,
    val contestHash: UInt256,  // (95)
    val allSelectionHashes: List<UInt256>, // nselections + limit, numerically sorted
    val selectedVectors: List<PreSelectionVector>, // limit number of them, sorted by selectionHash
) {
    fun selectedCodes() : List<String> = selectedVectors.map { it.shortCode }
}

data class PreSelectionVector(
    val selectionId: String, // do not serialize
    val selectionHash: ElementModQ, // ψi (93)
    val shortCode: String,
    val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
) {
    override fun toString() =
        buildString {
            append(" shortCode=$shortCode")
            append(" selectionHash=$selectionHash\n")
            encryptions.forEach { append("       encryption $it\n") }
        }
}

internal fun MarkedPreEncryptedBallot.makePreBallot(preeBallot : PreEncryptedBallot): PreBallot {
    val contests = mutableListOf<PreContest>()
    preeBallot.contests.forEach { preeContest ->
        val markedContest = this.contests.find { it.contestId == preeContest.contestId }
            ?: throw IllegalArgumentException("Cant find ${preeContest.contestId}")

        // find the selections
        val selections = mutableListOf<PreEncryptedSelection>()
        markedContest.selectedCodes.map { selectedShortCode ->
            val selection = preeContest.selections.find { it.shortCode == selectedShortCode } ?: throw RuntimeException()
            if (selection != null) selections.add(selection)
        }

        // add null vector on undervote
        val votesMissing = preeContest.votesAllowed - selections.size
        repeat (votesMissing) {
            val nullVector = findNullVectorNotSelected(preeContest.selections, selections)
            selections.add(nullVector)
        }
        require (selections.size == preeContest.votesAllowed)

        // The selectionVectors are sorted numerically by selectionHash, so cant be associated with a selection
        val sortedSelectedVectors = selections.sortedBy { it.selectionHash }
        val sortedRecordedVectors = sortedSelectedVectors.map { preeSelection ->
            PreSelectionVector(preeSelection.selectionId, preeSelection.selectionHash, preeSelection.shortCode, preeSelection.selectionVector)
        }
        val allSortedSelectedHashes = preeContest.selections.sortedBy { it.selectionHash }.map { it.selectionHash.toUInt256() }

        contests.add(
            //     val contestId: String,
            //    val contestHash: UInt256,
            //    val allSelectionHashes: List<UInt256>, // nselections + limit, numerically sorted
            //    val selectedVectors: List<RecordedSelectionVector> = emptyList(), // limit number of them, sorted by selectionHash
            PreContest(
                preeContest.contestId,
                preeContest.preencryptionHash,
                allSortedSelectedHashes,
                sortedRecordedVectors,
            )
        )
    }

    return PreBallot(
        this.ballotId,
        contests,
    )
}

// find a null vector not already in selections
private fun findNullVectorNotSelected(allSelections : List<PreEncryptedSelection>, selections : List<PreEncryptedSelection>) : PreEncryptedSelection {
    allSelections.forEach { it ->
        if (it.selectionId.startsWith("null")) {
            if (null == selections.find{ have -> have.selectionId == it.selectionId }) {
                return it
            }
        }
    }
    throw RuntimeException()
}