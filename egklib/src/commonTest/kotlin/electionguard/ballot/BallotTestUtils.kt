package electionguard.ballot

import com.github.michaelbull.result.unwrap
import electionguard.core.UInt256
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.model.Guardian
import electionguard.model.PublicKeys

fun makeDoerreTrustee(ktrustee: KeyCeremonyTrustee, electionId : UInt256): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(
        ktrustee.id(),
        ktrustee.xCoordinate(),
        ktrustee.guardianPublicKey(),
        ktrustee.computeSecretKeyShare(),
    )
}

fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.publicKeys().unwrap()
    return Guardian(
        trustee.id(),
        trustee.xCoordinate(),
        publicKeys.coefficientProofs,
    )
}

fun makeGuardian(publicKeys: PublicKeys): Guardian {
    return Guardian(
        publicKeys.guardianId,
        publicKeys.guardianXCoordinate,
        publicKeys.coefficientProofs,
    )
}
