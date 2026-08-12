package com.harshshah6.shizukueasy.capabilities

import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult

/**
 * Shell command execution through Shizuku.
 *
 * Commands execute with the Shizuku server's identity (shell or root).
 *
 * ```kotlin
 * ShizukuEasy.shell.exec("pm list packages").onSuccess { result ->
 *     println("Exit: ${result.exitCode}")
 *     println("Output: ${result.stdout}")
 * }
 * ```
 */
public class ShellCapability internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private fun requireReady(): ShizukuError? {
        val status = statusProvider()
        if (!status.isAvailable) return ShizukuError.Unavailable()
        if (!status.isAuthorized) return ShizukuError.PermissionDenied()
        return null
    }

    /**
     * Executes a shell command.
     *
     * @param command The command string to execute.
     * @return [ShizukuResult] containing the [ShellOutput].
     */
    public fun exec(command: String): ShizukuResult<ShellOutput> {
        requireReady()?.let { return ShizukuResult.failure(it) }

        return try {
            val stdout = ShizukuShellExecutor.exec(command)
            ShizukuResult.success(ShellOutput(0, stdout, ""))
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed(
                    "Shell command failed: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Executes a shell command with explicit arguments.
     *
     * @param command The command and arguments.
     * @return [ShizukuResult] containing the [ShellOutput].
     */
    public fun exec(command: List<String>): ShizukuResult<ShellOutput> {
        return exec(command.joinToString(" "))
    }
}

/**
 * The output of a shell command executed through Shizuku.
 *
 * @property exitCode The process exit code (0 = success).
 * @property stdout Standard output.
 * @property stderr Standard error output.
 */
public data class ShellOutput(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    /** `true` if the command exited with code 0. */
    val isSuccess: Boolean get() = exitCode == 0
}
