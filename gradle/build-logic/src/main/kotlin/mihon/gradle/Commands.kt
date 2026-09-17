package mihon.gradle

import org.gradle.api.Project
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

// Git is needed in your system PATH for these commands to work.
// If it's not installed, we fallback to default values.
fun Project.getLatestCommitCount(): String {
    return try {
        exec("git rev-list --count HEAD")
    } catch (e: Exception) {
        "1"
    }
}

fun Project.getLatestCommitSha(): String {
    return try {
        exec("git rev-parse --short HEAD")
    } catch (e: Exception) {
        "unknown"
    }
}

/**
 * @param useLatestCommitTime If `true`, the build time is based on the timestamp of the last Git commit;
 *                          otherwise, the current time is used. Both are in UTC.
 * @return An ISO 8601 formatted string representing the build time.
 */
fun Project.getBuildTime(useLatestCommitTime: Boolean): String {
    return if (useLatestCommitTime) {
        val epoch = try {
            exec("git log -1 --format=%ct").toLong()
        } catch (e: Exception) {
            0L
        }
        Instant.fromEpochSeconds(epoch).toString()
    } else {
        val now = Clock.System.now()
        (now - now.nanosecondsOfSecond.nanoseconds).toString()
    }
}

fun Project.exec(command: String): String {
    return providers.exec {
        commandLine = command.split(" ")
    }
        .standardOutput
        .asText
        .get()
        .trim()
}
