package com.harshshah6.shizukueasy.capabilities

import com.harshshah6.shizukueasy.ShizukuBackend
import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult

/**
 * Power management operations through Shizuku.
 *
 * Most power operations require the [ShizukuBackend.ROOT] backend.
 *
 * ```kotlin
 * ShizukuEasy.power.reboot().onFailure { error ->
 *     if (error is ShizukuError.InsufficientPrivilege) {
 *         showMessage("Root required for reboot.")
 *     }
 * }
 * ```
 */
public class PowerCapability internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private fun requireReady(): ShizukuError? {
        val status = statusProvider()
        if (!status.isAvailable) return ShizukuError.Unavailable()
        if (!status.isAuthorized) return ShizukuError.PermissionDenied()
        return null
    }

    private fun requireRoot(): ShizukuError? {
        requireReady()?.let { return it }
        val status = statusProvider()
        if (status.backend != ShizukuBackend.ROOT) {
            return ShizukuError.InsufficientPrivilege(
                "This operation requires ROOT backend, but current backend is ${status.backend.name}."
            )
        }
        return null
    }

    /**
     * Reboots the device.
     *
     * Requires root backend.
     *
     * @param reason An optional reason string (e.g., "recovery", "bootloader").
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun reboot(reason: String? = null): ShizukuResult<Unit> {
        requireRoot()?.let { return ShizukuResult.failure(it) }

        return try {
            val cmd = if (reason != null) "svc power reboot $reason" else "svc power reboot"
            ShizukuShellExecutor.exec(cmd)
            ShizukuResult.success(Unit)
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed("Reboot failed: ${e.message}", e)
            )
        }
    }

    /**
     * Shuts down the device.
     *
     * Requires root backend.
     *
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun shutdown(): ShizukuResult<Unit> {
        requireRoot()?.let { return ShizukuResult.failure(it) }

        return try {
            ShizukuShellExecutor.exec("svc power shutdown")
            ShizukuResult.success(Unit)
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed("Shutdown failed: ${e.message}", e)
            )
        }
    }
}
