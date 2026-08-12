package com.harshshah6.shizukueasy.internal

import android.os.IBinder
import com.harshshah6.shizukueasy.ShizukuBackend
import com.harshshah6.shizukueasy.ShizukuStatus
import com.harshshah6.shizukueasy.result.ShizukuError
import com.harshshah6.shizukueasy.result.ShizukuResult
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Internal resolver for Android system services via Shizuku.
 *
 * All IPC calls through resolved services are proxied through the Shizuku server
 * with the server's identity (shell or root).
 */
internal object SystemServiceResolver {

    /**
     * Resolves a system service interface through Shizuku.
     *
     * @param T The AIDL interface type for the service.
     * @param serviceName The system service name (e.g., "package", "activity").
     * @param converter Converts the [IBinder] to the desired interface.
     * @param statusProvider Provides the current [ShizukuStatus] for readiness checks.
     * @return A [ShizukuResult] containing the service proxy or an error.
     */
    fun <T> resolve(
        serviceName: String,
        converter: (IBinder) -> T,
        statusProvider: () -> ShizukuStatus
    ): ShizukuResult<T> {
        val status = statusProvider()

        if (!status.isAvailable) {
            return ShizukuResult.failure(ShizukuError.Unavailable())
        }
        if (!status.isAuthorized) {
            return ShizukuResult.failure(ShizukuError.PermissionDenied())
        }

        return try {
            val binder = SystemServiceHelper.getSystemService(serviceName)
                ?: return ShizukuResult.failure(
                    ShizukuError.OperationFailed("System service '$serviceName' not found.")
                )
            val wrapped = ShizukuBinderWrapper(binder)
            ShizukuResult.success(converter(wrapped))
        } catch (e: Exception) {
            ShizukuResult.failure(
                ShizukuError.OperationFailed(
                    "Failed to resolve service '$serviceName': ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * Resolves a system service, throwing on failure.
     * Used by the advanced API for callers who prefer exceptions.
     */
    fun <T> resolveOrThrow(
        serviceName: String,
        converter: (IBinder) -> T,
        statusProvider: () -> ShizukuStatus
    ): T {
        return resolve(serviceName, converter, statusProvider).getOrThrow()
    }

    /**
     * Executes an operation through a resolved system service, wrapping the result.
     *
     * @param T The service interface type.
     * @param R The operation result type.
     * @param serviceName The system service name.
     * @param converter Converts the binder to the service interface.
     * @param statusProvider Provides current status.
     * @param requiredBackend Optional backend requirement.
     * @param operation The operation to perform on the resolved service.
     */
    fun <T, R> execute(
        serviceName: String,
        converter: (IBinder) -> T,
        statusProvider: () -> ShizukuStatus,
        requiredBackend: ShizukuBackend? = null,
        operation: (T) -> R
    ): ShizukuResult<R> {
        val status = statusProvider()

        if (requiredBackend != null && status.backend != requiredBackend) {
            return ShizukuResult.failure(
                ShizukuError.InsufficientPrivilege(
                    "This operation requires ${requiredBackend.name} backend, " +
                        "but current backend is ${status.backend.name}."
                )
            )
        }

        return when (val serviceResult = resolve(serviceName, converter, statusProvider)) {
            is ShizukuResult.Success -> {
                try {
                    ShizukuResult.success(operation(serviceResult.value))
                } catch (e: SecurityException) {
                    ShizukuResult.failure(
                        ShizukuError.InsufficientPrivilege(
                            e.message ?: "Insufficient privilege for this operation.",
                            e
                        )
                    )
                } catch (e: Exception) {
                    ShizukuResult.failure(
                        ShizukuError.OperationFailed(
                            e.message ?: "Operation failed.",
                            e
                        )
                    )
                }
            }
            is ShizukuResult.Failure -> serviceResult
        }
    }
}
