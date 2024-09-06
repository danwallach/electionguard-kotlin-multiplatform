package electionguard.testvectors

import electionguard.model.electionExtendedHash
import electionguard.core.*
import electionguard.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyCeremonyTestVector {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    private var outputFile = "testOut/testvectors/KeyCeremonyTestVector.json"

    val numberOfGuardians = 5
    val quorum = 3
    val group = productionGroup()

    // make some things public for resuse
    val guardians = mutableListOf<GuardianJson>()
    var publicKey: ElementModP? = null
    val electionBaseHash = UInt256.random()
    var He: UInt256? = null

    @Serializable
    data class GuardianJson(
        val name: String,
        val coordinate: Int,
        val polynomial_coefficients: List<ElementModQJson>,
        val proof_nonces: List<ElementModQJson>,
        val task: String,
        val expected_proofs: List<SchnorrProofJson>,
    )

    @Serializable
    data class KeyCeremonyTestVector(
        val desc: String,
        val guardians : List<GuardianJson>,
        val task: String,
        val election_base_hash : UInt256Json,
        val expected_joint_public_key : ElementModPJson,
        val expected_extended_base_hash : UInt256Json,
    )

    @Test
    fun testKeyCeremonyTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("KeyCeremonyTestVector.json").toString()
        makeKeyCeremonyTestVector()
        readKeyCeremonyTestVector()
    }

    fun makeKeyCeremonyTestVector(publish : Boolean = true) {
        val publicKeys = mutableListOf<ElementModP>()

        repeat(numberOfGuardians) {
            val guardianXCoord = it + 1

            val coefficients = mutableListOf<ElementModQ>()
            val commitments = mutableListOf<ElementModP>()
            val nonces = mutableListOf<ElementModQ>()
            val proofs = mutableListOf<SchnorrProofJson>()

            for (coeff in 0 until quorum) {
                val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(group)
                coefficients.add(keypair.secretKey.key)
                commitments.add(keypair.publicKey.key)
                val nonce = group.randomElementModQ()
                val proof = keypair.schnorrProof(guardianXCoord, coeff, nonce)
                proofs.add(proof.publishJson())
                nonces.add(nonce)
            }

            publicKeys.add(commitments[0])

            guardians.add( GuardianJson(
                "Guardian$guardianXCoord",
                guardianXCoord,
                coefficients.map { it.publishJson() },
                nonces.map { it.publishJson() },
                "Generate Schnorr proofs for Guardian coefficients (section 3.2.2)",
                proofs,
                )
            )
        }

        val expectedPublicKey = publicKeys.reduce { a, b -> a * b }
        val extendedBaseHash = electionExtendedHash(electionBaseHash, expectedPublicKey)
        val keyCeremonyTestVector = KeyCeremonyTestVector(
            "Test KeyCeremony guardian creation",
            guardians,
            "Generate joint public key (eq 8) and extended base hash (eq 23)",
            electionBaseHash.publishJson(),
            expectedPublicKey.publishJson(),
            extendedBaseHash.publishJson(),
        )
        println(jsonReader.encodeToString(keyCeremonyTestVector))

        if (publish) {
            FileOutputStream(outputFile).use { out ->
                jsonReader.encodeToStream(keyCeremonyTestVector, out)
                out.close()
            }
        }

        // make stuff available outside
        publicKey = expectedPublicKey
        He = extendedBaseHash
    }

    fun readKeyCeremonyTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val keyCeremonyTestVector : KeyCeremonyTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<KeyCeremonyTestVector>(inp)
            }

        val publicKeys = mutableListOf<ElementModP>()
        keyCeremonyTestVector.guardians.forEach { guardianJson ->
            val guardianXCoord = guardianJson.coordinate
            val secretKey = guardianJson.polynomial_coefficients[0].import(group) ?: throw IllegalArgumentException("readKeyCeremonyTestVector malformed secretKey")
            publicKeys.add(group.gPowP(secretKey))

            var coeffIdx = 0
            guardianJson.polynomial_coefficients.forEach {
                val privateKey = it.import(group) ?: throw IllegalArgumentException("readKeyCeremonyTestVector malformed polynomial_coefficient")
                val publicKey = group.gPowP(privateKey)

                val keypair = ElGamalKeypair(ElGamalSecretKey(privateKey), ElGamalPublicKey(publicKey))
                val nonce = guardianJson.proof_nonces[coeffIdx].import(group) ?: throw IllegalArgumentException("readKeyCeremonyTestVector malformed nonce")
                val proof = keypair.schnorrProof(guardianXCoord, coeffIdx, nonce)
                val expectedProof = guardianJson.expected_proofs[coeffIdx]

               //  assertEquals(group.base16ToElementModP(expectedProof.public_key)!!, proof.publicKey)
                assertEquals(expectedProof.challenge.import(group), proof.challenge)
                assertEquals(expectedProof.response.import(group), proof.response)

                coeffIdx++
            }
        }

        val publicKey = publicKeys.reduce { a, b -> a * b }
        assertEquals(keyCeremonyTestVector.expected_joint_public_key.import(group), publicKey)

        // He = H(HB ; 0x12, K) ; spec 2.0.0 p.25, eq 23.
        val electionBaseHash = keyCeremonyTestVector.election_base_hash.import() ?: throw IllegalArgumentException("readKeyCeremonyTestVector malformed electionBaseHash")
        val extendedBaseHash = electionExtendedHash(electionBaseHash, publicKey)

        assertEquals(keyCeremonyTestVector.expected_extended_base_hash.import(), extendedBaseHash)
    }

}