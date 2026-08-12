package com.harshshah6.shizukueasy.capabilities

import android.os.IBinder
import android.os.Parcel
import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.internal.SystemServiceResolver
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * High-level package management operations through Shizuku.
 *
 * Uses Shizuku's privileged binder access to interact with the system
 * package manager service.
 *
 * ```kotlin
 * ShizukuEasy.packages.getInstalled().onSuccess { packages ->
 *     packages.forEach { println(it) }
 * }
 * ```
 *
 * @see ShizukuResult
 */
public class PackageCapability internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private fun requireReady(): ShizukuError? {
        val status = statusProvider()
        if (!status.isAvailable) return ShizukuError.Unavailable()
        if (!status.isAuthorized) return ShizukuError.PermissionDenied()
        return null
    }

    /**
     * Checks whether a package is installed using `cmd package path`.
     *
     * @param packageName The package name to check.
     * @return [ShizukuResult] containing `true` if installed, `false` otherwise.
     */
    public fun isInstalled(packageName: String): ShizukuResult<Boolean> {
        return shellViaTransact("cmd package path $packageName").let { result ->
            when (result) {
                is ShizukuResult.Success -> ShizukuResult.success(
                    result.value.isNotBlank() && result.value.contains("package:")
                )
                is ShizukuResult.Failure -> {
                    // Non-zero exit = package not found
                    if (result.error is ShizukuError.OperationFailed) {
                        ShizukuResult.success(false)
                    } else {
                        result
                    }
                }
            }
        }
    }

    /**
     * Gets a list of all installed package names.
     *
     * @return [ShizukuResult] containing the list of package names.
     */
    public fun getInstalled(): ShizukuResult<List<String>> {
        return shellViaTransact("cmd package list packages").map { output ->
            output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .sorted()
        }
    }

    /**
     * Enables a disabled package.
     *
     * @param packageName The package to enable.
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun enable(packageName: String): ShizukuResult<Unit> {
        return shellViaTransact("cmd package enable $packageName").map { }
    }

    /**
     * Disables a package for the current user.
     *
     * @param packageName The package to disable.
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun disable(packageName: String): ShizukuResult<Unit> {
        return shellViaTransact("cmd package disable-user $packageName").map { }
    }

    /**
     * Clears application data for a package.
     *
     * @param packageName The package whose data to clear.
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun clearData(packageName: String): ShizukuResult<Unit> {
        return shellViaTransact("pm clear $packageName").map { }
    }

    /**
     * Uninstalls a package for the current user.
     *
     * @param packageName The package to uninstall.
     * @return [ShizukuResult] indicating success or failure.
     */
    public fun uninstall(packageName: String): ShizukuResult<Unit> {
        return shellViaTransact("pm uninstall $packageName").map { }
    }

    /**
     * Gets the APK path for a package.
     *
     * @param packageName The package name.
     * @return [ShizukuResult] containing the APK path, or failure if not found.
     */
    public fun getPath(packageName: String): ShizukuResult<String> {
        return shellViaTransact("cmd package path $packageName").map { output ->
            output.lines()
                .firstOrNull { it.startsWith("package:") }
                ?.removePrefix("package:")?.trim()
                ?: throw IllegalStateException("Package '$packageName' not found.")
        }
    }

    /**
     * Executes a shell command via Shizuku's remote transact mechanism.
     */
    private fun shellViaTransact(command: String): ShizukuResult<String> {
        requireReady()?.let { return ShizukuResult.failure(it) }

        return try {
            val result = ShizukuShellExecutor.exec(command)
            ShizukuResult.success(result)
        } catch (e: SecurityException) {
            ShizukuResult.failure(
                ShizukuError.InsufficientPrivilege(e.message ?: "Permission denied.", e)
            )
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed("Command failed: ${e.message}", e)
            )
        }
    }
}
