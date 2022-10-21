package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

private const val debugBallots = false

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEncryptedBallots(
    val group: GroupContext,
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ,
    private val nthreads: Int,
) {
    val aggregator = SelectionAggregator() // for box 7

    fun verify(ballots: Iterable<EncryptedBallot>, showTime: Boolean = false): StatsAccum {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots)
            repeat(nthreads) {
                verifierJobs.add(
                    launchVerifier(
                        it,
                        ballotProducer,
                        aggregator
                    ) { ballot -> verifyEncryptedBallot(ballot) })
            }

            // wait for all verifications to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        // check duplicate confirmation codes (6.B)
        // LOOK what about checking for duplicate ballot ids?
        val results = mutableListOf<Result<Boolean, String>>()
        val checkDuplicates = mutableMapOf<UInt256, String>()
        confirmationCodes.forEach {
            if (checkDuplicates[it.code] != null) {
                results.add(Err("    6.B. Duplicate tracking code for ballot ${it.ballotId} and ${checkDuplicates[it.code]}"))
            }
            checkDuplicates[it.code] = it.ballotId
        }
        accumStats.add(results.merge())

        if (showTime) {
            val took = getSystemTimeInMillis() - starting
            val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
            println("   VerifyEncryptedBallots with $nthreads threads took $took millisecs for $count ballots = $perBallot msecs/ballot")
        }
        return accumStats
    }

    fun verifyEncryptedBallot(ballot: EncryptedBallot): Stats {
        val results = mutableListOf<Result<Boolean, String>>()

        results.add(verifyTrackingCode(ballot))

        var ncontests = 0
        var nselections = 0
        for (contest in ballot.contests) {
            val where = "${ballot.ballotId}/${contest.contestId}"

            ncontests++
            nselections += contest.selections.size

            // Box 4
            contest.selections.forEach {
                results.add(verifySelection(where, it))
            }

            // Box 5
            // calculate ciphertextAccumulation (A, B), note 5.A unneeded because we dont keep (A,B) in election record
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()

            // test that the proof is correct; covers 5.B, 5.C
            val proof: RangeChaumPedersenProofKnownNonce = contest.proof
            val cvalid = proof.validate(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
                manifest.contestIdToLimit[contest.contestId]!!
            )
            if (cvalid is Err) {
                results.add(Err("    5. ChaumPedersenProof failed for $where = ${cvalid.error} "))
            }
        }
        if (debugBallots) println(" Ballot '${ballot.ballotId}' ncontests = $ncontests nselections = $nselections")
        return Stats(ballot.ballotId, results.merge(), ncontests, nselections)
    }

    // Box 4
    private fun verifySelection(where: String, selection: EncryptedBallot.Selection): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val here = "${where}/${selection.selectionId}"

        // test that the proof is correct covers 4.A, 4.B, 4.C
        val svalid = selection.proof.validate(
            selection.ciphertext,
            this.jointPublicKey,
            this.cryptoExtendedBaseHash,
            1,
        )
        if (svalid is Err) {
            errors.add(Err("    4. DisjunctiveChaumPedersenProofKnownNonce failed for ${here}} = ${svalid.error} "))
        }
        return errors.merge()
    }

    // box 6.A
    private fun verifyTrackingCode(ballot: EncryptedBallot): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        val cryptoHashCalculated = hashElements(ballot.ballotId, manifest.cryptoHashUInt256(), ballot.contests) // B_i
        if (cryptoHashCalculated != ballot.cryptoHash) {
            errors.add(Err("    6. Test ballot.cryptoHash failed for ${ballot.ballotId} "))
        }

        val trackingCodeCalculated = hashElements(ballot.codeSeed, ballot.timestamp, ballot.cryptoHash)
        if (trackingCodeCalculated != ballot.code) {
            errors.add(Err("    6.A Test ballot.trackingCode failed for ${ballot.ballotId} "))
        }
        return errors.merge()
    }


    //////////////////////////////////////////////////////////////
    // coroutines

    private val accumStats = StatsAccum()
    private var count = 0
    private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> =
        produce {
            for (ballot in producer) {
                send(ballot)
                yield()
                count++
            }
            channel.close()
        }

    private val confirmationCodes = mutableListOf<ConfirmationCode>()
    private val mutex = Mutex()

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<EncryptedBallot>,
        agg: SelectionAggregator,
        verify: (EncryptedBallot) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val stat = verify(ballot)
            mutex.withLock {
                agg.add(ballot) // this slows down the ballot parallelism: nselections * (2 (modP multiplication))
                confirmationCodes.add(ConfirmationCode(ballot.ballotId, ballot.code))
                accumStats.add(stat)
            }
            yield()
        }
    }
}

// check confirmation codes
private data class ConfirmationCode(val ballotId: String, val code: UInt256)