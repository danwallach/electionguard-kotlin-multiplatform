package electionguard.testvectors

import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.json.ElementModQJson
import electionguard.json.import
import electionguard.json.publishJson
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.regeneratePolynomial
import kotlinx.serialization.Serializable

@Serializable
data class TrusteeJson(
    val id: String,
    val xCoordinate: Int,
    val polynomial_coefficients: List<ElementModQJson>,
    val keyShare: ElementModQJson,
    val missing: Boolean,
)

fun KeyCeremonyTrustee.publishJsonE(missing : Boolean): TrusteeJson {
    return TrusteeJson(
        this.idAttribute,
        this.xCoordinateAttribute,
        this.polynomial.coefficients.map { it.publishJson() },
        this.computeSecretKeyShare().publishJson(),
        missing,
    )
}

fun TrusteeJson.importKeyCeremonyTrustee(group: GroupContext, nguardians: Int): KeyCeremonyTrustee {
    return KeyCeremonyTrustee(
        group,
        this.id,
        this.xCoordinate,
        nguardians,
        polynomial_coefficients.size,
        group.regeneratePolynomial(
            this.id,
            this.xCoordinate,
            this.polynomial_coefficients.map { it.import(group) ?: throw IllegalArgumentException("TrusteeJson malformed polynomial_coefficients") },
        )
    )
}

fun TrusteeJson.importDecryptingTrustee(group: GroupContext): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(
        this.id,
        this.xCoordinate,
        group.gPowP(this.polynomial_coefficients[0].import(group) ?: throw IllegalArgumentException("TrusteeJson malformed polynomial_coefficients")),
        this.keyShare.import(group) ?: throw IllegalArgumentException("TrusteeJson malformed keyShare"),
        )
}