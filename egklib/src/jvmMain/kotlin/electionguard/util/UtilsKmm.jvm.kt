package electionguard.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger("UtilsKmmJvm")

actual fun pathExists(path: String): Boolean = Files.exists(Path.of(path))

actual fun createDirectories(directory: String): Boolean {
    if (pathExists(directory)) {
        return true
    }
    return try {
        Files.createDirectories(Path.of(directory))
        logger.warn { "error createDirectories = '$directory' " }
        true
    } catch (t: Throwable) {
        false
    }
}

actual fun isDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))

actual fun fileReadLines(filename: String): List<String> = File(filename).readLines()

actual fun fileReadBytes(filename: String): ByteArray = File(filename).readBytes()

actual fun fileReadText(filename: String): String = File(filename).readText()
