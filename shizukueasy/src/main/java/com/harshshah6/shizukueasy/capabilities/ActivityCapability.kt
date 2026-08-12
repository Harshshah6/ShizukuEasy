package com.harshshah6.shizukueasy.capabilities

import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult

/**
 * High-level activity management operations through Shizuku.
 *
 * ```kotlin
 * ShizukuEasy.activities.forceStop("com.example.app")
 * ```
 */
public class ActivityCapability internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private fun requireReady(): ShizukuError? {
        val status = statusProvider()
        if (!status.isAvailable) return ShizukuError.Unavailable()
        if (!status.isAuthorized) return ShizukuError.PermissionDenied()
        return null
    }

    /**
     * Force-stops an application.
     *
     * @param packageName The package to force-stop.
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun forceStop(packageName: String): ShizukuResult<Unit> {
        requireReady()?.let { return ShizukuResult.failure(it) }

        return try {
            ShizukuShellExecutor.exec("am force-stop $packageName")
            ShizukuResult.success(Unit)
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed("Force stop failed: ${e.message}", e)
            )
        }
    }
}
