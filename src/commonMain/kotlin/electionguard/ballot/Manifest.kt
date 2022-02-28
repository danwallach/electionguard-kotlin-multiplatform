package electionguard.ballot

import electionguard.core.*


/**
 * The Election Manifest: defines the candidates, contests, and associated information for a specific election.
 * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/election)
 */
data class Manifest(
    val groupContext : GroupContext,
    val electionScopeId: String,
    val specVersion: String,
    val electionType: ElectionType,
    val startDate: String, // LocalDateTime,
    val endDate: String, // LocalDateTime,
    val geopoliticalUnits: List<GeopoliticalUnit>,
    val parties: List<Party>,
    val candidates: List<Candidate>,
    val contests: List<ContestDescription>,
    val ballotStyles: List<BallotStyle>,
    val name: InternationalizedText?,
    val contactInformation: ContactInformation?
) : CryptoHashableElement {

    override fun cryptoHashElement(): ElementModQ {
        return groupContext.hashElements(
            electionScopeId,
            electionType.name,
            startDate.toString(), // to_iso_date_string
            endDate.toString(),
            name,
            contactInformation,
            geopoliticalUnits,
            parties,
            candidates,
            contests,
            ballotStyles,
        )
    }

    /**
     * The type of election.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/election-type)
     */
    enum class ElectionType {
        unknown,

        /**
         * For an election held typically on the national day for elections.
         */
        general,

        /**
         * For a primary election that is for a specific party where voter eligibility is based on registration.
         */
        partisan_primary_closed,

        /**
         * For a primary election that is for a specific party where voter declares desired party or chooses in private.
         */
        partisan_primary_open,

        /**
         * For a primary election without a specified type, such as a nonpartisan primary.
         */
        primary,

        /**
         * For an election to decide a prior contest that ended with no candidate receiving a majority of the votes.
         */
        runoff,

        /**
         * For an election held out of sequence for special circumstances, for example, to fill a vacated office.
         */
        special,

        /**
         * Used when the election type is not listed in this enumeration. If used, include a specific value of the OtherType element.
         */
        other
    }

    /**
     * The type of geopolitical unit.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/reporting-unit-type)
     */
    enum class ReportingUnitType {
        unknown,

        /**
         * Used to report batches of ballots that might cross precinct boundaries.
         */
        ballot_batch,

        /**
         * Used for a ballot-style area that's generally composed of precincts.
         */
        ballot_style_area,

        /**
         * Used as a synonym for a county.
         */
        borough,

        /**
         * Used for a city that reports results or for the district that encompasses it.
         */
        city,

        /**
         * Used for city council districts.
         */
        city_council,

        /**
         * Used for one or more precincts that have been combined for the purposes of reporting. If the term ward is
         * used interchangeably with combined precinct, use combined-precinct for the ReportingUnitType.
         */
        combined_precinct,

        /**
         * Used for national legislative body districts.
         */
        congressional,

        /**
         * Used for a country.
         */
        country,

        /**
         * Used for a county or for the district that encompasses it. Synonymous with borough and parish in some localities.
         */
        county,

        /**
         * Used for county council districts.
         */
        county_council,

        /**
         * Used for a dropbox for absentee ballots.
         */
        drop_box,

        /**
         * Used for judicial districts.
         */
        judicial,

        /**
         * Used as applicable for various units such as towns, townships, villages that report votes, or for the
         * district that encompasses them.
         */
        municipality,

        /**
         * Used for a polling place.
         */
        polling_place,

        /**
         * Used if the terms for ward or district are used interchangeably with precinct.
         */
        precinct,

        /**
         * Used for a school district.
         */
        school,

        /**
         * Used for a special district.
         */
        special,

        /**
         * Used for splits of precincts.
         */
        split_precinct,

        /**
         * Used for a state or for the district that encompasses it.
         */
        state,

        /**
         * Used for a state house or assembly district.
         */
        state_house,

        /**
         * Used for a state senate district.
         */
        state_senate,

        /**
         * Used for type of municipality that reports votes or for the district that encompasses it.
         */
        town,

        /**
         * Used for type of municipality that reports votes or for the district that encompasses it.
         */
        township,

        /**
         * Used for a utility district.
         */
        utility,

        /**
         * Used for a type of municipality that reports votes or for the district that encompasses it.
         */
        village,

        /**
         * Used for a vote center.
         */
        vote_center,

        /**
         * Used for combinations or groupings of precincts or other units.
         */
        ward,

        /**
         * Used for a water district.
         */
        water,

        /**
         * Used for other types of reporting units that aren't included in this enumeration.
         * If used, provide the item's custom type in an OtherType element.
         */
        other
    }

    /**
     * Enumeration for contest algorithm or rules in the contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/vote-variation)
     */
    enum class VoteVariationType {
        /**
         * Each voter can select up to one option.
         */
        one_of_m,

        /**
         * Approval voting, where each voter can select as many options as desired.
         */
        approval,

        /**
         * Borda count, where each voter can rank the options, and the rankings are assigned point values.
         */
        borda,

        /**
         * Cumulative voting, where each voter can distribute their vote to up to N options.
         */
        cumulative,

        /**
         * A 1-of-m method where the winner needs more than 50% of the vote to be elected.
         */
        majority,

        /**
         * A method where each voter can select up to N options.
         */
        n_of_m,

        /**
         * A 1-of-m method where the option with the most votes is elected, regardless of whether the option has
         * more than 50% of the vote.
         */
        plurality,

        /**
         * A proportional representation method, which is any system that elects winners in proportion to the total vote.
         * For the single transferable vote (STV) method, use rcv instead.
         */
        proportional,

        /**
         * Range voting, where each voter can select a score for each option.
         */
        range,

        /**
         * Ranked choice voting (RCV), where each voter can rank the options, and the ballots are counted in rounds.
         * Also known as instant-runoff voting (IRV) and the single transferable vote (STV).
         */
        rcv,

        /**
         * A 1-of-m method where the winner needs more than some predetermined fraction of the vote to be elected,
         * and where the fraction is more than 50%. For example, the winner might need three-fifths or two-thirds of the vote.
         */
        super_majority,

        /**
         * The vote variation is a type that isn't included in this enumeration. If used, provide the item's custom type
         * in an OtherType element.
         */
        other
    }

    /**
     * An annotated character string.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/annotated-string)
     */
    data class AnnotatedString(val annotation: String, val value: String, val cryptoHash: ElementModQ) :
        CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /** Classifies a set of contests by their set of parties and geopolitical units  */
    data class BallotStyle(
        val ballotStyleId: String,
        val geopoliticalUnitIds: List<String>,
        val partyIds: List<String>,
        val imageUri: String?,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * A candidate in a contest.
     * Note: The ElectionGuard Data Spec deviates from the NIST model in that selections for any contest type
     * are considered a "candidate". for instance, on a yes-no referendum contest, two `candidate` objects
     * would be included in the model to represent the `affirmative` and `negative` selections for the contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/candidate)
     */
    data class Candidate(
        val candidateId: String,
        val name: InternationalizedText,
        val partyId: String?,
        val imageUri: String?,
        val isWriteIn: Boolean,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * Contact information about persons, boards of authorities, organizations, etc.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/contact-information)
     */
    data class ContactInformation(
        val addressLine: List<String>,
        val email: List<AnnotatedString>,
        val phone: List<AnnotatedString>,
        val name: String?,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * A physical or virtual unit of representation or vote/seat aggregation.
     * Use this entity to define geopolitical units such as cities, districts, jurisdictions, or precincts
     * to associate contests, offices, vote counts, or other information with those geographies.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/gp-unit)
     */
    data class GeopoliticalUnit(
        val geopoliticalUnitId: String,
        val name: String,
        val type: ReportingUnitType,
        val contactInformation: ContactInformation?,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * Text that may have translations in multiple languages.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/internationalized-text)
     */
    data class InternationalizedText(val text: List<Language>, val cryptoHash: ElementModQ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * The ISO-639 language code.
     * @see [ISO 639](https://en.wikipedia.org/wiki/ISO_639)
     */
    data class Language(val value: String, val language: String, val cryptoHash: ElementModQ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * A political party.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/party)
     */
    data class Party(
        val partyId: String,
        val name: InternationalizedText,
        val abbreviation: String?,
        val color: String?,
        val logoUri: String?,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    /**
     * The metadata that describes the structure and type of one contest in the election.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/contest)
     */
    data class ContestDescription(
        val contestId: String,
        val sequenceOrder: Int,
        val geopoliticalUnitId: String,
        val voteVariation: VoteVariationType,
        val numberElected: Int,
        val votesAllowed: Int,
        val name: String,
        val selections: List<SelectionDescription>,
        val ballotTitle: InternationalizedText?,
        val ballotSubtitle: InternationalizedText?,
        val primaryPartyIds: List<String>,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }


    /**
     * A ballot selection for a specific candidate in a contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/ballot-selection)
     */
    data class SelectionDescription(
        val selectionId: String,
        val sequenceOrder: Int,
        val candidateId: String,
        val cryptoHash: ElementModQ
    ) : CryptoHashableElement {
        override fun cryptoHashElement() = cryptoHash
    }

    companion object {

        fun annotatedStringOf(
            groupContext: GroupContext,
            annotation: String,
            value: String
        ): AnnotatedString {
            return AnnotatedString(annotation, value, groupContext.hashElements(annotation, value))
        }

        fun ballotStyleOf(
            groupContext: GroupContext,
            styleId: String,
            geopoliticalUnitIds: List<String>?,
            partyIds: List<String>?,
            imageUri: String?
        ): BallotStyle {

            return BallotStyle(
                styleId,
                geopoliticalUnitIds?: emptyList(),
                partyIds?: emptyList(),
                imageUri,
                groupContext.hashElements(styleId, geopoliticalUnitIds, partyIds, imageUri)
            )
        }

        fun candidateOf(
            groupContext: GroupContext,
            candidateId: String,
            name: InternationalizedText?,
            partyId: String?,
            imageUri: String?,
            isWriteIn: Boolean
        ): Candidate {
            val useName = name ?:
            internationalizedTextOf(groupContext, listOf(makeLanguage(groupContext, "unknown", "en")))
            return Candidate(
                candidateId, useName, partyId, imageUri, isWriteIn,
                groupContext.hashElements(candidateId, name, partyId, imageUri)
            )
        }

        fun contestDescriptionOf(
            groupContext: GroupContext,
            contestId: String,
            sequenceOrder: Int,
            electoralDistrictId: String,
            voteVariation: VoteVariationType,
            numberElected: Int,
            votesAllowed: Int,
            name: String,
            ballotSelections: List<SelectionDescription>,
            ballotTitle: InternationalizedText?,
            ballotSubtitle: InternationalizedText?,
            primaryPartyIds : List<String>
        ): ContestDescription {

            return ContestDescription(
                contestId,
                sequenceOrder,
                electoralDistrictId,
                voteVariation,
                numberElected,
                votesAllowed,
                name,
                ballotSelections,
                ballotTitle,
                ballotSubtitle,
                primaryPartyIds,
                groupContext.hashElements(
                    contestId,
                    electoralDistrictId,
                    sequenceOrder,
                    voteVariation.name,
                    numberElected,
                    votesAllowed,
                    name,
                    ballotSelections,
                    ballotTitle,
                    ballotSubtitle,
                    primaryPartyIds
                )
            )
        }


        fun contactInformationOf(
            groupContext: GroupContext,
            addressLine: List<String>,
            email: List<AnnotatedString>,
            phone: List<AnnotatedString>,
            name: String?
        ): ContactInformation {

            return ContactInformation(
                addressLine, email, phone, name,
                groupContext.hashElements(name, addressLine, email, phone)
            )
        }

        fun geopoliticalUnitOf(
            groupContext: GroupContext,
            unitId: String,
            name: String,
            type: ReportingUnitType,
            contactInformation: ContactInformation?
        ): GeopoliticalUnit {

            return GeopoliticalUnit(
                unitId, name, type, contactInformation,
                groupContext.hashElements(unitId, name, type.name, contactInformation)
            )
        }

        fun internationalizedTextOf(
            groupContext: GroupContext,
            text: List<Language>
        ): InternationalizedText {
            return InternationalizedText(text, groupContext.hashElements(text))
        }

        fun makeLanguage(groupContext: GroupContext, value: String, language: String): Language {
            return Language(value, language, groupContext.hashElements(value, language))
        }

        fun partyOf(
            groupContext: GroupContext,
            partyId: String,
            name: InternationalizedText?,
            abbreviation: String?,
            color: String?,
            logoUri: String?,
        ): Party {
            val useName = name ?:
            internationalizedTextOf(groupContext, listOf(makeLanguage(groupContext, "unknown", "en")))

            return Party(
                partyId, useName, abbreviation, color, logoUri,
                groupContext.hashElements(partyId, name, abbreviation, color, logoUri)
            )
        }

        fun selectionDescriptionOf(
            groupContext: GroupContext,
            selectionId: String,
            sequenceOrder: Int,
            candidateId: String,
        ): SelectionDescription {
            return SelectionDescription(
                selectionId, sequenceOrder, candidateId,
                groupContext.hashElements(selectionId, candidateId, sequenceOrder)
            )
        }
    }

}
