package electionguard.keyceremony

import com.github.michaelbull.result.Result
import electionguard.core.ElementModP
import electionguard.core.SchnorrProof

interface KeyCeremonyTrusteeIF {
    fun id(): String
    fun xCoordinate(): Int
    fun electionPublicKey(): ElementModP
    fun coefficientCommitments(): List<ElementModP>
    fun coefficientProofs(): List<SchnorrProof>

    /** Send my PublicKeys. */
    fun publicKeys(): Result<PublicKeys, String>
    /** Receive the PublicKeys from another guardian. */
    fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String>
    /** Create another guardians share of my key. */
    fun secretKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String>
    /** Receive and verify a secret key share. */
    fun receiveSecretKeyShare(share: EncryptedKeyShare): Result<Boolean, String>

    /** Create another guardians share of my key, not encrypted. */
    fun keyShareFor(otherGuardian: String): Result<KeyShare, String>
    /** Receive and verify a key share. */
    fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String>
}