package electionguard.testvectors

import electionguard.byteArrays
import electionguard.cli.ManifestBuilder
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalPublicKey
import electionguard.core.UInt256
import electionguard.core.base16ToElementModP
import electionguard.core.productionGroup
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor
import electionguard.input.BallotInputValidation
import electionguard.input.RandomBallotProvider
import electionguard.json.*
import electionguard.model.Manifest
import electionguard.model.PlaintextBallot
import electionguard.util.ErrorMessages
import io.kotest.property.arbitrary.single
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

class ConfirmationCodeTestVector {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    private var outputFile = "testOut/testvectors/ConfirmationCodeTestVector.json"

    val group = productionGroup()
    val nBallots = 1

    @Serializable
    data class EncryptedBallotJson(
        val ballotId: String,
        val ballotNonce: UInt256Json,
        val codeBaux: String,
        val task: String,
        val expected_confirmationCode: UInt256Json,
        val contests: List<EncryptedContestJson>,
     )

    @Serializable
    data class EncryptedContestJson(
        val contestId: String,
        val task: String,
        val expected_contestHash: UInt256Json,
    )

    fun CiphertextBallot.publishJson(): EncryptedBallotJson {
        return EncryptedBallotJson(
            this.ballotId,
            this.ballotNonce.publishJson(),
            this.codeBaux.toHex(),
            "Compute confirmation code (eq 59)",
            this.confirmationCode.publishJson(),
            this.contests.map { EncryptedContestJson(
                it.contestId,
                "Compute contest hash (eq 58)",
                it.contestHash.publishJson()) }
        )
    }

    @Serializable
    data class ConfirmationCodeTestVector(
        val desc: String,
        val joint_public_key: String,
        val extended_base_hash: String,
        val ballots: List<PlaintextBallotJsonV>,
        val expected_encrypted_ballots: List<EncryptedBallotJson>,
    )

    @Test
    fun testConfirmationCodeTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("ConfirmationCodeTestVector.json").toString()
        makeConfirmationCodeTestVector()
        readConfirmationCodeTestVector()
    }

    fun makeConfirmationCodeTestVector() {
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
        val validator = BallotInputValidation(manifest)

        val useBallots = mutableListOf<PlaintextBallot>()
        val eballots = mutableListOf<CiphertextBallot>()
        RandomBallotProvider(manifest, nBallots).ballots().forEach { ballot ->
            val msgs = validator.validate(ballot)
            println(msgs)
            if ( !msgs.hasErrors() ) {
                val codeBaux = byteArrays(11).single()
                val eballot = encryptor.encrypt(ballot, codeBaux, ErrorMessages("makeConfirmationCodeTestVector"))!!
                eballots.add(eballot)
                useBallots.add(ballot)
            }
        }

        val confirmationCodeTestVector = ConfirmationCodeTestVector(
            "Test ballot confirmation code",
            publicKey.toHex(),
            extendedBaseHash.toHex(),
            useBallots.map { it.publishJsonE() },
            eballots.map { it.publishJson() },
        )
        println(jsonReader.encodeToString(confirmationCodeTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonReader.encodeToStream(confirmationCodeTestVector, out)
            out.close()
        }
    }

    fun readConfirmationCodeTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: ConfirmationCodeTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                jsonReader.decodeFromStream<ConfirmationCodeTestVector>(inp)
            }

        val publicKey = ElGamalPublicKey(group.base16ToElementModP(testVector.joint_public_key)!!)
        val extendedBaseHash = UInt256(testVector.extended_base_hash.fromHex()!!)
        val ballotsZipped = testVector.ballots.zip(testVector.expected_encrypted_ballots)

        ballotsZipped.forEach { (ballot, eballot) ->
            val manifest = PlaintextBallotJsonManifestFacade(ballot)
            val encryptor = Encryptor(group, manifest, publicKey, extendedBaseHash, "device")
            val ballotNonce = eballot.ballotNonce.import()
            val codeBaux = eballot.codeBaux.fromHex()!!
            val cyberBallot = encryptor.encrypt(ballot.import(), codeBaux, ErrorMessages("readConfirmationCodeTestVector"), ballotNonce)!!
            checkEquals(eballot, cyberBallot)
        }
    }

    fun checkEquals(expect : EncryptedBallotJson, actual : CiphertextBallot) {
        assertEquals(expect.ballotId, actual.ballotId)
        assertEquals(expect.expected_confirmationCode.import(), actual.confirmationCode)

        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.expected_contestHash.import(), actualContest.contestHash)
        }
        println("ballot ${actual.ballotId} is ok")
    }
}