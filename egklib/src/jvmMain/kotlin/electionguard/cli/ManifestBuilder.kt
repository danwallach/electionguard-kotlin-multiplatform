package electionguard.cli

import electionguard.model.Manifest
import electionguard.model.protocolVersion

/** Build test Manifests */
class ManifestBuilder(val manifestName: String = electionScopeId, styleDef : String = styleDefault) {
    val districtDefault = "district9"

    private val contests = ArrayList<ContestBuilder>()
    private val candidates = HashMap<String, Manifest.Candidate>()
    private var style = styleDef
    var districts = ArrayList<Manifest.GeopoliticalUnit>()
    var ballotStyles = ArrayList<Manifest.BallotStyle>()

    fun addGpunit(gpunitName: String): ManifestBuilder {
        districts.add(Manifest.GeopoliticalUnit(gpunitName, "name", Manifest.ReportingUnitType.congressional, null))
        return this
    }

    fun setDefaultStyle(style: String): ManifestBuilder {
        this.style = style
        return this
    }

    fun addStyle(style: String, vararg gpunits: String): ManifestBuilder {
        for (gpunit in gpunits) {
            districts.add(Manifest.GeopoliticalUnit(gpunit, "name", Manifest.ReportingUnitType.congressional, null))
        }
        ballotStyles.add(Manifest.BallotStyle(style, gpunits.asList(), emptyList(), null))
        return this
    }

    fun addCandidateAndParty(candidate_id: String, party: String?): ManifestBuilder {
        val c = Manifest.Candidate(candidate_id, null, party, null, false)
        candidates[candidate_id] = c
        return this
    }

    fun addBallotStyle(ballotStyle: Manifest.BallotStyle): ManifestBuilder {
        ballotStyles.add(ballotStyle)
        return this
    }

    fun addContest(contest_id: String): ContestBuilder {
        val c = ContestBuilder(contest_id)
        contests.add(c)
        return c
    }

    fun addContest(contest_id: String, seq: Int): ContestBuilder {
        val c = ContestBuilder(contest_id).setSequence(seq)
        contests.add(c)
        return c
    }

    fun addCandidate(candidate_id: String) {
        val c = Manifest.Candidate(candidate_id)
        candidates[candidate_id] = c
    }

    fun removeCandidate(candidate_id: String): ManifestBuilder {
        candidates.remove(candidate_id)
        return this
    }

    fun build(): Manifest {
        if (districts.isEmpty()) {
            districts.add(
                Manifest.GeopoliticalUnit(
                    districtDefault,
                    "districtName",
                    Manifest.ReportingUnitType.precinct,
                    null
                )
            )
        }
        if (ballotStyles.isEmpty()) {
            ballotStyles.add(Manifest.BallotStyle(style, listOf(districtDefault), emptyList(), null))
        }
        val parties: List<Manifest.Party> = listOf(Manifest.Party("dog"), Manifest.Party("cat"))
        return Manifest(
            manifestName,
            protocolVersion,
            Manifest.ElectionType.general,
            "start",
            "end",
            districts,
            parties,
            candidates.values.toList(),
            contests.map { it.build() },
            ballotStyles,
            emptyList(),
            Manifest.ContactInformation("contact", emptyList(), null, "911"),
        )
    }

    private var contest_seq = 0
    private var selection_seq = 0

    inner class ContestBuilder internal constructor(val id: String) {
        private var seq: Int = contest_seq++
        private val selections = ArrayList<SelectionBuilder>()
        private var type: Manifest.VoteVariationType = Manifest.VoteVariationType.one_of_m
        private var allowed = 1
        private var number_elected = 1
        private var district: String = districtDefault

        fun setVoteVariationType(type: Manifest.VoteVariationType, allowed: Int, numberElected: Int? = null): ContestBuilder {
            require(allowed > 0)
            this.type = type
            this.allowed = allowed
            this.number_elected = numberElected?: allowed
            return this
        }

        fun setGpunit(gpunit: String): ContestBuilder {
            district = gpunit
            return this
        }

        fun setSequence(seq: Int): ContestBuilder {
            this.seq = seq
            return this
        }

        fun addSelection(id: String, candidateId: String): ContestBuilder {
            val s  = SelectionBuilder(id, candidateId)
            selections.add(s)
            addCandidate(candidateId)
            return this
        }

        fun addSelection(id: String, candidateId: String, seq: Int): ContestBuilder {
            val s: SelectionBuilder = SelectionBuilder(id, candidateId).setSequence(seq)
            selections.add(s)
            addCandidate(candidateId)
            return this
        }

        fun done(): ManifestBuilder {
            return this@ManifestBuilder
        }

        fun build(): Manifest.ContestDescription {
            //         val contestId: String,
            //        val sequenceOrder: Int,
            //        val geopoliticalUnitId: String,
            //        val voteVariation: VoteVariationType,
            //        val numberElected: Int,
            //        val votesAllowed: Int,
            //        val name: String,
            //        val selections: List<SelectionDescription>,
            //        val ballotTitle: String?,
            //        val ballotSubtitle: String?,
            return Manifest.ContestDescription(
                id, seq, district,
                type, allowed, number_elected, id,
                selections.map { it.build() },
                "title", "subtitle",
            )
        }

        inner class SelectionBuilder internal constructor(private val id: String, private val candidate_id: String) {
            private var seq: Int = selection_seq++
            fun setSequence(seq: Int): SelectionBuilder {
                this.seq = seq
                return this
            }

            fun build(): Manifest.SelectionDescription {
                return Manifest.SelectionDescription(id, seq, candidate_id)
            }
        }
    }

    companion object {
        const val styleDefault = "ballotStyle"
        const val electionScopeId = "TestManifest"
    }
}

fun buildTestManifest(ncontests: Int, nselections: Int) : Manifest {
    val builder = ManifestBuilder()
    var ccount = 1
    List(ncontests) {
        val contestBuilder = builder.addContest("contest${it+1}", it+1)
        List(nselections) { selIdx ->
            contestBuilder.addSelection("selection${ccount}", "candidate${ccount}", selIdx+1)
            ccount++
        }
        contestBuilder.done()
    }
    return builder.build()
}