package com.harshshah6.shizukueasy

/**
 * Represents the current state of the Shizuku connection and permission.
 *
 * @deprecated Use [ShizukuStatus] instead, which separates connection, permission,
 * and backend state. This enum will be removed in a future release.
 */
@Deprecated(
    message = "Use ShizukuStatus instead.",
    replaceWith = ReplaceWith("ShizukuStatus")
)
public enum class ShizukuState {
    /** [ShizukuEasy.init] has not been called yet. */
    NOT_INITIALIZED,

    /** Shizuku is not installed, not running, or uses an unsupported version. */
    UNAVAILABLE,

    /** Shizuku is running but permission has not been granted. */
    UNAUTHORIZED,

    /** Shizuku is running and permission is granted. Ready to use. */
    READY,

    /** The Shizuku binder has died. Waiting for reconnection. */
    DEAD
}
