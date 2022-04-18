@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class PlaintextTally(
    val tallyId: String = "",
    val contests: List<electionguard.protogen.PlaintextTallyContest> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTally =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTally> {
        public val defaultInstance: electionguard.protogen.PlaintextTally by
            lazy { electionguard.protogen.PlaintextTally() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTally =
            electionguard.protogen.PlaintextTally.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTally, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "tally_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "tallyId",
                            value = electionguard.protogen.PlaintextTally::tallyId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "contests",
                            number = 2,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.PlaintextTallyContest>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .PlaintextTallyContest
                                                            .Companion
                                                )
                                    ),
                            jsonName = "contests",
                            value = electionguard.protogen.PlaintextTally::contests
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextTally",
                    messageClass = electionguard.protogen.PlaintextTally::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class PlaintextTallyContest(
    val contestId: String = "",
    val selections: List<electionguard.protogen.PlaintextTallySelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.PlaintextTallyContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.PlaintextTallyContest> {

        public val defaultInstance: electionguard.protogen.PlaintextTallyContest by
            lazy { electionguard.protogen.PlaintextTallyContest() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.PlaintextTallyContest =
            electionguard.protogen.PlaintextTallyContest.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<electionguard.protogen.PlaintextTallyContest, *>
                    >(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "contest_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "contestId",
                            value = electionguard.protogen.PlaintextTallyContest::contestId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selections",
                            number = 2,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.PlaintextTallySelection>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .PlaintextTallySelection
                                                            .Companion
                                                )
                                    ),
                            jsonName = "selections",
                            value = electionguard.protogen.PlaintextTallyContest::selections
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextTallyContest",
                    messageClass = electionguard.protogen.PlaintextTallyContest::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class PlaintextTallySelection(
    val selectionId: String = "",
    val tally: Int = 0,
    val value: electionguard.protogen.ElementModP? = null,
    val message: electionguard.protogen.ElGamalCiphertext? = null,
    val shares: List<electionguard.protogen.CiphertextDecryptionSelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.PlaintextTallySelection = protoMergeImpl(other)
    override val descriptor:
        pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallySelection>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.PlaintextTallySelection> {

        public val defaultInstance: electionguard.protogen.PlaintextTallySelection by
            lazy { electionguard.protogen.PlaintextTallySelection() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.PlaintextTallySelection =
            electionguard.protogen.PlaintextTallySelection.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallySelection> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<electionguard.protogen.PlaintextTallySelection, *>
                    >(5)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selection_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "selectionId",
                            value = electionguard.protogen.PlaintextTallySelection::selectionId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "tally",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "tally",
                            value = electionguard.protogen.PlaintextTallySelection::tally
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 3,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModP.Companion
                                    ),
                            jsonName = "value",
                            value = electionguard.protogen.PlaintextTallySelection::value
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "message",
                            number = 4,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElGamalCiphertext.Companion
                                    ),
                            jsonName = "message",
                            value = electionguard.protogen.PlaintextTallySelection::message
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "shares",
                            number = 5,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.CiphertextDecryptionSelection>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .CiphertextDecryptionSelection
                                                            .Companion
                                                )
                                    ),
                            jsonName = "shares",
                            value = electionguard.protogen.PlaintextTallySelection::shares
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextTallySelection",
                    messageClass = electionguard.protogen.PlaintextTallySelection::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class CiphertextDecryptionSelection(
    val selectionId: String = "",
    val guardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val proofOrParts: ProofOrParts<*>? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    public sealed class ProofOrParts<V>(value: V) : pbandk.Message.OneOf<V>(value) {
        public class Proof(proof: electionguard.protogen.GenericChaumPedersenProof) :
            ProofOrParts<electionguard.protogen.GenericChaumPedersenProof>(proof)
        public class RecoveredParts(recoveredParts: electionguard.protogen.RecoveredParts) :
            ProofOrParts<electionguard.protogen.RecoveredParts>(recoveredParts)
    }

    val proof: electionguard.protogen.GenericChaumPedersenProof?
        get() = (proofOrParts as? ProofOrParts.Proof)?.value
    val recoveredParts: electionguard.protogen.RecoveredParts?
        get() = (proofOrParts as? ProofOrParts.RecoveredParts)?.value

    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.CiphertextDecryptionSelection = protoMergeImpl(other)
    override val descriptor:
        pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.CiphertextDecryptionSelection> {

        public val defaultInstance: electionguard.protogen.CiphertextDecryptionSelection by
            lazy { electionguard.protogen.CiphertextDecryptionSelection() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.CiphertextDecryptionSelection =
            electionguard.protogen.CiphertextDecryptionSelection.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<
                            electionguard.protogen.CiphertextDecryptionSelection,
                            *
                        >
                    >(5)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selection_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "selectionId",
                            value =
                                electionguard.protogen.CiphertextDecryptionSelection::selectionId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "guardian_id",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "guardianId",
                            value = electionguard.protogen.CiphertextDecryptionSelection::guardianId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "share",
                            number = 3,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModP.Companion
                                    ),
                            jsonName = "share",
                            value = electionguard.protogen.CiphertextDecryptionSelection::share
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "proof",
                            number = 4,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen
                                                .GenericChaumPedersenProof
                                                .Companion
                                    ),
                            oneofMember = true,
                            jsonName = "proof",
                            value = electionguard.protogen.CiphertextDecryptionSelection::proof
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "recovered_parts",
                            number = 5,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.RecoveredParts.Companion
                                    ),
                            oneofMember = true,
                            jsonName = "recoveredParts",
                            value =
                                electionguard.protogen.CiphertextDecryptionSelection::recoveredParts
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "CiphertextDecryptionSelection",
                    messageClass = electionguard.protogen.CiphertextDecryptionSelection::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class RecoveredParts(
    val fragments: List<electionguard.protogen.CiphertextCompensatedDecryptionSelection> =
        emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RecoveredParts =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredParts>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RecoveredParts> {
        public val defaultInstance: electionguard.protogen.RecoveredParts by
            lazy { electionguard.protogen.RecoveredParts() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RecoveredParts =
            electionguard.protogen.RecoveredParts.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredParts> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RecoveredParts, *>>(1)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "fragments",
                            number = 1,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<
                                        electionguard.protogen.CiphertextCompensatedDecryptionSelection
                                    >(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .CiphertextCompensatedDecryptionSelection
                                                            .Companion
                                                )
                                    ),
                            jsonName = "fragments",
                            value = electionguard.protogen.RecoveredParts::fragments
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "RecoveredParts",
                    messageClass = electionguard.protogen.RecoveredParts::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class CiphertextCompensatedDecryptionSelection(
    val selectionId: String = "",
    val guardianId: String = "",
    val missingGuardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val recoveryKey: electionguard.protogen.ElementModP? = null,
    val proof: electionguard.protogen.GenericChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.CiphertextCompensatedDecryptionSelection = protoMergeImpl(other)
    override val descriptor:
        pbandk.MessageDescriptor<electionguard.protogen.CiphertextCompensatedDecryptionSelection>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.CiphertextCompensatedDecryptionSelection> {

        public val defaultInstance: electionguard.protogen.CiphertextCompensatedDecryptionSelection
            by lazy { electionguard.protogen.CiphertextCompensatedDecryptionSelection() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.CiphertextCompensatedDecryptionSelection =
            electionguard.protogen.CiphertextCompensatedDecryptionSelection.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<
                electionguard.protogen.CiphertextCompensatedDecryptionSelection
            > by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<
                            electionguard.protogen.CiphertextCompensatedDecryptionSelection,
                            *
                        >
                    >(6)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selection_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "selectionId",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::selectionId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "guardian_id",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "guardianId",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::guardianId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "missing_guardian_id",
                            number = 3,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "missingGuardianId",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::missingGuardianId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "share",
                            number = 4,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModP.Companion
                                    ),
                            jsonName = "share",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::share
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "recovery_key",
                            number = 5,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModP.Companion
                                    ),
                            jsonName = "recoveryKey",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::recoveryKey
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "proof",
                            number = 6,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen
                                                .GenericChaumPedersenProof
                                                .Companion
                                    ),
                            jsonName = "proof",
                            value =
                                electionguard.protogen
                                    .CiphertextCompensatedDecryptionSelection::proof
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "CiphertextCompensatedDecryptionSelection",
                    messageClass =
                        electionguard.protogen.CiphertextCompensatedDecryptionSelection::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTally")
public fun PlaintextTally?.orDefault(): electionguard.protogen.PlaintextTally =
    this ?: PlaintextTally.defaultInstance

private fun PlaintextTally.protoMergeImpl(plus: pbandk.Message?): PlaintextTally =
    (plus as? PlaintextTally)
        ?.let {
            it.copy(
                contests = contests + plus.contests,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTally.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTally {
    var tallyId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextTallyContest>? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> tallyId = _fieldValue as String
                2 ->
                    contests =
                        (contests ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.PlaintextTallyContest>
                            }
            }
        }
    return PlaintextTally(tallyId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallyContest")
public fun PlaintextTallyContest?.orDefault(): electionguard.protogen.PlaintextTallyContest =
    this ?: PlaintextTallyContest.defaultInstance

private fun PlaintextTallyContest.protoMergeImpl(plus: pbandk.Message?): PlaintextTallyContest =
    (plus as? PlaintextTallyContest)
        ?.let {
            it.copy(
                selections = selections + plus.selections,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTallyContest.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): PlaintextTallyContest {
    var contestId = ""
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextTallySelection>? =
        null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> contestId = _fieldValue as String
                2 ->
                    selections =
                        (selections ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.PlaintextTallySelection>
                            }
            }
        }
    return PlaintextTallyContest(
        contestId,
        pbandk.ListWithSize.Builder.fixed(selections),
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallySelection")
public fun PlaintextTallySelection?.orDefault(): electionguard.protogen.PlaintextTallySelection =
    this ?: PlaintextTallySelection.defaultInstance

private fun PlaintextTallySelection.protoMergeImpl(plus: pbandk.Message?): PlaintextTallySelection =
    (plus as? PlaintextTallySelection)
        ?.let {
            it.copy(
                value = value?.plus(plus.value) ?: plus.value,
                message = message?.plus(plus.message) ?: plus.message,
                shares = shares + plus.shares,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTallySelection.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): PlaintextTallySelection {
    var selectionId = ""
    var tally = 0
    var value: electionguard.protogen.ElementModP? = null
    var message: electionguard.protogen.ElGamalCiphertext? = null
    var shares: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextDecryptionSelection>? =
        null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> selectionId = _fieldValue as String
                2 -> tally = _fieldValue as Int
                3 -> value = _fieldValue as electionguard.protogen.ElementModP
                4 -> message = _fieldValue as electionguard.protogen.ElGamalCiphertext
                5 ->
                    shares =
                        (shares ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<
                                            electionguard.protogen.CiphertextDecryptionSelection
                                        >
                            }
            }
        }
    return PlaintextTallySelection(
        selectionId,
        tally,
        value,
        message,
        pbandk.ListWithSize.Builder.fixed(shares),
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextDecryptionSelection")
public fun CiphertextDecryptionSelection?.orDefault():
    electionguard.protogen.CiphertextDecryptionSelection =
        this ?: CiphertextDecryptionSelection.defaultInstance

private fun CiphertextDecryptionSelection.protoMergeImpl(
    plus: pbandk.Message?
): CiphertextDecryptionSelection =
    (plus as? CiphertextDecryptionSelection)
        ?.let {
            it.copy(
                share = share?.plus(plus.share) ?: plus.share,
                proofOrParts =
                    when {
                        proofOrParts is CiphertextDecryptionSelection.ProofOrParts.Proof &&
                            plus.proofOrParts is CiphertextDecryptionSelection.ProofOrParts.Proof ->
                            CiphertextDecryptionSelection.ProofOrParts
                                .Proof(proofOrParts.value + plus.proofOrParts.value)
                        proofOrParts is CiphertextDecryptionSelection.ProofOrParts.RecoveredParts &&
                            plus.proofOrParts is
                                CiphertextDecryptionSelection.ProofOrParts.RecoveredParts ->
                            CiphertextDecryptionSelection.ProofOrParts
                                .RecoveredParts(proofOrParts.value + plus.proofOrParts.value)
                        else -> plus.proofOrParts ?: proofOrParts
                    },
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextDecryptionSelection.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): CiphertextDecryptionSelection {
    var selectionId = ""
    var guardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var proofOrParts: CiphertextDecryptionSelection.ProofOrParts<*>? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> selectionId = _fieldValue as String
                2 -> guardianId = _fieldValue as String
                3 -> share = _fieldValue as electionguard.protogen.ElementModP
                4 ->
                    proofOrParts =
                        CiphertextDecryptionSelection.ProofOrParts
                            .Proof(_fieldValue as electionguard.protogen.GenericChaumPedersenProof)
                5 ->
                    proofOrParts =
                        CiphertextDecryptionSelection.ProofOrParts
                            .RecoveredParts(_fieldValue as electionguard.protogen.RecoveredParts)
            }
        }
    return CiphertextDecryptionSelection(
        selectionId,
        guardianId,
        share,
        proofOrParts,
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForRecoveredParts")
public fun RecoveredParts?.orDefault(): electionguard.protogen.RecoveredParts =
    this ?: RecoveredParts.defaultInstance

private fun RecoveredParts.protoMergeImpl(plus: pbandk.Message?): RecoveredParts =
    (plus as? RecoveredParts)
        ?.let {
            it.copy(
                fragments = fragments + plus.fragments,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun RecoveredParts.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RecoveredParts {
    var fragments:
        pbandk.ListWithSize.Builder<
            electionguard.protogen.CiphertextCompensatedDecryptionSelection
        >? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 ->
                    fragments =
                        (fragments ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<
                                            electionguard.protogen.CiphertextCompensatedDecryptionSelection
                                        >
                            }
            }
        }
    return RecoveredParts(pbandk.ListWithSize.Builder.fixed(fragments), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextCompensatedDecryptionSelection")
public fun CiphertextCompensatedDecryptionSelection?.orDefault():
    electionguard.protogen.CiphertextCompensatedDecryptionSelection =
        this ?: CiphertextCompensatedDecryptionSelection.defaultInstance

private fun CiphertextCompensatedDecryptionSelection.protoMergeImpl(
    plus: pbandk.Message?
): CiphertextCompensatedDecryptionSelection =
    (plus as? CiphertextCompensatedDecryptionSelection)
        ?.let {
            it.copy(
                share = share?.plus(plus.share) ?: plus.share,
                recoveryKey = recoveryKey?.plus(plus.recoveryKey) ?: plus.recoveryKey,
                proof = proof?.plus(plus.proof) ?: plus.proof,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextCompensatedDecryptionSelection.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): CiphertextCompensatedDecryptionSelection {
    var selectionId = ""
    var guardianId = ""
    var missingGuardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var recoveryKey: electionguard.protogen.ElementModP? = null
    var proof: electionguard.protogen.GenericChaumPedersenProof? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> selectionId = _fieldValue as String
                2 -> guardianId = _fieldValue as String
                3 -> missingGuardianId = _fieldValue as String
                4 -> share = _fieldValue as electionguard.protogen.ElementModP
                5 -> recoveryKey = _fieldValue as electionguard.protogen.ElementModP
                6 -> proof = _fieldValue as electionguard.protogen.GenericChaumPedersenProof
            }
        }
    return CiphertextCompensatedDecryptionSelection(
        selectionId,
        guardianId,
        missingGuardianId,
        share,
        recoveryKey,
        proof,
        unknownFields
    )
}
