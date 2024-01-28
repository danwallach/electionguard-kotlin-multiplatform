package electionguard.core

import electionguard.util.sigfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VACalgTest {
    val group = productionGroup()

    @Test
    fun testVACexample() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }

        // works
        FEexp(group, es, true).prodPowP(bases)
        println("///////////////////////////////////////////")

        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACexample2() {
        val e0 = 1231130.toElementModQ(group)
        val e1 = 3462110.toElementModQ(group)
        val e2 = 5673241.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        // works
        val feResult = FEexp(group, es, true).prodPowP(bases)
        println("///////////////////////////////////////////")
        assertEquals(check, feResult)

        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACexample3() {
        val es = listOf(
            1231130.toElementModQ(group),
            3462110.toElementModQ(group),
            5673241.toElementModQ(group),
            2983477.toElementModQ(group),
            6345902.toElementModQ(group),
            329756.toElementModQ(group),
        )

        val bases = List(es.size) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        // works
        //val feResult = FEexp(group, es, true).prodPowP(bases)
        //assertEquals(check, feResult)
        println("///////////////////////////////////////////")

        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACexample4() {
        val es = listOf(
            12313130.toElementModQ(group),
            34623110.toElementModQ(group),
            56733241.toElementModQ(group),
            32983477.toElementModQ(group),
            63435902.toElementModQ(group),
            3297356.toElementModQ(group),
            12331130.toElementModQ(group),
            34362110.toElementModQ(group),
            56733241.toElementModQ(group),
            29834377.toElementModQ(group),
            63345902.toElementModQ(group),
            3239756.toElementModQ(group),
            )

        val bases = List(es.size) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        // works
        //val feResult = FEexp(group, es, true).prodPowP(bases)
        //assertEquals(check, feResult)
        println("///////////////////////////////////////////")

        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACsizes() {
        runVAC(3, true)
        runVAC(8, true)
        runVAC(16, true)
        runVAC(30, true)
    }

    fun runVAC(nrows : Int, show: Boolean = false) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val vac = VACalg(group, exps, show)
        val result = vac.prodPowP(bases)

        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
    }

    @Test
    fun testVACtiming() {
        val nexps = 30
        runVACtiming(nexps, 10)
        runVACtiming(nexps, 100)
    }

    fun runVACtiming(nexps : Int, nbases: Int) {
        println("runVACtiming nexps = $nexps, nbases = $nbases")

        val exps = List(nexps) { group.randomElementModQ() }

        // how long to build?
        var starting11 = getSystemTimeInMillis()
        val vac = VACalg(group, exps)
        val time11 = getSystemTimeInMillis() - starting11

        var starting12 = getSystemTimeInMillis()
        var countFE = 0

        // how long to calculate. apply same VACalg to all the rows
        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            vac.prodPowP(bases)
            countFE += bases.size
        }
        val time12 = getSystemTimeInMillis() - starting12

        // heres the way we do it now
        var starting = getSystemTimeInMillis()
        var countP = 0
        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            bases.mapIndexed { idx, it ->
                countP++
                it powP exps[idx] }.reduce { a, b -> (a * b) }
        }
        val timePowP = getSystemTimeInMillis() - starting

        // println(" countFE = $countFE countP = $countP")
        val ratio = (time11 + time12).toDouble()/timePowP
        println(" timeVAC = $time11 + $time12 = ${time11 + time12}, timePowP = $timePowP ratio = ${ratio.sigfig(2)}")
    }
}

/*
runFEtiming nrows = 12, nbases = 1
 timeFE = 58 + 466 = 524, timePowP = 69 ratio = 7.5
runFEtiming nrows = 12, nbases = 10
 timeFE = 27 + 695 = 722, timePowP = 553 ratio = 1.3
runFEtiming nrows = 12, nbases = 50
 timeFE = 16 + 3107 = 3123, timePowP = 1883 ratio = 1.6
runFEtiming nrows = 12, nbases = 100
 timeFE = 9 + 5683 = 5692, timePowP = 3849 ratio = 1.4
runFEtiming nrows = 12, nbases = 1000
 timeFE = 12 + 56623 = 56635, timePowP = 37555 ratio = 1.5
 */

/*
runFEtiming nrows = 8, nbases = 100
 timeFE = 25 + 3931 = 3956, timePowP = 2517 ratio = 1.5
runFEtiming nrows = 12, nbases = 100
 timeFE = 31 + 5728 = 5759, timePowP = 3736 ratio = 1.5
runFEtiming nrows = 16, nbases = 100
 timeFE = 190 + 19006 = 19196, timePowP = 5075 ratio = 3.7
 */