package publish

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.protoconvert.ElectionRecordToProto
import electionguard.protoconvert.PlaintextTallyConvert
import electionguard.protoconvert.SubmittedBallotConvert
import io.ktor.utils.io.errors.*
import pbandk.encodeToStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Publishes the Manifest Record to Json or protobuf files.  */
class Publisher {
    enum class Mode {
        readonly,   // read files only
        writeonly,  // write new files, but do not create directories
        createIfMissing,  // create directories if not already exist
        createNew   // create clean directories
    }

    private val topdir: String
    private val createMode: Mode
    private val electionRecordDir: Path

    constructor(where: String, createMode: Mode) {
        this.topdir = where
        this.createMode = createMode
        this.electionRecordDir = Path.of(where).resolve(ELECTION_RECORD_DIR)
        
        if (createMode == Mode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles()
            }
        } else if (createMode == Mode.createIfMissing) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }
        } else {
            check(Files.exists(electionRecordDir)) { "Non existing election directory $electionRecordDir" }
        }
    }

    internal constructor(electionRecordDir: Path, createMode : Mode) {
        this.createMode = createMode
        topdir = electionRecordDir.toAbsolutePath().toString()
        this.electionRecordDir = electionRecordDir
        
        if (createMode == Mode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles()
            }

        } else if (createMode == Mode.createIfMissing) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }

        } else {
            check(Files.exists(electionRecordDir)) { "Non existing election directory $electionRecordDir" }
        }
    }

    /** Delete everything in the output directory, but leave that directory.  */
    @Throws(IOException::class)
    private fun removeAllFiles() {
        if (!electionRecordDir.toFile().exists()) {
            return
        }
        val filename: String = electionRecordDir.getFileName().toString()
        if (!filename.startsWith("election_record")) {
            throw RuntimeException(
                String.format(
                    "Publish directory '%s' should start with 'election_record'",
                    filename
                )
            )
        }
        Files.walk(electionRecordDir)
            .filter { p: Path -> p != electionRecordDir }
            .map { obj: Path -> obj.toFile() }
            .sorted { o1: File, o2: File? -> -o1.compareTo(o2) }
            .forEach { f: File -> f.delete() }
    }

    /** Make sure output dir exists and is writeable.  */
    fun validateOutputDir(error: java.util.Formatter): Boolean {
        if (!Files.exists(electionRecordDir)) {
            error.format(" Output directory '%s' does not exist%n", electionRecordDir)
            return false
        }
        if (!Files.isDirectory(electionRecordDir)) {
            error.format(" Output directory '%s' is not a directory%n", electionRecordDir)
            return false
        }
        if (!Files.isWritable(electionRecordDir)) {
            error.format(" Output directory '%s' is not writeable%n", electionRecordDir)
            return false
        }
        if (!Files.isExecutable(electionRecordDir)) {
            error.format(" Output directory '%s' is not executable%n", electionRecordDir)
            return false
        }
        return true
    }

    ////////////////////
    fun electionRecordProtoPath(): Path {
        return electionRecordDir.resolve(ELECTION_RECORD_FILE_NAME).toAbsolutePath()
    }

    fun submittedBallotProtoPath(): Path {
        return electionRecordDir.resolve(SUBMITTED_BALLOT_PROTO).toAbsolutePath()
    }

    fun spoiledBallotProtoPath(): Path {
        return electionRecordDir.resolve(SPOILED_BALLOT_FILE).toAbsolutePath()
    }

    /** Publishes the entire election record as proto.  */
    @Throws(IOException::class)
    fun writeElectionRecordProto(
        groupContext: GroupContext,
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>,
        devices: Iterable<EncryptionDevice>,
        submittedBallots: Iterable<SubmittedBallot>?,
        ciphertextTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        spoiledBallots: Iterable<PlaintextTally>?,
        availableGuardians: List<AvailableGuardian>?
    ) {
        if (createMode == Mode.readonly) {
            throw UnsupportedOperationException("Trying to write to readonly election record")
        }
        if (submittedBallots != null) {
            val submittedBallotConvert = SubmittedBallotConvert(groupContext)
            FileOutputStream(submittedBallotProtoPath().toFile()).use { out ->
                for (ballot in submittedBallots) {
                    val ballotProto = submittedBallotConvert.translateToProto(ballot)
                    ballotProto.encodeToStream(out)
                }
            }
        }
        if (spoiledBallots != null) {
            val plaintextTallyConvert = PlaintextTallyConvert(groupContext)
            FileOutputStream(spoiledBallotProtoPath().toFile()).use { out ->
                for (ballot in spoiledBallots) {
                    val ballotProto = plaintextTallyConvert.translateToProto(ballot)
                    ballotProto.encodeToStream(out)
                }
            }
        }

        /*
        translateToProto(
        version : String,
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>?,
        devices: Iterable<EncryptionDevice>,
        encryptedTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        availableGuardians: List<AvailableGuardian>?,
         */
        val electionRecordProto = ElectionRecordToProto(groupContext).translateToProto(
            "version",
            manifest,
            context,
            constants,
            guardianRecords,
            devices,
            ciphertextTally,
            decryptedTally,
            availableGuardians
        )
        FileOutputStream(electionRecordProtoPath().toFile()).use {
                out -> electionRecordProto.encodeToStream(out) }
    }

    /** Copy accepted ballots file from the inputDir to this election record.  */
    @Throws(IOException::class)
    fun copyAcceptedBallots(inputDir: String) {
        if (createMode == Mode.readonly) {
            throw UnsupportedOperationException("Trying to write to readonly election record")
        }
        val source: Path = Publisher(inputDir, Mode.writeonly).submittedBallotProtoPath()
        val dest: Path = submittedBallotProtoPath()
        if (source == dest) {
            return
        }
        System.out.printf("Copy AcceptedBallots from %s to %s%n", source, dest)
        Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES)
    }

    companion object {
        const val ELECTION_RECORD_DIR = "election_record"

        //// proto
        const val PROTO_SUFFIX = ".protobuf"
        const val ELECTION_RECORD_FILE_NAME = "electionRecord" + PROTO_SUFFIX
        const val GUARDIANS_FILE = "guardians" + PROTO_SUFFIX
        const val SUBMITTED_BALLOT_PROTO = "submittedBallots" + PROTO_SUFFIX
        const val SPOILED_BALLOT_FILE = "spoiledBallotsTally" + PROTO_SUFFIX
        const val TRUSTEES_FILE = "trustees" + PROTO_SUFFIX
    }
}