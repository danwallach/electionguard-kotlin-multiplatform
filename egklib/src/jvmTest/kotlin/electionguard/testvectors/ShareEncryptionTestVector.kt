package electionguard.testvectors

import electionguard.core.*
import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import electionguard.keyceremony.*
import electionguard.keyceremony.PrivateKeyShare
import electionguard.json2.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareEncryptionTestVector {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    private var outputFile = "testOut/testvectors/ShareEncryptionTestVector.json"

    val numberOfGuardians = 3
    val quorum = 3
    val group = productionGroup()

    // make some things public for resuse
    var xtrustees: List<KeyCeremonyTrustee> = emptyList()
    var publicKey: ElementModP? = null
    val electionBaseHash = UInt256.random()
    var He: UInt256? = null

    @Serializable
    data class GuardianJson(
        val name: String,
        val coordinate: Int,
        val polynomial_coefficients: List<ElementModQJson>,
    )

    @Serializable
    data class GuardianSharesJson(
        val name: String,
        val share_nonces: Map<String, ElementModQJson>,
        val task1: String,
        val expected_shares: List<KeyShareJson>,
        val task2: String,
        val expected_my_share_of_secret: ElementModQJson,
    )

    internal fun GuardianSharesJson.import(group : GroupContext) =
            GuardianShares(
                name,
                share_nonces.mapValues { it.value.import(group) ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed share_nonces") },
                expected_shares.map { it.import(group) },
                expected_my_share_of_secret.import(group) ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed expected_my_share_of_secret"),
            )

    internal data class GuardianShares(
        val name: String,
        val shareNonces: Map<String, ElementModQ>,
        val expected_shares: List<PrivateKeyShare>,
        val expected_my_share: ElementModQ,
    )

    @Serializable
    data class KeyShareJson(
        val ownerXcoord: Int, // guardian i xCoordinate
        val polynomialOwner: String, // guardian i name
        val secretShareFor: String, // guardian l name
        val yCoordinate: ElementModQJson, // ElementModQ, // my polynomial's y value at the other's x coordinate = Pi(ℓ)
        val encryptedCoordinate: HashedElGamalCiphertextJson, // El(Pi_(ℓ))
    )

    internal fun PrivateKeyShare.publishJson() =
        KeyShareJson(
            this.ownerXcoord,
            this.polynomialOwner,
            this.secretShareFor,
            this.yCoordinate.publishJson(),
            this.encryptedCoordinate.publishJson(),
    )

    internal fun KeyShareJson.import(group : GroupContext) =
        PrivateKeyShare(
            ownerXcoord,
            polynomialOwner,
            secretShareFor,
            encryptedCoordinate.import(group),
            yCoordinate.import(group) ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed primary_nonce"),
        )

    @Serializable
    data class HashedElGamalCiphertextJson(
        val c0: ElementModPJson, // ElementModP,
        val c1: String, // ByteArray,
        val c2: UInt256Json, // UInt256,
        val numBytes: Int
    )

    fun HashedElGamalCiphertext.publishJson() =
            HashedElGamalCiphertextJson(this.c0.publishJson(), this.c1.toHex(), this.c2.publishJson(), this.numBytes)

    fun HashedElGamalCiphertextJson.import(group : GroupContext) =
        HashedElGamalCiphertext(
            c0.import(group) ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed primary_nonce"),
            c1.fromHexSafe(),
            c2.import() ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed primary_nonce"),
            numBytes,
        )

    @Serializable
    data class ShareEncryptionTestVector(
        val desc : String,
        val guardians : List<GuardianJson>,
        val expected_guardian_shares : List<GuardianSharesJson>,
    )

    @Test
    fun testShareEncryptionTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("ShareEncryptionTestVector.json").toString()
        makeShareEncryptionTestVector()
        readShareEncryptionTestVector()
    }

    fun makeShareEncryptionTestVector(publish : Boolean = true) {

        val trustees: List<KeyCeremonyTrusteeSaveNonces> = List(numberOfGuardians) {
            val seq = it + 1
            KeyCeremonyTrusteeSaveNonces(group, "guardian$seq", seq, numberOfGuardians, quorum)
        }.sortedBy { it.xCoordinate() }

        keyCeremonyExchange(trustees)

        val guardians = trustees.map { trustee ->
            GuardianJson(
                trustee.id(),
                trustee.xCoordinate(),
                trustee.polynomial.coefficients.map { it.publishJson() },
            )
        }

        val guardianShares = trustees.map { trustee ->
            GuardianSharesJson(
                trustee.id(),
                trustee.shareNonces.mapValues { it.value.publishJson()},
                "Generate this guardian's shares for other guardians (Pi(ℓ) = yCoordinate, El(Pi(ℓ) = encryptedCoordinate), eq 17",
                trustee.myShareOfOthers.values.map { it.publishJson() },
                "Generate this guardian's share of the secret key, eq 66",
                trustee.computeSecretKeyShare().publishJson(),
            )
        }

        val shareEncryptionTestVector = ShareEncryptionTestVector(
            "Test partial key (aka share) exchange during the KeyCeremony",
            guardians,
            guardianShares,
        )
        println(jsonReader.encodeToString(shareEncryptionTestVector))

        if (publish) {
            FileOutputStream(outputFile).use { out ->
                jsonReader.encodeToStream(shareEncryptionTestVector, out)
                out.close()
            }
        }

        xtrustees = trustees
    }

    fun readShareEncryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val shareEncryptionTestVector: ShareEncryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                jsonReader.decodeFromStream<ShareEncryptionTestVector>(inp)
            }

        val guardians = shareEncryptionTestVector.guardians
        val guardianShares = shareEncryptionTestVector.expected_guardian_shares.map { it.import(group) }
        assertTrue(guardians.size == guardianShares.size)

        val trustees = guardians.zip(guardianShares).map { (guardianJson, share) ->
            val coefficients = guardianJson.polynomial_coefficients.map { it.import(group) ?: throw IllegalArgumentException("ShareEncryptionTestVector malformed polynomial_coefficients") }
            val polynomial = group.regeneratePolynomial(
                guardianJson.name,
                guardianJson.coordinate,
                coefficients,
            )

            KeyCeremonyTrusteeWithNonces(
                group,
                guardianJson.name,
                guardianJson.coordinate,
                numberOfGuardians,
                quorum,
                polynomial,
                share.shareNonces
            )
        }

        keyCeremonyExchange(trustees)

        guardianShares.forEach { guardianShare ->
            guardianShare.expected_shares.forEach { expectedShare ->
                val actualShare = expectedShare.findMatchingShare(trustees)
                assertEquals(expectedShare.yCoordinate, actualShare.yCoordinate)
                assertEquals(expectedShare.encryptedCoordinate, actualShare.encryptedCoordinate)
            }
        }

        trustees.zip(guardianShares).map { (trustee, share) ->
            assertEquals(share.expected_my_share, trustee.computeSecretKeyShare())
        }
    }

    internal fun PrivateKeyShare.findMatchingShare(trustees : List<KeyCeremonyTrustee>) : PrivateKeyShare {
        val trustee = trustees.find { it.id() == this.secretShareFor }!!
        return trustee.myShareOfOthers[this.polynomialOwner]!!
    }
}

