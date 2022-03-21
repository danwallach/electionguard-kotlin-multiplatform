package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("Schnorr")

/**
 * Representation of a proof that the prover know the private key corresponding to the given public
 * key.
 */
data class SchnorrProof(
    val publicKey: ElGamalPublicKey,
    val challenge: ElementModQ,
    val response: ElementModQ
)

/**
 * Given an ElGamal keypair (public and private key), and a random nonce, this generates a proof
 * that the author of the proof knew the public and corresponding private keys.
 */
fun ElGamalKeypair.schnorrProof(nonce: ElementModQ): SchnorrProof {
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce)
    val c = hashElements(publicKey, h).toElementModQ(context)
    val u = nonce + secretKey.key * c

    return SchnorrProof(publicKey, c, u)
}

/**
 * Check validity of the proof for proving possession of the private key corresponding to the
 * `publicKey` field inside the proof.
 */
fun ElGamalPublicKey.hasValidSchnorrProof(proof: SchnorrProof): Boolean {
    val (k, challenge, u) = proof
    val context = compatibleContextOrFail(this.key, k.key, challenge, u)

    val validPublicKey = k.key.isValidResidue()
    val inBoundsU = u.inBounds()

    val h = context.gPowP(u) / this.powP(challenge)

    val c = hashElements(k, h).toElementModQ(context)
    val validChallenge = c == challenge
    val validProof = context.gPowP(u) == h * (k powP c)
    val samePublicKey = this == proof.publicKey
    val success = validPublicKey && inBoundsU && validChallenge && validProof && samePublicKey

    if (!success) {
        val resultMap =
            mapOf(
                "inBoundsU" to inBoundsU,
                "validPublicKey" to validPublicKey,
                "validChallenge" to validChallenge,
                "validProof" to validProof,
                "samePublicKey" to samePublicKey,
                "proof" to this
            )
        logger.warn { "found an invalid Schnorr proof: $resultMap" }
    }

    return success
}