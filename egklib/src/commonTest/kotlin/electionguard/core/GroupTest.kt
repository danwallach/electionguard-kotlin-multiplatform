package electionguard.core

import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.*

class GroupTest {
    @Test
    fun basics3072() = basics { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun basics4096() = basics { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun basicsSm() = basics { tinyGroup() }

    fun basics(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            val three = 3.toElementModQ(context)
            val four = 4.toElementModQ(context)
            val seven = 7.toElementModQ(context)
            assertEquals(seven, three + four)
        }
    }

    @Test
    fun comparisonOperations3072() =
        comparisonOperations { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun comparisonOperations4096() =
        comparisonOperations { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun comparisonOperationsSm() = comparisonOperations { tinyGroup() }

    fun comparisonOperations(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            val three = 3.toElementModQ(context)
            val four = 4.toElementModQ(context)

            assertTrue(three < four)
            assertTrue(three <= four)
            assertTrue(four > three)
            assertTrue(four >= four)
        }
    }

    @Test
    fun generatorsWork3072() = generatorsWork { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun generatorsWork4096() = generatorsWork { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun generatorsWorkSm() = generatorsWork { tinyGroup() }

    fun generatorsWork(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModP(context)) { it.inBounds() }
            forAll(propTestFastConfig, elementsModQ(context)) { it.inBounds() }
        }
    }

    @Test
    fun validResiduesForGPowP4096() =
        validResiduesForGPowP { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun validResiduesForGPowP3072() =
        validResiduesForGPowP { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun validResiduesForGPowPSm() = validResiduesForGPowP { tinyGroup() }

    fun validResiduesForGPowP(contextF: () -> GroupContext): TestResult {
        return runTest {
            forAll(propTestFastConfig, validResiduesOfP(contextF())) { it.isValidResidue() }
        }
    }

    @Test
    fun binaryArrayRoundTrip(): TestResult {
        return runTest {
            val context = productionGroup()
            forAll(propTestFastConfig, elementsModP(context)) {
                it == context.binaryToElementModP(it.byteArray())
            }
            forAll(propTestFastConfig, elementsModQ(context)) {
                it == context.binaryToElementModQ(it.byteArray())
            }
        }
    }

    @Test
    fun base64RoundTrip(): TestResult {
        return runTest {
            val context = productionGroup()
            forAll(propTestFastConfig, elementsModP(context)) {
                it == context.base64ToElementModP(it.base64())
            }
            forAll(propTestFastConfig, elementsModQ(context)) {
                it == context.base64ToElementModQ(it.base64())
            }
        }
    }

    /**
     * Converts a base-64 string to an [ElementModQ]. Returns null if the number is out of bounds or the
     * string is malformed.
     */
    fun GroupContext.base64ToElementModQ(s: String): ElementModQ? =
        s.fromBase64()?.let { binaryToElementModQ(it) }

    /**
     * Converts a base-64 string to an [ElementModP]. Returns null if the number is out of bounds or the
     * string is malformed.
     */
    fun GroupContext.base64ToElementModP(s: String): ElementModP? =
        s.fromBase64()?.let { binaryToElementModP(it) }

    /** Converts from any [Element] to a base64 string representation. */
    fun Element.base64(): String = byteArray().toBase64()

    @Test
    fun baseConversionFails(): TestResult {
        return runTest {
            val context = productionGroup()
            listOf("", "@@", "-10", "1234567890".repeat(1000))
                .forEach {
                    assertNull(context.base64ToElementModP(it))
                    assertNull(context.base64ToElementModQ(it))
                }
        }
    }

    @Test
    fun additionBasics4096() = additionBasics { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun additionBasics3072() = additionBasics { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun additionBasicsSm() = additionBasics { tinyGroup() }

    fun additionBasics(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { a, b, c ->
                assertEquals(a, a + context.ZERO_MOD_Q) // identity
                assertEquals(a + b, b + a) // commutative
                assertEquals(a + (b + c), (a + b) + c) // associative
            }
        }
    }

    @Test
    fun additionWrappingQ4096() =
        additionWrappingQ { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun additionWrappingQ3072() =
        additionWrappingQ { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun additionWrappingQSm() = additionWrappingQ { tinyGroup() }

    fun additionWrappingQ(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(propTestFastConfig, Arb.int(min=0, max= intTestQ - 1)) { i ->
                val iq = i.toElementModQ(context)
                val q = context.ZERO_MOD_Q - iq
                assertTrue(q.inBounds())
                assertEquals(context.ZERO_MOD_Q, q + iq)
            }
        }
    }

    @Test
    fun multiplicationBasicsP4096() =
        multiplicationBasicsP { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun multiplicationBasicsP3072() =
        multiplicationBasicsP { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun multiplicationBasicsPSm() = multiplicationBasicsP { tinyGroup() }

    fun multiplicationBasicsP(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModPNoZero(context),
                elementsModPNoZero(context),
                elementsModPNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_P) // identity
                assertEquals(a * b, b * a) // commutative
                assertEquals(a * (b * c), (a * b) * c) // associative
            }
        }
    }

    @Test
    fun multiplicationBasicsQ4096() =
        multiplicationBasicsQ { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun multiplicationBasicsQ3072() =
        multiplicationBasicsQ { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun multiplicationBasicsQsm() = multiplicationBasicsQ { tinyGroup() }

    fun multiplicationBasicsQ(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_Q)
                assertEquals(a * b, b * a)
                assertEquals(a * (b * c), (a * b) * c)
            }
        }
    }

    @Test
    fun subtractionBasics4096() =
        subtractionBasics { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun subtractionBasics3072() =
        subtractionBasics { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun subtractionBasicsSm() = subtractionBasics { tinyGroup() }

    fun subtractionBasics(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a - context.ZERO_MOD_Q, "identity")
                assertEquals(a - b, -(b - a), "commutativity-ish")
                assertEquals(a - (b - c), (a - b) + c, "associativity-ish")
            }
        }
    }

    @Test
    fun negation4096() = negation { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun negation3072() = negation { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun negationSm() = negation { tinyGroup() }

    fun negation(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it }
        }
    }

    @Test
    fun multiplicativeInversesP4096() =
        multiplicativeInversesP { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun multiplicativeInversesP3072() =
        multiplicativeInversesP { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun multiplicativeInversesPSm() = multiplicativeInversesP { tinyGroup() }

    fun multiplicativeInversesP(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            // our inverse code only works for elements in the subgroup, which makes it faster
            forAll(propTestFastConfig, validResiduesOfP(context)) {
                it.multInv() * it == context.ONE_MOD_P
            }
        }
    }

    @Test
    fun multiplicativeInversesQ4096() =
        multiplicativeInversesQ { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun multiplicativeInversesQ3072() =
        multiplicativeInversesQ { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun multiplicativeInversesQSm() = multiplicativeInversesQ { tinyGroup() }

    fun multiplicativeInversesQ(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(propTestFastConfig, elementsModQNoZero(context)) {
                assertEquals(context.ONE_MOD_Q, it.multInv() * it)
            }
        }
    }

    @Test
    fun divisionP4096() = divisionP { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun divisionP3072() = divisionP { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun divisionPSm() = divisionP { tinyGroup() }

    fun divisionP(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, validResiduesOfP(context), validResiduesOfP(context))
                { a, b -> (a * b) / b == a }
        }
    }

    @Test
    fun exponentiation4096() = exponentiation { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun exponentiation3072() = exponentiation { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun exponentiationSm() = exponentiation { tinyGroup() }

    fun exponentiation(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                context.gPowP(a) * context.gPowP(b) == context.gPowP(a + b)
            }
        }
    }

    @Test
    fun acceleratedExponentiation4096() =
        acceleratedExponentiation { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun acceleratedExponentiation3072() =
        acceleratedExponentiation { productionGroup(mode = ProductionMode.Mode3072) }

    fun acceleratedExponentiation(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                val ga = context.gPowP(a)
                val normal = ga powP b
                val gaAccelerated = ga.acceleratePow()
                val faster = gaAccelerated powP b
                normal == faster
            }
        }
    }

    @Test
    fun subgroupInverses4096() =
        subgroupInverses { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun subgroupInverses3072() =
        subgroupInverses { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun subgroupInversesSm() = subgroupInverses { tinyGroup() }

    fun subgroupInverses(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context)) {
                val p1 = context.gPowP(it)
                val p2 = p1 powP (context.ZERO_MOD_Q - context.ONE_MOD_Q)
                p1 * p2 == context.ONE_MOD_P
            }
        }
    }

    @Test
    fun iterableAddition4096() =
        iterableAddition { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun iterableAddition3072() =
        iterableAddition { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun iterableAdditionSm() = iterableAddition { tinyGroup() }

    fun iterableAddition(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { a, b, c ->
                val expected = a + b + c
                assertEquals(expected, context.addQ(a, b, c))
                assertEquals(expected, with(context) { listOf(a, b, c).addQ() })
            }
        }
    }

    @Test
    fun iterableMultiplication4096() =
        iterableMultiplication { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun iterableMultiplication3072() =
        iterableMultiplication { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun iterableMultiplicationSm() = iterableMultiplication { tinyGroup() }

    fun iterableMultiplication(contextF: () -> GroupContext): TestResult {
        return runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                validResiduesOfP(context),
                validResiduesOfP(context),
                validResiduesOfP(context)
            ) { a, b, c ->
                val expected = a * b * c
                assertEquals(expected, context.multP(a, b, c))
                assertEquals(expected, with(context) { listOf(a, b, c).multP() })
            }
        }
    }

    @Test
    fun groupCompatibility(): TestResult {
        return runTest {
            val ctxP =
                productionGroup(
                    acceleration = PowRadixOption.NO_ACCELERATION,
                    mode = ProductionMode.Mode4096
                )
            val ctxP2 =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode4096
                )
            val ctx3 =
                productionGroup(
                    acceleration = PowRadixOption.NO_ACCELERATION,
                    mode = ProductionMode.Mode3072
                )
            val ctxT = tinyGroup()

            assertTrue(ctxP.isCompatible(ctxP.constants))
            assertTrue(ctxP.isCompatible(ctxP2.constants))
            assertFalse(ctx3.isCompatible(ctxP))
            assertFalse(ctxT.isCompatible(ctxP.constants))
        }
    }

}

//Op counts have been removed to make the group implementation pure kotlin multiplatform
/*
fun GroupContext.showOpCountResults(where: String): String {
    val opCounts = this.getAndClearOpCounts()
    return buildString {
        appendLine("$where:")
        opCounts.toSortedMap().forEach{ (key, value) ->
            appendLine("   $key : $value")
        }
    }
}

 */