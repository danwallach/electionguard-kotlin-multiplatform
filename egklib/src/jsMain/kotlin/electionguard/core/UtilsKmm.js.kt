package electionguard.core

import node.fs.BigIntStats
import node.fs.Mode
import node.fs.StatSyncFnBigIntOptions
import node.fs.Stats
import node.fs.exists
import node.fs.existsSync
import node.fs.fstatSync
import node.fs.lstatSync
import node.fs.mkdir
import node.fs.mkdirSync
import kotlin.js.Date

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() = Date().getMilliseconds().toLong()

//All file system operations are only available when targeting NodeJs
/** Does this path exist? */
actual fun pathExists(path: String): Boolean {
    return existsSync(path)
}

/** Create the named directory */
actual fun createDirectories(directory: String): Boolean {
    mkdirSync(directory, options = null)
    return pathExists(directory)
}

/** Is this path a directory? */
actual fun isDirectory(path: String): Boolean {
    //TODO implement
    return false
}

/** Read lines from a file. */
actual fun fileReadLines(filename: String): List<String> {
    return fileReadLines(filename)
}

/** Read all the bytes in a file. */
actual fun fileReadBytes(filename: String): ByteArray {
    return fileReadBytes(filename)
}

/** Read all int text in a file. */
actual fun fileReadText(filename: String): String {
    return fileReadText(filename)
}

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean {
    //Simple test for endianness of ByteArray
    val testArray = 0x1122.toByteArray()
    return testArray[0] == 0x11.toByte()
}