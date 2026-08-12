package com.harshshah6.shizukueasy.advanced

import android.content.ServiceConnection
import android.os.IBinder
import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.internal.SystemServiceResolver
import com.harshshah6.shizukueasy.userservice.UserServiceManager
import rikka.shizuku.Shizuku

/**
 * Advanced Shizuku API for experienced developers.
 *
 * This layer provides direct access to Shizuku functionality that is
 * intentionally hidden from the simple API:
 * - Raw system service resolution
 * - Direct binder access
 * - UserService management
 *
 * Most developers should use the high-level capability APIs instead
 * ([ShizukuEasy.packages][com.harshshah6.shizukueasy.ShizukuEasy.packages],
 * [ShizukuEasy.shell][com.harshshah6.shizukueasy.ShizukuEasy.shell], etc.).
 *
 * ```kotlin
 * // Raw system service access
 * val pm = ShizukuEasy.advanced.getSystemService("package") { binder ->
 *     IPackageManager.Stub.asInterface(binder)
 * }
 *
 * // UserService
 * ShizukuEasy.advanced.userService.bind(MyService::class.java, ...) { result -> }
 * ```
 */
public class AdvancedShizukuApi internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    /**
     * UserService management.
     *
     * Provides a clean abstraction over Shizuku's UserService API for
     * running code with elevated privileges in a separate process.
     */
    public val userService: UserServiceManager = UserServiceManager(statusProvider)

    /**
     * Obtains a system service interface through Shizuku.
     *
     * The service binder is wrapped so that all IPC calls are proxied through
     * the Shizuku server with elevated privileges.
     *
     * @param T The AIDL interface type.
     * @param serviceName The system service name (e.g., "package", "activity").
     * @param converter Converts the wrapped [IBinder] to the desired interface,
     *   typically using `IFoo.Stub.asInterface(binder)`.
     * @return The service interface proxy.
     * @throws IllegalStateException if Shizuku is not ready.
     */
    public fun <T> getSystemService(serviceName: String, converter: (IBinder) -> T): T {
        return SystemServiceResolver.resolveOrThrow(serviceName, converter, statusProvider)
    }

    /**
     * Gets the raw Shizuku binder for direct IPC.
     *
     * @return The Shizuku binder, or `null` if not connected.
     */
    public fun getBinder(): IBinder? {
        return try {
            if (Shizuku.pingBinder()) Shizuku.getBinder() else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Binds a UserService directly using raw Shizuku API.
     *
     * For most use cases, prefer [userService] instead.
     *
     * @param args The UserService arguments.
     * @param connection The service connection callback.
     */
    public fun bindUserService(
        args: Shizuku.UserServiceArgs,
        connection: ServiceConnection
    ) {
        Shizuku.bindUserService(args, connection)
    }

    /**
     * Unbinds a UserService directly using raw Shizuku API.
     *
     * @param args The UserService arguments.
     * @param connection The service connection that was used to bind.
     * @param removeTask Whether to remove the service task.
     */
    @JvmOverloads
    public fun unbindUserService(
        args: Shizuku.UserServiceArgs,
        connection: ServiceConnection,
        removeTask: Boolean = true
    ) {
        Shizuku.unbindUserService(args, connection, removeTask)
    }

    /**
     * Checks whether the binder is alive without going through ShizukuEasy state.
     *
     * Useful for diagnosing connection issues.
     */
    public fun pingBinder(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Gets the Shizuku server UID directly.
     *
     * @return The server UID, or -1 if unavailable.
     */
    public fun getServerUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Gets the Shizuku server version directly.
     *
     * @return The server version, or -1 if unavailable.
     */
    public fun getServerVersion(): Int {
        return try {
            Shizuku.getVersion()
        } catch (_: Exception) {
            -1
        }
    }
}
