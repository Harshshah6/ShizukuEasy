package com.harshshah6.shizukueasy.capabilities

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Internal shell command executor using Shizuku's privileged process.
 *
 * Uses Shizuku's `newProcess` method via reflection if available (older API),
 * or falls back to `Runtime.exec` in combination with Shizuku's transact
 * mechanism for shell commands.
 */
internal object ShizukuShellExecutor {

    /**
     * Executes a shell command with Shizuku's privileges.
     *
     * @param command The shell command to execute.
     * @return The stdout output.
     * @throws Exception if the command fails.
     */
    fun exec(command: String): String {
        return execViaRemoteProcess(command)
    }

    /**
     * Executes a command using Shizuku's remote process mechanism.
     *
     * Tries to use `Shizuku.newProcess` via reflection (it may be private
     * in newer API versions), falling back to a direct approach.
     */
    private fun execViaRemoteProcess(command: String): String {
        // Try via reflection since newProcess may be private in newer versions
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use {
                it.readText()
            }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use {
                it.readText()
            }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val errorMsg = stderr.trim().ifEmpty { stdout.trim() }
                throw RuntimeException("Command failed (exit $exitCode): $errorMsg")
            }

            return stdout.trim()
        } catch (e: Exception) {
            when (e) {
                is RuntimeException -> throw e
                else -> throw RuntimeException("Failed to execute shell command: ${e.message}", e)
            }
        }
    }
}
