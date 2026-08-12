package com.harshshah6.shizukueasy.userservice

import android.app.Service
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku UserService lifecycle.
 *
 * UserServices run in a separate process with the Shizuku server's identity
 * (shell or root), allowing privileged operations without requiring AIDL stubs
 * for system services.
 *
 * This manager hides [Shizuku.UserServiceArgs], service binding boilerplate,
 * and cleanup.
 *
 * ```kotlin
 * ShizukuEasy.advanced.userService.bind(
 *     serviceClass = MyPrivilegedService::class.java,
 *     converter = { binder -> IMyService.Stub.asInterface(binder) }
 * ) { result ->
 *     result.onSuccess { service ->
 *         service.doPrivilegedWork()
 *     }
 * }
 * ```
 */
public class UserServiceManager internal constructor(
    private val statusProvider: () -> ShizukuStatus
) {
    private val activeConnections = mutableMapOf<Class<*>, ServiceConnection>()

    /**
     * Binds to a UserService.
     *
     * The service runs in a separate process with Shizuku's identity.
     *
     * @param T The service interface type.
     * @param serviceClass The [Service] implementation class.
     * @param converter Converts the [IBinder] to the desired interface.
     * @param processNameSuffix Optional process name suffix for identification.
     * @param debuggable Whether the service process should be debuggable.
     * @param version Service version for update detection.
     * @param callback Called with the bound service interface or an error.
     */
    @JvmOverloads
    public fun <T : IInterface> bind(
        serviceClass: Class<out Service>,
        converter: (IBinder) -> T,
        processNameSuffix: String = serviceClass.simpleName,
        debuggable: Boolean = false,
        version: Int = 1,
        callback: (ShizukuResult<T>) -> Unit
    ) {
        val status = statusProvider()
        if (!status.isReady) {
            val error = if (!status.isAvailable) {
                ShizukuError.Unavailable()
            } else {
                ShizukuError.PermissionDenied()
            }
            callback(ShizukuResult.failure(error))
            return
        }

        // Unbind existing connection for this class if any
        unbind(serviceClass)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder != null) {
                    try {
                        callback(ShizukuResult.success(converter(binder)))
                    } catch (e: Exception) {
                        callback(
                            ShizukuResult.failure(
                                ShizukuError.OperationFailed(
                                    "Failed to convert binder: ${e.message}",
                                    e
                                )
                            )
                        )
                    }
                } else {
                    callback(
                        ShizukuResult.failure(
                            ShizukuError.OperationFailed("Received null binder from UserService.")
                        )
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(activeConnections) {
                    activeConnections.remove(serviceClass)
                }
            }
        }

        synchronized(activeConnections) {
            activeConnections[serviceClass] = connection
        }

        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(
                    statusProvider().backend.name, // placeholder, actual package is resolved by Shizuku
                    serviceClass.name
                )
            )
                .processNameSuffix(processNameSuffix)
                .debuggable(debuggable)
                .version(version)

            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            synchronized(activeConnections) {
                activeConnections.remove(serviceClass)
            }
            callback(
                ShizukuResult.failure(
                    ShizukuError.OperationFailed(
                        "Failed to bind UserService: ${e.message}",
                        e
                    )
                )
            )
        }
    }

    /**
     * Unbinds a previously bound UserService.
     *
     * @param serviceClass The service class to unbind.
     */
    public fun unbind(serviceClass: Class<out Service>) {
        val connection: ServiceConnection?
        synchronized(activeConnections) {
            connection = activeConnections.remove(serviceClass)
        }
        if (connection != null) {
            try {
                val args = Shizuku.UserServiceArgs(
                    ComponentName("", serviceClass.name)
                )
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Exception) {
                // Best effort cleanup
            }
        }
    }

    /**
     * Unbinds all active UserServices.
     */
    public fun unbindAll() {
        val connections: Map<Class<*>, ServiceConnection>
        synchronized(activeConnections) {
            connections = activeConnections.toMap()
            activeConnections.clear()
        }
        connections.forEach { (serviceClass, connection) ->
            try {
                val args = Shizuku.UserServiceArgs(
                    ComponentName("", serviceClass.name)
                )
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Exception) {
                // Best effort cleanup
            }
        }
    }
}