private class KeyCeremonyTrusteeSaveNonces(
    group: GroupContext,
    id: String,
    xCoordinate: Int,
    nguardians: Int,
    quorum: Int,
    polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum)
) : KeyCeremonyTrustee(group, id, xCoordinate, nguardians, quorum, polynomial) {
    val shareNonces = mutableMapOf<String, ElementModQ>()

    override fun shareEncryption(
        Pil: ElementModQ,
        other: PublicKeys,
        nonce: ElementModQ // An overriding function is not allowed to specify default values for its parameters
    ) : HashedElGamalCiphertext {
        // val nonce: ElementModQ = group.randomElementModQ(minimum = 2)
        shareNonces[other.guardianId] = nonce
        return super.shareEncryption(Pil, other, nonce)
    }

    fun myShareOfOthers(): Map<String, PrivateKeyShare> = super.myShareOfOthers
}

private class KeyCeremonyTrusteeWithNonces(
    group: GroupContext,
    id: String,
    xCoordinate: Int,
    nguardians: Int,
    quorum: Int,
    polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum),
    val shareNonces: Map<String, ElementModQ>,
) : KeyCeremonyTrustee(group, id, xCoordinate, nguardians, quorum, polynomial) {

    override fun shareEncryption(
        Pil: ElementModQ,
        other: PublicKeys,
        nonce: ElementModQ, // ignored
    ) : HashedElGamalCiphertext {
        val useNonce: ElementModQ = shareNonces[other.guardianId]!!
        return super.shareEncryption(Pil, other, useNonce)
    }
}