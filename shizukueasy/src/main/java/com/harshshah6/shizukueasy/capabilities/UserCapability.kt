package com.harshshah6.shizukueasy.capabilities

import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult

/**
 * High-level user management operations through Shizuku.
 *
 * ```kotlin
 * ShizukuEasy.users.getCurrentUserId().onSuccess { userId ->
 *     Log.d("Demo", "Current user: $userId")
 * }
 * ```
 */
public class UserCapability internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private fun requireReady(): ShizukuError? {
        val status = statusProvider()
        if (!status.isAvailable) return ShizukuError.Unavailable()
        if (!status.isAuthorized) return ShizukuError.PermissionDenied()
        return null
    }

    private fun execShell(command: String): ShizukuResult<String> {
        requireReady()?.let { return ShizukuResult.failure(it) }

        return try {
            val stdout = ShizukuShellExecutor.exec(command)
            ShizukuResult.success(stdout)
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed("Shell command failed: ${e.message}", e)
            )
        }
    }

    /**
     * Gets the current foreground user ID.
     *
     * @return [ShizukuResult] containing the user ID.
     */
    public fun getCurrentUserId(): ShizukuResult<Int> {
        return execShell("am get-current-user").map { output ->
            output.trim().toIntOrNull()
                ?: throw IllegalStateException("Could not parse user ID: $output")
        }
    }

    /**
     * Gets the list of user profiles.
     *
     * @return [ShizukuResult] containing user IDs and names.
     */
    public fun getProfiles(): ShizukuResult<List<UserProfile>> {
        return execShell("pm list users").map { output ->
            output.lines()
                .filter { it.contains("UserInfo{") }
                .mapNotNull { line ->
                    val match = Regex("""UserInfo\{(\d+):([^:]*):""").find(line)
                    match?.let {
                        UserProfile(
                            id = it.groupValues[1].toInt(),
                            name = it.groupValues[2]
                        )
                    }
                }
        }
    }
}

/**
 * Represents an Android user profile.
 *
 * @property id The user ID.
 * @property name The user's display name.
 */
public data class UserProfile(
    val id: Int,
    val name: String
)
