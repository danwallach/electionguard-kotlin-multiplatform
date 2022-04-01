package electionguard.ballot

/**
 * The plaintext representation of a voter's ballot selections for all the contests in an election.
 * The ballotId is a unique Ballot ID created by the external system. This is used both as input,
 * and for the roundtrip: input -> encrypt -> decrypt -> output.
 */
data class PlaintextBallot(
    val ballotId: String,
    val ballotStyleId: String, // matches BallotStyle.ballotStyleId
    val contests: List<Contest>
) {

    /** The plaintext representation of a voter's selections for one contest. */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val sequenceOrder: Int,
        val selections: List<Selection>,
        val maxSelections: Int // maximum allowed by the contest's definition
    )

    /** The plaintext representation of one selection for a particular contest. */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int,
        val vote: Int,
        val isPlaceholderSelection: Boolean,
        val extendedData: ExtendedData?,
    )

    /** Used to indicate a write-in candidate. */
    data class ExtendedData(val value: String, val length: Int)
}