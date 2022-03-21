package electionguard.protoconvert

import electionguard.core.*
import electionguard.core.ElGamalPublicKey
import mu.KotlinLogging
import pbandk.ByteArr
private val logger = KotlinLogging.logger("CommonConvert")

/**
 * Converts a deserialized ElementModQ to the internal representation, returning `null` if the input
 * was missing, out of bounds, or otherwise malformed. `null` is accepted as an input, for
 * convenience. If `null` is passed as input, `null` is returned.
 */
fun GroupContext.importElementModQ(modQ: electionguard.protogen.ElementModQ?): ElementModQ? =
    if (modQ == null) null else this.binaryToElementModQ(modQ.value.array)

// dont actually need the GroupContext
fun GroupContext.importUInt256(modQ: electionguard.protogen.UInt256?): UInt256? =
    if (modQ == null) null else modQ.value.array.toUInt256()

/**
 * Converts a deserialized ElementModP to the internal representation, returning `null` if the input
 * was missing, out of bounds, or otherwise malformed. `null` is accepted as an input, for
 * convenience. If `null` is passed as input, `null` is returned.
 */
fun GroupContext.importElementModP(modP: electionguard.protogen.ElementModP?,): ElementModP? =
    if (modP == null) null else this.binaryToElementModP(modP.value.array)

fun GroupContext.importCiphertext(
    ciphertext: electionguard.protogen.ElGamalCiphertext?,
): ElGamalCiphertext? {
    if (ciphertext == null) return null

    val pad = this.importElementModP(ciphertext.pad)
    val data = this.importElementModP(ciphertext.data)

    if (pad == null || data == null) {
        logger.error { "ElGamalCiphertext pad or value was malformed or out of bounds" }
        return null
    }

    return ElGamalCiphertext(pad, data);
}

fun GroupContext.importChaumPedersenProof(
    proof: electionguard.protogen.ChaumPedersenProof?,
): GenericChaumPedersenProof? {
    // TODO: this should probably be a ConstantChaumPedersenProofKnownSecretKey, which needs to know
    //   what the constant actually is. That should be nearby in the serialized data.

    if (proof == null) return null

    //    val pad = this.importElementModP(proof.pad)
    //    val data = this.importElementModP(proof.data)
    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    if (challenge == null || response == null) {
        logger.error { "one or more ChaumPedersenProof inputs was malformed or out of bounds" }
        return null
    }

    return GenericChaumPedersenProof(challenge, response)
}

fun GroupContext.importSchnorrProof(proof: electionguard.protogen.SchnorrProof?,): SchnorrProof? {

    if (proof == null) return null

    val publicKey = this.importElGamalPublicKey(proof.publicKey)
    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    if (publicKey == null || challenge == null || response == null) {
        logger.error { "one or more SchnorrProof inputs was malformed or out of bounds" }
        return null
    }

    return SchnorrProof(publicKey, challenge, response)
}

fun GroupContext.importElGamalPublicKey(
    publicKey: electionguard.protogen.ElementModP?,
) : ElGamalPublicKey? {
    val key = this.importElementModP(publicKey)
    if (key == null) {
        logger.error { "ElGamalPublicKey was malformed or out of bounds" }
        return null
    }
    return ElGamalPublicKey(key)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun ElementModQ.publishElementModQ(): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))
}

fun UInt256.publishUInt256(): electionguard.protogen.UInt256 {
    val ba = this.bytes
    val bba = ByteArr(ba)
    val uq = electionguard.protogen.UInt256(bba)
    return uq
    // return electionguard.protogen.UInt256(ByteArr(this.bytes))
}

fun ElementModP.publishElementModP(): electionguard.protogen.ElementModP {
    return electionguard.protogen.ElementModP(ByteArr(this.byteArray()))
}

fun ElGamalCiphertext.publishCiphertext(): electionguard.protogen.ElGamalCiphertext {
    return electionguard.protogen
        .ElGamalCiphertext(this.pad.publishElementModP(), this.data.publishElementModP())
}

fun GenericChaumPedersenProof.publishChaumPedersenProof():
    electionguard.protogen.ChaumPedersenProof {
        return electionguard.protogen
            .ChaumPedersenProof(
                this.c.context.G_MOD_P.publishElementModP(), // TODO: REMOVE!
                this.c.context.G_MOD_P.publishElementModP(), // TODO: REMOVE!
                this.c.publishElementModQ(),
                this.r.publishElementModQ()
            )
    }

fun SchnorrProof.publishSchnorrProof(): electionguard.protogen.SchnorrProof {
    return electionguard.protogen
        .SchnorrProof(
            this.publicKey.publishElGamalPublicKey(),
            this.publicKey.context.G_MOD_P.publishElementModP(), // TODO: REMOVE!
            this.challenge.publishElementModQ(),
            this.response.publishElementModQ()
        )
}

fun ElGamalPublicKey.publishElGamalPublicKey() : electionguard.protogen.ElementModP {
    return this.key.publishElementModP()
}