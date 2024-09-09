package electionguard.testvectors

import com.github.michaelbull.result.Err
import electionguard.core.ElGamalPublicKey
import electionguard.core.UInt256
import electionguard.core.productionGroup
import electionguard.decryptBallot.DecryptWithNonce
import electionguard.demonstrate.ManifestBuilder
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.demonstrate.RandomBallotProvider
import electionguard.json.*
import electionguard.model.EncryptedBallot
import electionguard.model.Manifest
import electionguard.model.PlaintextBallot
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class DecryptWithNonceTestVector {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    private var outputFile = "testOut/testvectors/DecryptWithNonceTestVector.json"

    val group = productionGroup()
    val nBallots = 1

    @Serializable
    data class DecryptWithNonceTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val primary_nonce: UInt256Json,
        val encrypted_ballot: EncryptedBallotJson,
        val task: String,
        val expected_decrypted_ballot : PlaintextBallotJson,
    )

    @Test
    fun testDecryptWithNonceTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("DecryptWithNonceTestVector.json").toString()
        makeDecryptWithNonceTestVector()
        readDecryptWithNonceTestVector()
    }

    fun makeDecryptWithNonceTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash, "device")
        val ciphertextBallot = RandomBallotProvider(manifest, nBallots).ballots().map { ballot ->
            encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("makeDecryptWithNonceTestVector"))!!
        }.first()
        val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

        val decryptionWithPrimaryNonce = DecryptWithNonce(group, ElGamalPublicKey(publicKey), extendedBaseHash)
        val decryptResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(ciphertextBallot.ballotNonce) }
        if (decryptResult is Err) {
            throw RuntimeException("encrypted ballot fails decryption = $decryptResult" )
        }
        val decryptedBallot: PlaintextBallot = decryptResult.component1()!!

        val decryptWithNonceTestVector = DecryptWithNonceTestVector(
            "Test decrypt ballot with nonce",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            ciphertextBallot.ballotNonce.publishJson(),
            encryptedBallot.publishJson(),
            "Decrypt ballot with given primary nonce",
            decryptedBallot.publishJson()
        )
        println(jsonReader.encodeToString(decryptWithNonceTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonReader.encodeToStream(decryptWithNonceTestVector, out)
            out.close()
        }
    }

    fun readDecryptWithNonceTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: DecryptWithNonceTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                jsonReader.decodeFromStream<DecryptWithNonceTestVector>(inp)
            }

        val extendedBaseHash = testVector.extended_base_hash.import() ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed extended_base_hash")
        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group) ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed joint_public_key"))
        val encryptedBallot: EncryptedBallot = testVector.encrypted_ballot.import(group, ErrorMessages("readDecryptBallotTestVector"))!!

        val decryptionWithPrimaryNonce = DecryptWithNonce(group, publicKey, extendedBaseHash)
        val decryptResult = with (decryptionWithPrimaryNonce) {
            encryptedBallot.decrypt(testVector.primary_nonce.import() ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed primary_nonce"))
        }
        if (decryptResult is Err) {
            throw RuntimeException("encrypted ballot fails decryption = $decryptResult" )
        }
        val decryptedBallot: PlaintextBallot = decryptResult.component1()!!

        testVector.expected_decrypted_ballot.contests.zip(decryptedBallot.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.vote, actualSelection.vote)
            }
        }
    }

}