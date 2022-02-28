package electionguard.protoconvert

import electionguard.ballot.*
import pbandk.ByteArr

fun ElectionRecord.translateToProto(): electionguard.protogen.ElectionRecord {
    return translateToProto(
        this.protoVersion,
        this.manifest,
        this.context,
        this.constants,
        this.guardianRecords,
        this.devices,
        this.encryptedTally,
        this.decryptedTally,
        this.availableGuardians,
    )
}

fun translateToProto(
    version: String,
    manifest: Manifest,
    context: ElectionContext,
    constants: ElectionConstants,
    guardianRecords: List<GuardianRecord>?,
    devices: Iterable<EncryptionDevice>,
    encryptedTally: CiphertextTally?,
    decryptedTally: PlaintextTally?,
    availableGuardians: List<AvailableGuardian>?,
): electionguard.protogen.ElectionRecord {

    return electionguard.protogen.ElectionRecord(
        version,
        constants.publishConstants(),
        manifest.publishManifest(),
        context.publishContext(),
        guardianRecords?.map { it.publishGuardianRecord() } ?: emptyList(),
        devices.map { it.publishDevice() },
        encryptedTally?.let { encryptedTally.publishCiphertextTally() },
        decryptedTally?.let { decryptedTally.publishPlaintextTally() },
        availableGuardians?.map { it.publishAvailableGuardian() } ?: emptyList(),
    )
}

private fun AvailableGuardian.publishAvailableGuardian(): electionguard.protogen.AvailableGuardian {
    return electionguard.protogen.AvailableGuardian(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoordinate.publishElementModQ(),
    )
}

private fun ElectionConstants.publishConstants(): electionguard.protogen.ElectionConstants {
    return electionguard.protogen.ElectionConstants(
        this.name,
        ByteArr(this.largePrime),
        ByteArr(this.smallPrime),
        ByteArr(this.cofactor),
        ByteArr(this.generator),
    )
}

private fun ElectionContext.publishContext(): electionguard.protogen.ElectionContext {
    val extendedData: List<electionguard.protogen.ElectionContext.ExtendedDataEntry> =
        if (this.extendedData == null) {
            emptyList()
        } else {
            this.extendedData.map { (key, value) -> publishExtendedData(key, value) }
        }


    return electionguard.protogen.ElectionContext(
        this.numberOfGuardians,
        this.quorum,
        this.jointPublicKey.publishElementModP(),
        this.manifestHash.publishElementModQ(),
        this.cryptoBaseHash.publishElementModQ(),
        this.cryptoExtendedBaseHash.publishElementModQ(),
        this.commitmentHash.publishElementModQ(),
        extendedData
    )
}

private fun publishExtendedData(key: String, value: String): electionguard.protogen.ElectionContext.ExtendedDataEntry {
    return electionguard.protogen.ElectionContext.ExtendedDataEntry(key, value)
}


private fun EncryptionDevice.publishDevice(): electionguard.protogen.EncryptionDevice {
    return electionguard.protogen.EncryptionDevice(
        this.deviceId,
        this.sessionId,
        this.launchCode,
        this.location
    )
}

private fun GuardianRecord.publishGuardianRecord(): electionguard.protogen.GuardianRecord {
    return electionguard.protogen.GuardianRecord(
        this.guardianId,
        this.xCoordinate,
        this.guardianPublicKey.publishElementModP(),
        this.coefficientCommitments.map { it.publishElementModP() },
        this.coefficientProofs.map { it.publishSchnorrProof() },
    )
}