package com.harshshah6.shizukueasy.result

/**
 * Describes why a Shizuku operation failed.
 *
 * Each subclass represents a distinct failure category, allowing callers to
 * handle errors specifically:
 *
 * ```kotlin
 * when (result) {
 *     is ShizukuResult.Failure -> when (result.error) {
 *         is ShizukuError.Unavailable -> showShizukuRequired()
 *         is ShizukuError.PermissionDenied -> requestPermission()
 *         is ShizukuError.InsufficientPrivilege -> showRootRequired()
 *         else -> showGenericError(result.error.message)
 *     }
 * }
 * ```
 *
 * @property message Human-readable description of the failure.
 * @property cause The underlying exception, if any.
 */
public sealed class ShizukuError(
    public val message: String,
    public val cause: Throwable? = null
) {
    /** Shizuku is not running or not connected. */
    public class Unavailable(
        message: String = "Shizuku is not available.",
        cause: Throwable? = null
    ) : ShizukuError(message, cause)

    /** Shizuku permission has not been granted. */
    public class PermissionDenied(
        message: String = "Shizuku permission is not granted.",
        cause: Throwable? = null
    ) : ShizukuError(message, cause)

    /** The operation is not supported on the current API level or Shizuku version. */
    public class Unsupported(
        message: String,
        cause: Throwable? = null
    ) : ShizukuError(message, cause)

    /**
     * The current backend lacks the privilege for this operation.
     *
     * For example, some operations require root but Shizuku is running via ADB.
     */
    public class InsufficientPrivilege(
        message: String,
        cause: Throwable? = null
    ) : ShizukuError(message, cause)

    /** The operation failed for a reason not covered by other error types. */
    public class OperationFailed(
        message: String,
        cause: Throwable? = null
    ) : ShizukuError(message, cause)

    override fun toString(): String = "${this::class.simpleName}(message=$message)"
}
