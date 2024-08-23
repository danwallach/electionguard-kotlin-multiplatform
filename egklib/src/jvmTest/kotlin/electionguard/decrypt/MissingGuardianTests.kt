package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.electionExtendedHash
import electionguard.ballot.makeDoerreTrustee
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.UInt256
import electionguard.core.productionGroup
import electionguard.keyceremony.KeyCeremonyTrustee
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/** Test decryption with various combinations of missing guardinas. */

class MissingGuardianTests {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(1, 2, 3)),
            Arguments.of(listOf(1, 2, 3, 4)),
            Arguments.of(listOf(2, 3, 4)),
            Arguments.of(listOf(2, 4, 5)),
            Arguments.of(listOf(2, 3, 4, 5)),
            Arguments.of(listOf(5, 6, 7, 8, 9)),
            Arguments.of(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)),
            Arguments.of(listOf(2, 3, 4, 5, 6, 7, 9)),
            Arguments.of(listOf(2, 3, 4, 5, 6, 9)),
            Arguments.of(listOf(2, 3, 4, 5, 6, 7, 11)),
            Arguments.of(listOf(2, 3, 4, 7, 11)),
        )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testMissingGuardians(present: List<Int>) {
        val group = productionGroup()
        val nguardians = present.maxOf { it }
        val quorum = present.count()

        val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
            val seq = it + 1
            KeyCeremonyTrustee(group, "guardian$seq", seq, nguardians, quorum)
        }.sortedBy { it.xCoordinate() }

        // exchange PublicKeys
        trustees.forEach { t1 ->
            trustees.forEach { t2 ->
                t1.receivePublicKeys(t2.publicKeys().unwrap())
            }
        }

        // exchange SecretKeyShares
        trustees.forEach { t1 ->
            trustees.filter { it.id() != t1.id() }.forEach { t2 ->
                t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id()).unwrap())
            }
        }
        trustees.forEach { it.isComplete() }

        val jointPublicKey: ElementModP =
            trustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }
        val electionExtendedHash = electionExtendedHash(UInt256.random(), jointPublicKey)
        val dTrustees: List<DecryptingTrusteeDoerre> = trustees.map { makeDoerreTrustee(it, electionExtendedHash) }

        encryptDecrypt(group, ElGamalPublicKey(jointPublicKey), dTrustees, present)
    }

}



