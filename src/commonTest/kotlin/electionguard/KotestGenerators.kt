package electionguard

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.ShrinkingMode
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.map

/**
 * Generate an arbitrary ElementModP in [minimum, P) for the given group context (defaults to the
 * fast 16-bit test context).
 */
fun elementsModP(ctx: GroupContext = testGroup(), minimum: Int = 0): Arb<ElementModP> =
    Arb.byteArray(Arb.constant(512), Arb.byte()).map { ctx.safeBinaryToElementModP(it, minimum) }

/**
 * Generate an arbitrary ElementModQ in [minimum, Q) for the given group context (defaults to the
 * fast 16-bit test context).
 */
fun elementsModQ(ctx: GroupContext = testGroup(), minimum: Int = 0): Arb<ElementModQ> =
    Arb.byteArray(Arb.constant(32), Arb.byte()).map { ctx.safeBinaryToElementModQ(it, minimum) }

/**
 * Generates a valid element of the subgroup of ElementModP where there exists an e in Q such that v
 * = g^e.
 */
fun validElementsModP(ctx: GroupContext = testGroup()): Arb<ElementModP> =
    elementsModQ(ctx).map { e -> ctx.gPowP(e) }

/**
 * Property-based testing can run slowly. This will speed things up by turning off shrinking and
 * using fewer iterations.
 */
val propTestFastConfig =
    PropTestConfig(maxFailure = 1, shrinkingMode = ShrinkingMode.Off, iterations = 10)