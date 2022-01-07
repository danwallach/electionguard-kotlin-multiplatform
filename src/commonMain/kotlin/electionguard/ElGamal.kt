package electionguard

//typealias ElGamalSecretKey = ElementModQ
typealias ElGamalPublicKey = ElementModP

/** A thin wrapper around an ElementModQ that allows us to hang onto a pre-computed `negativeE` */
class ElGamalSecretKey(val e: ElementModQ) {
    val negativeE: ElementModQ = -e

    override fun equals(other: Any?) =
        when {
            other is ElementModQ -> e == other
            other is ElGamalSecretKey -> e == other.e
            else -> false
        }

    override fun hashCode(): Int = e.hashCode()

    override fun toString(): String = e.toString()
}

/** A public and private keypair, suitable for doing ElGamal cryptographic operations. */
data class ElGamalKeypair(val secretKey: ElGamalSecretKey, val publicKey: ElGamalPublicKey)

val ElGamalKeypair.context: GroupContext
    get() = this.publicKey.context

/**
 * An "exponential ElGamal ciphertext" (i.e., with the plaintext in the exponent to allow for
 * homomorphic addition). (See
 * [ElGamal 1982](https://ieeexplore.ieee.org/abstract/document/1057074))
 */
data class ElGamalCiphertext(val pad: ElementModP, val data: ElementModP)

/**
 * Given an ElGamal secret key, derives the corresponding secret/public key pair.
 *
 * @throws ArithmeticException if the secret key is less than two
 */
fun elGamalKeyPairFromSecret(secret: ElementModQ) =
    if (secret < secret.context.TWO_MOD_Q)
        throw ArithmeticException("secret key must be in [2, Q)")
    else
        ElGamalKeypair(ElGamalSecretKey(secret), secret.context.gPowP(secret).acceleratePow())

/** Generates a random ElGamal keypair. */
fun elGamalKeyPairFromRandom(context: GroupContext) =
    elGamalKeyPairFromSecret(context.randomElementModQ(minimum = 2))

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 *
 * @throws ArithmeticException if the nonce is zero or if the message is negative
 */
fun Int.encrypt(
    publicKey: ElGamalPublicKey,
    nonce: ElementModQ = publicKey.context.randomElementModQ(minimum = 1)
): ElGamalCiphertext {
    val context = compatibleContextOrFail(publicKey, nonce)

    if (nonce == context.ZERO_MOD_Q) {
        throw ArithmeticException("Can't use a zero nonce for ElGamal encryption")
    }

    if (this < 0) {
        throw ArithmeticException("Can't encrypt a negative message")
    }

    // We don't have to check if message >= Q, because it's an integer, and Q
    // is much larger than that.

    val pad = context.gPowP(nonce)
    val expM = context.gPowPSmall(this)
    val keyN = publicKey powP nonce
    val data = expM * keyN

    return ElGamalCiphertext(pad, data)
}

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 */
fun Int.encrypt(
    keypair: ElGamalKeypair,
    nonce: ElementModQ = keypair.context.randomElementModQ(minimum = 1)
) = this.encrypt(keypair.publicKey, nonce)

/** Decrypts using the secret key. if the decryption fails, `null` is returned. */
fun ElGamalCiphertext.decrypt(secretKey: ElGamalSecretKey): Int? {
    val context = compatibleContextOrFail(pad, secretKey.e)
    val blind = pad powP secretKey.negativeE
    val gPowM = data * blind
    return context.dLog(gPowM)
}

/** Decrypts using the secret key from the keypair. If the decryption fails, `null` is returned. */
fun ElGamalCiphertext.decrypt(keypair: ElGamalKeypair) = decrypt(keypair.secretKey)

/** Decrypts a message by knowing the nonce. If the decryption fails, `null` is returned. */
fun ElGamalCiphertext.decryptWithNonce(publicKey: ElGamalPublicKey, nonce: ElementModQ): Int? {
    val context = compatibleContextOrFail(this.pad, publicKey, nonce)
    val blind = publicKey powP nonce
    val gPowM = data / blind
    return context.dLog(gPowM)
}

/** Homomorphically "adds" two ElGamal ciphertexts together through piecewise multiplication. */
operator fun ElGamalCiphertext.plus(o: ElGamalCiphertext): ElGamalCiphertext {
    compatibleContextOrFail(this.pad, o.pad)
    return ElGamalCiphertext(pad * o.pad, data * o.data)
}

/**
 * Homomorphically "adds" a sequence of ElGamal ciphertexts through piecewise multiplication.
 *
 * @throws ArithmeticException if the sequence is empty
 */
fun Iterable<ElGamalCiphertext>.encryptedSum(): ElGamalCiphertext =
    // This operation isn't defined on an empty list -- we'd have to have some way of getting
    // an encryption of zero, but we don't have the public key handy -- so we'll just raise
    // an exception on that, and otherwise we're fine.
    asSequence()
        .let {
            it.ifEmpty { throw ArithmeticException("Cannot sum an empty list of ciphertexts") }
                .reduce { a, b -> a + b }
        }