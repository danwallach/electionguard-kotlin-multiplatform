package electionguard.cli

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.cli.RunTrustedTallyDecryption.Companion.readDecryptingTrustees
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.model.DecryptedTallyOrBallot
import electionguard.model.EncryptedBallot
import electionguard.model.TallyResult
import electionguard.publish.DecryptedTallyOrBallotSinkIF
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.runCli
import electionguard.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlin.jvm.JvmStatic

private val logger = KotlinLogging.logger("RunTrustedBallotDecryption")
private const val debug = false

/**
 * Decrypt spoiled ballots with local trustees CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 * A version of this where each Trustee is in its own process space is implemented in the webapps modules.
 */
class RunTrustedBallotDecryption {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runCli {
            trustedBallotDecryptionCli(args)
        }

        suspend fun trustedBallotDecryptionCli(args: Array<String>) {
            val parser = ArgParser("RunTrustedBallotDecryption")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            ).required()
            val trusteeDir by parser.option(
                ArgType.String,
                shortName = "trustees",
                description = "Directory to read private trustees"
            ).required()
            val outputDir by parser.option(
                ArgType.String,
                shortName = "out",
                description = "Directory to write output election record"
            ).required()
            val decryptChallenged by parser.option(
                ArgType.String,
                shortName = "challenged",
                description = "decrypt challenged ballots"
            )
            val nthreads by parser.option(
                ArgType.Int,
                shortName = "nthreads",
                description = "Number of parallel threads to use"
            )
            parser.parse(args)
            println(
                "RunTrustedBallotDecryption starting\n   input= $inputDir\n   trustees= $trusteeDir\n" +
                        "   decryptChallenged = $decryptChallenged\n   output = $outputDir"
            )

            try {
                val group = productionGroup()
                runDecryptBallots(
                    group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir),
                    decryptChallenged, nthreads ?: 11
                )
            } catch (t : Throwable) {
                logger.error{"Exception= ${t.message} ${t.stackTraceToString()}"}
            }
        }

        suspend fun runDecryptBallots(
            group: GroupContext,
            inputDir: String,
            outputDir: String,
            decryptingTrustees: List<DecryptingTrusteeIF>,
            decryptChallenged: String?, // comma delimited, no spaces
            nthreads: Int,
        ): Int {
            println(" runDecryptBallots on ballots in ${inputDir} with nthreads = $nthreads")
            val starting = getSystemTimeInMillis() // wall clock

            val consumerIn = makeConsumer(group, inputDir)
            val result: Result<TallyResult, ErrorMessages> = consumerIn.readTallyResult()
            if (result is Err) {
                logger.error { result.error.toString() }
                return 0
            }
            val tallyResult = result.unwrap()
            val guardians = Guardians(group, tallyResult.electionInitialized.guardians)

            val decryptor = DecryptorDoerre(
                group,
                tallyResult.electionInitialized.extendedBaseHash,
                tallyResult.electionInitialized.jointElGamalPublicKey(),
                guardians,
                decryptingTrustees,
            )

            // TODO you often want to put the decryption results in the same directory, but sinks now are append-only.
            val publisher = makePublisher(outputDir, false, consumerIn.isJson())
            val sink: DecryptedTallyOrBallotSinkIF = publisher.decryptedTallyOrBallotSink()

            val ballotIter: Iterable<EncryptedBallot> =
                when {
                    (decryptChallenged == null) -> {
                        println(" use all challenged")
                        consumerIn.iterateAllSpoiledBallots()
                    }

                    (decryptChallenged.trim().lowercase() == "all") -> {
                        println(" use all")
                        consumerIn.iterateAllEncryptedBallots { true }
                    }

                    pathExists(decryptChallenged) -> {
                        println(" use ballots in file '$decryptChallenged'")
                        val wanted: List<String> = fileReadLines(decryptChallenged)
                        val wantedTrim: List<String> = wanted.map { it.trim() }
                        consumerIn.iterateAllEncryptedBallots { wantedTrim.contains(it.ballotId) }
                    }

                    else -> {
                        println(" use ballots in list '${decryptChallenged}'")
                        val wanted: List<String> = decryptChallenged.split(",")
                        consumerIn.iterateAllEncryptedBallots {
                            // println(" ballot ${it.ballotId}")
                            wanted.contains(it.ballotId)
                        }
                    }
                }

            try {
                 MainScope().launch {
                    val outputChannel = Channel<DecryptedTallyOrBallot>()
                    val decryptorJobs = mutableListOf<Job>()
                    val ballotProducer = produceBallots(ballotIter)
                    repeat(nthreads) {
                        decryptorJobs.add(
                            launchDecryptor(
                                it,
                                ballotProducer,
                                decryptor,
                                outputChannel
                            )
                        )
                    }
                    launchSink(outputChannel, sink)

                    // wait for all decryptions to be done, then close everything
                    joinAll(*decryptorJobs.toTypedArray())
                    outputChannel.close()
                }.join()
            } finally {
                sink.close()
            }

            decryptor.stats.show(5)
            val count = decryptor.stats.count()

            val took = getSystemTimeInMillis() - starting
            val msecsPerBallot = (took.toDouble() / 1000 / count).sigfig()
            println(" decrypt ballots took ${took / 1000} wallclock secs for $count ballots = $msecsPerBallot secs/ballot")

            return count
        }

        // parallelize over ballots
        // place the ballot reading into its own coroutine
        private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> =
            produce {
                for (ballot in producer) {
                    logger.debug { "Producer sending ballot ${ballot.ballotId}" }
                    send(ballot)
                    yield()
                }
                channel.close()
            }

        private fun CoroutineScope.launchDecryptor(
            id: Int,
            input: ReceiveChannel<EncryptedBallot>,
            decryptor: DecryptorDoerre,
            output: SendChannel<DecryptedTallyOrBallot>,
        ) = launch(Dispatchers.Default) {
            for (ballot in input) {
                val errs = ErrorMessages("RunTrustedBallotDecryption ballot=${ballot.ballotId}")
                try {
                    val decrypted = decryptor.decryptBallot(ballot, errs)
                    if (decrypted != null) {
                        logger.debug { " Decryptor #$id sending DecryptedTallyOrBallot ${decrypted.id}" }
                        if (debug) println(" Decryptor #$id sending DecryptedTallyOrBallot ${decrypted.id}")
                        output.send(decrypted)
                    } else {
                        logger.error { " decryptBallot error on ${ballot.ballotId} error=${errs}" }
                    }
                } catch (t : Throwable) {
                    errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
                    logger.error { errs }
                }
                yield()
            }
            logger.debug { "Decryptor #$id done" }
        }

        // place the output writing into its own coroutine
        private fun CoroutineScope.launchSink(
            input: Channel<DecryptedTallyOrBallot>, sink: DecryptedTallyOrBallotSinkIF,
        ) = launch {
            for (tally in input) {
                sink.writeDecryptedTallyOrBallot(tally)
            }
        }
    }
}
