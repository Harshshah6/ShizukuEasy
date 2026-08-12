package com.harshshah6.shizukueasy

/**
 * Composite status of the Shizuku connection.
 *
 * Combines [ConnectionState], [PermissionState], and [ShizukuBackend] into
 * a single snapshot. Use the convenience properties to quickly check readiness.
 *
 * ```kotlin
 * val status = ShizukuEasy.status
 * if (status.isReady) {
 *     // Safe to call Shizuku APIs
 * }
 * ```
 *
 * @property connection The current binder connection state.
 * @property permission The current permission state.
 * @property backend The detected Shizuku server backend.
 */
public data class ShizukuStatus(
    val connection: ConnectionState,
    val permission: PermissionState,
    val backend: ShizukuBackend
) {
    /** `true` if the Shizuku binder is alive and reachable. */
    val isAvailable: Boolean get() = connection == ConnectionState.CONNECTED

    /** `true` if Shizuku permission has been granted. */
    val isAuthorized: Boolean get() = permission == PermissionState.GRANTED

    /** `true` if Shizuku is both [isAvailable] and [isAuthorized]. Safe to use Shizuku APIs. */
    val isReady: Boolean get() = isAvailable && isAuthorized

    public companion object {
        /** Initial status before [ShizukuEasy.init] is called. */
        @JvmField
        public val INITIAL: ShizukuStatus = ShizukuStatus(
            connection = ConnectionState.NOT_INITIALIZED,
            permission = PermissionState.UNKNOWN,
            backend = ShizukuBackend.UNKNOWN
        )
    }
}
