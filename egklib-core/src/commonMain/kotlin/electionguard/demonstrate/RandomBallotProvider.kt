package electionguard.demonstrate

import electionguard.model.Manifest
import electionguard.model.PlaintextBallot
import kotlin.random.Random

/** Create nballots randomly generated fake Ballots, used for testing.  */
class RandomBallotProvider(val manifest: Manifest, val nballots: Int = 11) {
    var addWriteIns = false
    var useSequential = false
    var sequentialId = 1

    fun withWriteIns() : RandomBallotProvider {
        this.addWriteIns = true
        return this
    }

    fun withSequentialIds(): RandomBallotProvider {
        this.useSequential = true
        return this
    }

    fun ballots(ballotStyleId: String? = null): List<PlaintextBallot> {
        val ballots = mutableListOf<PlaintextBallot>()
        val useStyle = ballotStyleId ?: manifest.ballotStyles[0].ballotStyleId
        for (i in 0 until nballots) {
            val ballotId = if (useSequential) "id-" + sequentialId++ else "id" + Random.nextInt()
            ballots.add(getFakeBallot(manifest, useStyle, ballotId))
        }
        return ballots
    }

    fun makeBallot(): PlaintextBallot {
        val useStyle = manifest.ballotStyles[0].ballotStyleId
        val ballotId = if (useSequential) "id-" + sequentialId++ else "id" + Random.nextInt()
        return getFakeBallot(manifest, useStyle, ballotId)
    }

    fun getFakeBallot(manifest: Manifest, ballotStyleId: String, ballotId: String): PlaintextBallot {
        if (manifest.ballotStyles.find { it.ballotStyleId == ballotStyleId } == null) {
            throw RuntimeException("BallotStyle '$ballotStyleId' not found in manifest ballotStyles= ${manifest.ballotStyles}")
        }
        val contests = mutableListOf<PlaintextBallot.Contest>()
        for (contestp in manifest.contestsForBallotStyle(ballotStyleId)!!) {
            contests.add(makeContestFrom(contestp as Manifest.ContestDescription))
        }
        val sn = Random.nextInt(1000)
        return PlaintextBallot(ballotId, ballotStyleId, contests, sn.toLong())
    }

    fun makeContestFrom(contest: Manifest.ContestDescription): PlaintextBallot.Contest {
        var voted = 0
        val selections =  mutableListOf<PlaintextBallot.Selection>()
        val nselections = contest.selections.size

        for (selection_description in contest.selections) {
            val selection: PlaintextBallot.Selection = getRandomVoteForSelection(selection_description, nselections, voted < contest.votesAllowed)
            selections.add(selection)
            voted += selection.vote
        }
        val choice = Random.nextInt(nselections)
        val writeins = if (!addWriteIns || choice != 0) emptyList() else {
            listOf("writein")
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            selections,
            writeins,
        )
    }

    companion object {
        fun getRandomVoteForSelection(description: Manifest.SelectionDescription, nselections: Int, moreAllowed : Boolean): PlaintextBallot.Selection {
            val choice = Random.nextInt(nselections)
            return PlaintextBallot.Selection(
                description.selectionId, description.sequenceOrder,
                if (choice == 0 && moreAllowed) 1 else 0,
            )
        }
    }
}