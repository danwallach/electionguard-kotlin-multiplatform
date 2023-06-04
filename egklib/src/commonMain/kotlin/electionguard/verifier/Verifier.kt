package electionguard.verifier

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.*
import electionguard.publish.ElectionRecord

// since there's no verification spec 2.0 yet, this is approximate
class Verifier(val record: ElectionRecord, val nthreads: Int = 11) {
    val group: GroupContext
    val manifest: Manifest
    val jointPublicKey: ElGamalPublicKey
    val qbar: ElementModQ

    init {
        group = productionGroup()
        manifest = record.manifest()

        if (record.stage() < ElectionRecord.Stage.INIT) { // fake
            qbar = group.ONE_MOD_Q
            jointPublicKey = ElGamalPublicKey(group.ONE_MOD_P)
        } else {
            jointPublicKey = ElGamalPublicKey(record.jointPublicKey()!!)
            qbar = record.extendedBaseHash()!!.toElementModQ(group)
        }
    }

    fun verify(stats : Stats, showTime : Boolean = false): Boolean {
        println("\n****Verify election record in = ${record.topdir()}\n")
        val starting13 = getSystemTimeInMillis()

        val parametersOk = verifyParameters(record.config())
        println(" 1. verifyParameters= $parametersOk")

        if (record.stage() < ElectionRecord.Stage.INIT) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            val took = getSystemTimeInMillis() - starting13
            if (showTime) println("   verify `took $took millisecs")
            return true
        }

        val guardiansOk = verifyGuardianPublicKey()
        println(" 2. verifyGuardianPublicKeys= $guardiansOk")

        val publicKeyOk = verifyElectionPublicKey(record.electionBaseHash()!!)
        println(" 3. verifyElectionPublicKey= $publicKeyOk")

        if (record.stage() < ElectionRecord.Stage.ENCRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            val took = getSystemTimeInMillis() - starting13
            if (showTime) println("   verify 2,3 took $took millisecs")
            return true
        }

        // encryption and vote limits 4, 5, 6
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        // Note we are validating all ballots, not just CAST
        val ballotResult = verifyBallots.verify(record.encryptedBallots { true }, stats, showTime)
        println(" 4,5,6. verifyEncryptedBallots $ballotResult")

        // 10 contest data for encrypted ballots

        if (record.stage() < ElectionRecord.Stage.TALLIED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally accumulation, box 7 and 9F
        val verifyAggregation = VerifyAggregation(group, verifyBallots.aggregator)
        val aggResult = verifyAggregation.verify(record.encryptedTally()!!, showTime)
        println(" 7. verifyBallotAggregation $aggResult")

        if (record.stage() < ElectionRecord.Stage.DECRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally decryption
        val verifyDecryption = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        val tallyResult = verifyDecryption.verify(record.decryptedTally()!!, isBallot = false, stats)
        println(" 8,9. verifyTallyDecryption $tallyResult")

        // 10, 11, 12, 13, 14 spoiled ballots
        val spoiledResult =
            verifyDecryption.verifySpoiledBallotTallies(record.decryptedBallots(), nthreads, stats, showTime)
        println(" 10,11,12,13,14. verifySpoiledBallotTallies $spoiledResult")

        val allOk = (parametersOk is Ok) && (guardiansOk is Ok) && (publicKeyOk is Ok) &&
                (ballotResult is Ok) && (aggResult is Ok) && (tallyResult is Ok) && (spoiledResult is Ok)
        println("verify allOK = $allOk\n")
        return allOk
    }

    // Verification 1 (Parameter validation)
    private fun verifyParameters(config : ElectionConfig): Result<Boolean, String> {
        val check: MutableList<Result<Boolean, String>> = mutableListOf()
        val constants = config.constants

        if (!constants.largePrime.contentEquals(group.constants.largePrime)) {
            check.add(Err("  1.A The large prime is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.smallPrime.contentEquals(group.constants.smallPrime)) {
            check.add(Err("  1.B The small prime is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.cofactor.contentEquals(group.constants.cofactor)) {
            check.add(Err("  1.C The cofactor is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.generator.contentEquals(group.constants.generator)) {
            check.add(Err("  1.D The small prime is non equal to the large modulus p defined in Section 3.1.1"))
        }

        val Hp = parameterBaseHash(config.constants)
        if (Hp != config.parameterBaseHash) {
            check.add(Err("  1.E The parameter base hash does not match eq 4"))
        }
        val Hm = manifestHash(Hp, config.manifestFile)
        if (Hm != config.manifestHash) {
            check.add(Err("  1.F The manifest hash does not match eq 5"))
        }
        val Hd = electionBaseHash(Hp, config.numberOfGuardians, config.quorum, config.electionDate, config.jurisdictionInfo, Hm)
        if (Hd != config.electionBaseHash) {
            check.add(Err("  1.F The election base hash does not match eq 6"))
        }

        return check.merge()
    }

    // Verification Box 2
    private fun verifyGuardianPublicKey(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for (guardian in this.record.guardians()) {
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val result = proof.validate(guardian.xCoordinate, index)
                if (result is Err) {
                    checkProofs.add(Err("  2. Guardian ${guardian.guardianId} has invalid proof for coefficient $index " +
                        result.unwrapError()
                    ))
                }
            }
        }
        return checkProofs.merge()
    }

    // Verification Box 3
    private fun verifyElectionPublicKey(cryptoBaseHash: UInt256): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        val jointPublicKeyComputed = this.record.guardians().map { it.publicKey() }.reduce { a, b -> a * b }
        if (!jointPublicKey.equals(jointPublicKeyComputed)) {
            errors.add(Err("  3.A jointPublicKey K does not equal computed K = Prod(K_i)"))
        }

        val commitments = mutableListOf<ElementModP>()
        this.record.guardians().forEach { commitments.addAll(it.coefficientCommitments()) }
        // spec 1.53, eq 18 and 3.B
        val computedQbar: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitments)
        if (qbar != computedQbar.toElementModQ(group)) {
            errors.add(Err("  3.B qbar does not match computed = H(Q, K, {K_ij})"))
            println("qbar $qbar != computed $computedQbar")
        }

        return errors.merge()
    }

    fun verifyEncryptedBallots(stats : Stats): Result<Boolean, String> {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        return verifyBallots.verify(record.encryptedBallots { true }, stats)
    }

    fun verifyEncryptedBallots(ballots: Iterable<EncryptedBallot>, stats : Stats): Result<Boolean, String> {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        return verifyBallots.verify(ballots, stats)
    }

    fun verifyDecryptedTally(tally: DecryptedTallyOrBallot, stats: Stats): Result<Boolean, String> {
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        return verifyTally.verify(tally, false, stats)
    }

    fun verifySpoiledBallotTallies(stats: Stats): Result<Boolean, String> {
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        return verifyTally.verifySpoiledBallotTallies(record.decryptedBallots(), nthreads, stats, true)
    }

}
