package electionguard.publish

import electionguard.ballot.*
import electionguard.protoconvert.publishDecryptionResult
import electionguard.protoconvert.publishElectionConfig
import electionguard.protoconvert.publishElectionInitialized
import electionguard.protoconvert.publishPlaintextBallot
import electionguard.protoconvert.publishSubmittedBallot
import electionguard.protoconvert.publishTallyResult
import electionguard.publish.ElectionRecordPath.Companion.DECRYPTION_RESULT_NAME
import electionguard.publish.ElectionRecordPath.Companion.ELECTION_CONFIG_FILE_NAME
import electionguard.publish.ElectionRecordPath.Companion.ELECTION_INITIALIZED_FILE_NAME
import electionguard.publish.ElectionRecordPath.Companion.ELECTION_RECORD_DIR
import electionguard.publish.ElectionRecordPath.Companion.SPOILED_BALLOT_FILE
import electionguard.publish.ElectionRecordPath.Companion.SUBMITTED_BALLOT_PROTO
import electionguard.publish.ElectionRecordPath.Companion.TALLY_RESULT_NAME
import io.ktor.utils.io.errors.*
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Publishes the Manifest Record to Json or protobuf files.  */
actual class Publisher actual constructor(topDir: String, publisherMode: PublisherMode) {
    private val createPublisherMode: PublisherMode = publisherMode
    private val electionRecordDir = Path.of(topDir).resolve(ELECTION_RECORD_DIR)
    private var path: ElectionRecordPath = ElectionRecordPath(topDir)

    init {
        if (createPublisherMode == PublisherMode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles()
            }
        } else if (createPublisherMode == PublisherMode.createIfMissing) {
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
    // duplicated from ElectionRecordPath so that we can use java.nio.file.Path

    fun electionConfigPath(): Path {
        return electionRecordDir.resolve(ELECTION_CONFIG_FILE_NAME).toAbsolutePath()
    }

    fun electionInitializedPath(): Path {
        return electionRecordDir.resolve(ELECTION_INITIALIZED_FILE_NAME).toAbsolutePath()
    }

    fun decryptionResultPath(): Path {
        return electionRecordDir.resolve(DECRYPTION_RESULT_NAME).toAbsolutePath()
    }

    fun spoiledBallotProtoPath(): Path {
        return electionRecordDir.resolve(SPOILED_BALLOT_FILE).toAbsolutePath()
    }

    fun submittedBallotProtoPath(): Path {
        return electionRecordDir.resolve(SUBMITTED_BALLOT_PROTO).toAbsolutePath()
    }

    fun tallyResultPath(): Path {
        return electionRecordDir.resolve(TALLY_RESULT_NAME).toAbsolutePath()
    }

    actual fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishElectionConfig()
        FileOutputStream(electionConfigPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishElectionInitialized()
        FileOutputStream(electionInitializedPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeEncryptions(
        init: ElectionInitialized,
        ballots: Iterable<SubmittedBallot>
    ) {
        writeElectionInitialized(init)
        val sink = submittedBallotSink()
        ballots.forEach {sink.writeSubmittedBallot(it) }
        sink.close()
    }

    actual fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishTallyResult()
        FileOutputStream(tallyResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishDecryptionResult()
        FileOutputStream(decryptionResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    /** Copy accepted ballots file from the inputDir to this election record.  */
    @Throws(IOException::class)
    fun copyAcceptedBallots(inputDir: String) {
        if (createPublisherMode == PublisherMode.readonly) {
            throw UnsupportedOperationException("Trying to write to readonly election record")
        }
        val source: Path = Publisher(inputDir, PublisherMode.writeonly).submittedBallotProtoPath()
        val dest: Path = submittedBallotProtoPath()
        if (source == dest) {
            return
        }
        System.out.printf("Copy AcceptedBallots from %s to %s%n", source, dest)
        Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES)
    }

    @Throws(IOException::class)
    actual fun writeInvalidBallots(invalidDir: String, invalidBallots: List<PlaintextBallot>) {
        if (!invalidBallots.isEmpty()) {
            val fileout = path.invalidBallotProtoPath(invalidDir)
            FileOutputStream(fileout).use { out ->
                for (ballot in invalidBallots) {
                    val ballotProto = ballot.publishPlaintextBallot()
                    writeDelimitedTo(ballotProto, out)
                }
                out.close()
            }
        }
    }

    actual fun submittedBallotSink(): SubmittedBallotSinkIF =
        SubmittedBallotSink(submittedBallotProtoPath().toString())

    inner class SubmittedBallotSink(path: String) : SubmittedBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path)

        override fun writeSubmittedBallot(ballot: SubmittedBallot) {
            val ballotProto: pbandk.Message = ballot.publishSubmittedBallot()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    fun writeDelimitedTo(proto: pbandk.Message, output: OutputStream) {
        val bb = ByteArrayOutputStream()
        proto.encodeToStream(bb)
        writeVlen(bb.size(), output)
        output.write(bb.toByteArray())
    }

    fun writeVlen(input: Int, output: OutputStream) {
        var value = input
        while (true) {
            if (value and 0x7F.inv() == 0) {
                output.write(value)
                return
            } else {
                output.write(value and 0x7F or 0x80)
                value = value ushr 7
            }
        }
    }
}