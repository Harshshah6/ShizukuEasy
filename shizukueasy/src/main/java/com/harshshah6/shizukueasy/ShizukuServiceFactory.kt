package com.harshshah6.shizukueasy

import android.os.IBinder
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Factory for obtaining system service interfaces through Shizuku.
 *
 * This provides the foundation for accessing Android system services with
 * elevated privileges. Services obtained through this factory run with the
 * Shizuku server's identity (shell or root).
 *
 * **Example — obtaining IPackageManager:**
 * ```kotlin
 * val pm = ShizukuServiceFactory.getSystemService("package") { binder ->
 *     IPackageManager.Stub.asInterface(binder)
 * }
 * ```
 *
 * @see ShizukuEasy.getSystemService
 */
object ShizukuServiceFactory {

    /**
     * Obtains a system service interface through Shizuku.
     *
     * The service binder is obtained via [SystemServiceHelper] and wrapped with
     * [ShizukuBinderWrapper] so that all IPC calls are proxied through the
     * Shizuku server with elevated privileges.
     *
     * @param T The AIDL interface type for the service.
     * @param serviceName The system service name (e.g., "package", "activity").
     * @param converter Converts the wrapped [IBinder] to the desired interface,
     *   typically using `IFoo.Stub.asInterface(binder)`.
     * @return The service interface proxy.
     * @throws IllegalStateException if [ShizukuEasy.ready] is false.
     * @throws RuntimeException if the service cannot be obtained.
     */
    @JvmStatic
    fun <T> getSystemService(serviceName: String, converter: (IBinder) -> T): T {
        check(ShizukuEasy.ready) {
            "ShizukuEasy is not ready. Check ShizukuEasy.ready before calling getSystemService()."
        }

        val binder = SystemServiceHelper.getSystemService(serviceName)
            ?: throw RuntimeException("System service '$serviceName' not found.")

        val wrappedBinder = ShizukuBinderWrapper(binder)
        return converter(wrappedBinder)
    }

    /**
     * Obtains the raw Shizuku binder for advanced use cases.
     *
     * Most users should prefer [getSystemService] instead.
     *
     * @return The Shizuku binder, or `null` if not connected.
     */
    @JvmStatic
    fun getBinder(): IBinder? {
        return try {
            if (Shizuku.pingBinder()) {
                Shizuku.getBinder()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
