package electionguard.json

import electionguard.model.ContestData
import electionguard.model.ContestDataStatus
import electionguard.model.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class DecryptedTallyOrBallotJson(
    val id: String,
    val contests: List<DecryptedContestJson>,
    val election_id: UInt256Json,     // unique election identifier
)

@Serializable
data class DecryptedContestJson(
    val contest_id: String,
    val selections: List<DecryptedSelectionJson>,
    val decrypted_contest_data: DecryptedContestDataJson?, //  ballot decryption only
)

@Serializable
data class DecryptedSelectionJson(
    val selection_id: String,
    val tally: Int,
    val b_over_m: ElementModPJson, // eq 65
    val encrypted_vote: ElGamalCiphertextJson,
    val proof: ChaumPedersenJson,
)

fun DecryptedTallyOrBallot.publishJson() = DecryptedTallyOrBallotJson(
    this.id,
    this.contests.map { contest ->
        DecryptedContestJson(
            contest.contestId,
            contest.selections.map { selection ->
                DecryptedSelectionJson(
                    selection.selectionId,
                    selection.tally,
                    selection.bOverM.publishJson(),
                    selection.encryptedVote.publishJson(),
                    selection.proof.publishJson(),
                )
            },
            contest.decryptedContestData?.publishJson(),
        )
    },
    this.electionId.publishJson(),
)

/////////////////////////////////////////////////////////////////////////////////////////////

fun DecryptedTallyOrBallotJson.import(group: GroupContext, errs: ErrorMessages): DecryptedTallyOrBallot? {
    val contests = this.contests.map { it.import(group, errs.nested("DecryptedContestJson ${it.contest_id}")) }
    val electionId = this.election_id.import() ?: errs.addNull("malformed election_id") as UInt256?

    return if (errs.hasErrors()) null
    else DecryptedTallyOrBallot(
        this.id,
        contests.filterNotNull(),
        electionId!!,
    )
}

fun DecryptedContestJson.import(group: GroupContext, errs: ErrorMessages): DecryptedTallyOrBallot.Contest? {
    val selections = this.selections.map { it.import(group, errs.nested("DecryptedSelectionJson ${it.selection_id}")) }
    val decryptedContestData = if (this.decrypted_contest_data == null) null else
        this.decrypted_contest_data.import(group, errs.nested("DecryptedContestDataJson"))

    return if (errs.hasErrors()) null
    else DecryptedTallyOrBallot.Contest(
            this.contest_id,
            selections.filterNotNull(),
            decryptedContestData,
        )
}

fun DecryptedSelectionJson.import(group: GroupContext, errs: ErrorMessages): DecryptedTallyOrBallot.Selection? {
    val bOverM = this.b_over_m.import(group) ?: errs.addNull("malformed b_over_m") as ElementModP?
    val encryptedVote = this.encrypted_vote.import(group) ?: errs.addNull("malformed encrypted_vote") as ElGamalCiphertext?
    val proof = this.proof.import(group, errs.nested("Proof"))

    return if (errs.hasErrors()) null
    else DecryptedTallyOrBallot.Selection(
        this.selection_id,
        this.tally,
        bOverM!!,
        encryptedVote!!,
        proof!!,
    )
}

@Serializable
data class DecryptedContestDataJson(
    val contest_data: ContestDataJson,
    val encrypted_contest_data: HashedElGamalCiphertextJson,  // matches EncryptedBallotContest.encrypted_contest_data
    val proof: ChaumPedersenJson,
    val beta: ElementModPJson, //  β = C0^s mod p ; needed to verify 10.2
)

fun DecryptedTallyOrBallot.DecryptedContestData.publishJson() = DecryptedContestDataJson(
    this.contestData.publishJson(),
    this.encryptedContestData.publishJson(),
    this.proof.publishJson(),
    this.beta.publishJson(),
)

fun DecryptedContestDataJson.import(group: GroupContext, errs : ErrorMessages): DecryptedTallyOrBallot.DecryptedContestData? {
    val beta = this.beta.import(group) ?: errs.addNull("malformed beta") as ElementModP?
    val encryptedContestData = this.encrypted_contest_data.import(group) ?: errs.addNull("malformed encrypted_contest_data") as HashedElGamalCiphertext?
    val proof = this.proof.import(group, errs.nested("Proof"))

    return if (errs.hasErrors()) null
    else DecryptedTallyOrBallot.DecryptedContestData(
        this.contest_data.import(),
        encryptedContestData!!,
        proof!!,
        beta!!,
    )
}

// (incomplete) strawman for contest data (section 3.3.7)
// "The contest data can contain different kinds of information such as undervote, null vote, and
// overvote information together with the corresponding selections, the text captured for write-in
// options and other data associated to the contest."
@Serializable
data class ContestDataJson(
    val over_votes: List<Int>,  // list of selection sequence_number for this contest
    val write_ins: List<String>, //  list of write_in strings
    val status: String,
)

fun ContestData.publishJson() =
    ContestDataJson(this.overvotes, this.writeIns, this.status.name)

fun ContestDataJson.import() =
    ContestData(this.over_votes, this.write_ins, ContestDataStatus.valueOf(this.status))
