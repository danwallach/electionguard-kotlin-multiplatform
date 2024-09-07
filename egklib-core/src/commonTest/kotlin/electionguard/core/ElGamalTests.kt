package electionguard.core

import electionguard.runTest
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

private fun smallInts() = Arb.int(min=0, max=1000)

class ElGamalTests {

    @Test
    fun noSmallKeys(): TestResult {
        return runTest {
            val context = tinyGroup()
            shouldThrow<ArithmeticException> { elGamalKeyPairFromSecret(0.toElementModQ(context)) }
            shouldThrow<ArithmeticException> { elGamalKeyPairFromSecret(1.toElementModQ(context)) }
            shouldNotThrowAny { elGamalKeyPairFromSecret(2.toElementModQ(context)) }
        }
    }

    @Test
    fun encryptionBasicsLg() {
        encryptionBasics { productionGroup(PowRadixOption.LOW_MEMORY_USE) }
    }

    @Test
    fun encryptionBasicsSm() {
        encryptionBasics { tinyGroup() }
    }

    fun encryptionBasics(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                smallInts()
            ) { keypair, nonce, message ->
                message == message.encrypt(keypair, nonce).decrypt(keypair)
            }
        }
    }

    @Test
    fun encryptionBasicsAutomaticNonces(): TestResult {
        return runTest {
            val context = tinyGroup()

            checkAll(elGamalKeypairs(context), smallInts()) { keypair, message ->
                val encryption = message.encrypt(keypair)
                val decryption = encryption.decrypt(keypair)
                assertEquals(message, decryption)
            }
        }
    }

    @Test
    fun encryptExtraNonce(): TestResult {
        return runTest {
            val group = productionGroup()

            var count = 0
            checkAll(
                propTestFastConfig,
                elGamalKeypairs(group), smallInts()) { keypair, message ->
                val org = message.encrypt(keypair)
                val extra : ElementModQ = group.randomElementModQ(minimum = 1)
                val extraEncryption =  ElGamalCiphertext(org.pad * group.gPowP(extra), org.data * (keypair.publicKey powP extra))

                val decryption = extraEncryption.decrypt(keypair)
                println("$count $message ${message == decryption} ")
                assertEquals(message, decryption)
                count++
            }
        }
    }

    @Test
    fun decryptWithNonce(): TestResult {
        return runTest {
            val context = tinyGroup()

            checkAll(elGamalKeypairs(context), elementsModQNoZero(context), smallInts())
                { keypair, nonce, message ->
                    val encryption = message.encrypt(keypair, nonce)
                    val decryption = encryption.decryptWithNonce(keypair.publicKey, nonce)
                    assertEquals(message, decryption)
                }
        }
    }

    @Test
    fun homomorphicAccumulation(): TestResult {
        return runTest {
            val context = tinyGroup()

            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                smallInts(),
                smallInts(),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { keypair, p1, p2, n1, n2 ->
                val c1 = p1.encrypt(keypair, n1)
                val c2 = p2.encrypt(keypair, n2)
                val csum = c1 + c2
                val d = csum.decrypt(keypair)
                p1 + p2 == d
            }
        }
    }
}
