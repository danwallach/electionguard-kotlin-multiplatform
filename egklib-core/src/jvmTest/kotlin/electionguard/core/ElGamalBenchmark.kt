package electionguard.core

import kotlinx.coroutines.runBlocking
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle

// This is a simple benchmark that just measures how fast ElGamal encryption runs.
// Make sure you're running the JVM with enough memory to handle the "extreme" case
// without running out of RAM, e.g., -Xss128m -Xmx4g -Xms256m (the Xmx flag sets the
// maximum heap size, which is what really matters).

fun main() {
    val N = 1000
    println("ElGamal encryption/decryption benchmark, JDK: ${System.getProperty("java.version")}")

    runBlocking {
        ProductionMode.values()
            .forEach { mode ->
                PowRadixOption.values()
                    .forEach { powRadixOption ->
                        println("=======================================================")
                        println("Initializing benchmark for $powRadixOption, $mode")
                        val context = productionGroup(powRadixOption, mode)

                        val keypair = elGamalKeyPairFromRandom(context)
                        val nonces = Array(N) { context.randomElementModQ() }
                        val prng = Random(System.nanoTime()) // not secure, but we don't care
                        val messages = Array(N) { prng.nextInt(1000) }

                        // force the PowRadix tables to be realized before we start the clock
                        messages[0].encrypt(keypair, nonces[0]).decrypt(keypair)

                        println("Running!")

                        var ciphertexts: List<ElGamalCiphertext>

                        val encryptionTimeMs = measureTimeMillis {
                            ciphertexts = ProgressBar
                                .wrap(
                                    (0..N - 1).asIterable().toList(),
                                    ProgressBarBuilder()
                                        .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                                        .setInitialMax(N.toLong())
                                        .setUpdateIntervalMillis(50)
                                        .setSpeedUnit(ChronoUnit.SECONDS)
                                        .setUnit(" enc", 1L)
                                        .setMaxRenderedLength(100)
                                        .showSpeed()
                                )
                                .map { messages[it].encrypt(keypair, nonces[it]) }
                        }
                        val encryptionTime = encryptionTimeMs / 1000.0

                        val ciphertextArray = ciphertexts.toTypedArray()

                        var decryptions: List<Int?>
                        val decryptionTimeMs = measureTimeMillis {
                            decryptions = ProgressBar
                                .wrap(
                                    (0..N - 1).asIterable().toList(),
                                    ProgressBarBuilder()
                                        .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                                        .setInitialMax(N.toLong())
                                        .setUpdateIntervalMillis(50)
                                        .setSpeedUnit(ChronoUnit.SECONDS)
                                        .setUnit(" dec", 1L)
                                        .setMaxRenderedLength(100)
                                        .showSpeed()
                                )
                                .map { ciphertextArray[it].decrypt(keypair) }
                        }
                        val decryptionTime = decryptionTimeMs / 1000.0

                        println(
                            "ElGamal %.2f encryptions/sec, %.2f decryptions/sec"
                                .format(N / encryptionTime, N / decryptionTime)
                        )

                        if (decryptions.contains(null)) {
                            println("------- Unexpected decryption failure! -------")
                            exitProcess(1)
                        }

                        val decryptionsNoNull = decryptions.filterNotNull()

                        if (decryptionsNoNull != messages.toList()) {
                            println("------- Unexpected decryption not inverse of encryption! -------")
                            exitProcess(1)
                        }
                    }
            }
    }
}
