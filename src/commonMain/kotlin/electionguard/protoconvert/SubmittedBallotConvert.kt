package electionguard.protoconvert

import electionguard.ballot.SubmittedBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext

data class SubmittedBallotConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.SubmittedBallot): SubmittedBallot {
        return SubmittedBallot(
            proto.objectId,
            proto.ballotStyleId,
            convertElementModQ(proto.manifestHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertElementModQ(proto.trackingHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertElementModQ(proto.previousTrackingHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proto.contests.map{ convertContest(it) },
            proto.timestamp,
            convertElementModQ(proto.cryptoHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertElementModQ(proto.nonce?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertBallotState(proto.state),
        )
    }

    private fun convertBallotState(proto: electionguard.protogen.SubmittedBallot.BallotBoxState): SubmittedBallot.BallotState {
        return SubmittedBallot.BallotState.valueOf(proto.name?: throw IllegalArgumentException("BallotBoxState cannot be null"))
    }

    private fun convertContest(proto: electionguard.protogen.CiphertextBallotContest): SubmittedBallot.Contest {
        return SubmittedBallot.Contest(
            proto.objectId,
            proto.sequenceOrder,
            convertElementModQ(proto.descriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proto.selections.map{ convertSelection(it) },
            convertElementModQ(proto.cryptoHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.ciphertextAccumulation?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertElementModQ(proto.nonce?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertConstantChaumPedersenProof(proto.proof?: throw IllegalArgumentException("Selection value cannot be null")),
        )
    }

    private fun convertSelection(proto: electionguard.protogen.CiphertextBallotSelection): SubmittedBallot.Selection {
        return SubmittedBallot.Selection(
            proto.objectId,
            proto.sequenceOrder,
            convertElementModQ(proto.descriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.ciphertext?: throw IllegalArgumentException("Selection message cannot be null"), groupContext),
            convertElementModQ(proto.cryptoHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proto.isPlaceholderSelection,
            convertElementModQ(proto.nonce?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertDisjunctiveChaumPedersenProof(proto.proof?: throw IllegalArgumentException("Selection value cannot be null")),
            convertCiphertext(proto.extendedData?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
        )
    }

    fun convertConstantChaumPedersenProof(proof: electionguard.protogen.ConstantChaumPedersenProof): ConstantChaumPedersenProofKnownNonce? {
        return ConstantChaumPedersenProofKnownNonce(
            GenericChaumPedersenProof(
                convertElementModP(proof.pad?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModP(proof.data?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.challenge?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.response?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            ),
            proof.constant
        )
    }

    fun convertDisjunctiveChaumPedersenProof(proof: electionguard.protogen.DisjunctiveChaumPedersenProof): DisjunctiveChaumPedersenProofKnownNonce? {
        return DisjunctiveChaumPedersenProofKnownNonce(
            GenericChaumPedersenProof(
                convertElementModP(proof.proofZeroPad?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModP(proof.proofZeroData?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.proofZeroChallenge?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.proofZeroResponse?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            ),
            GenericChaumPedersenProof(
                convertElementModP(proof.proofOnePad?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModP(proof.proofOneData?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.proofOneChallenge?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
                convertElementModQ(proof.proofOneResponse?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            ),
            convertElementModQ(proof.challenge?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
        )
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(ballot: SubmittedBallot): electionguard.protogen.SubmittedBallot {
        return electionguard.protogen.SubmittedBallot(
            ballot.objectId,
            ballot.ballotStyleId,
            convertElementModQ(ballot.manifestHash),
            convertElementModQ(ballot.trackingHash),
            convertElementModQ(ballot.previousTrackingHash),
            ballot.contests.map{ convertContest(it) },
            ballot.timestamp,
            convertElementModQ(ballot.cryptoHash),
            if (ballot.nonce == null) { null } else { convertElementModQ(ballot.nonce) },
            convertBallotState(ballot.state)
        )
    }

    private fun convertBallotState(type: SubmittedBallot.BallotState ): electionguard.protogen.SubmittedBallot.BallotBoxState{
        return electionguard.protogen.SubmittedBallot.BallotBoxState.fromName(type.name)
    }

    private fun convertContest(contest: SubmittedBallot.Contest): electionguard.protogen.CiphertextBallotContest {
        return electionguard.protogen.CiphertextBallotContest(
                contest.contestId,
                contest.sequenceOrder,
            convertElementModQ(contest.contestHash),
            contest.ballotSelections.map{ convertSelection(it) },
            convertElementModQ(contest.cryptoHash),
            convertCiphertext(contest.encryptedTotal),
            if (contest.nonce == null) { null } else { convertElementModQ(contest.nonce) },
            if (contest.proof == null) { null } else { convertConstantChaumPedersenProof(contest.proof) },
        )
    }

    private fun convertSelection(selection: SubmittedBallot.Selection): electionguard.protogen.CiphertextBallotSelection {
        return electionguard.protogen.CiphertextBallotSelection(
                selection.selectionId,
                selection.sequenceOrder,
                convertElementModQ(selection.descriptionHash),
                convertCiphertext(selection.ciphertext),
            convertElementModQ(selection.cryptoHash),
            selection.isPlaceholderSelection,
            if (selection.nonce == null) { null } else { convertElementModQ(selection.nonce) },
            if (selection.proof == null) { null } else { convertDisjunctiveChaumPedersenProof(selection.proof) },
            if (selection.extendedData == null) { null } else { convertCiphertext(selection.extendedData) },
            )
    }

    fun convertConstantChaumPedersenProof(proof: ConstantChaumPedersenProofKnownNonce):  electionguard.protogen.ConstantChaumPedersenProof {
        return electionguard.protogen.ConstantChaumPedersenProof(
                convertElementModP(proof.proof.a),
                convertElementModP(proof.proof.b),
                convertElementModQ(proof.proof.c),
                convertElementModQ(proof.proof.r),
            proof.constant
        )
    }

    fun convertDisjunctiveChaumPedersenProof(proof: DisjunctiveChaumPedersenProofKnownNonce): electionguard.protogen.DisjunctiveChaumPedersenProof {
        return electionguard.protogen.DisjunctiveChaumPedersenProof(
            convertElementModP(proof.proof0.a),
            convertElementModP(proof.proof0.b),
            convertElementModP(proof.proof1.a),
            convertElementModP(proof.proof1.b),
            convertElementModQ(proof.proof0.c),
            convertElementModQ(proof.proof0.r),
            convertElementModQ(proof.c),
            convertElementModQ(proof.proof1.c),
            convertElementModQ(proof.proof1.r),
        )
    }
}