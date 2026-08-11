package com.harshshah6.shizukueasy

/**
 * Represents the current state of the Shizuku connection and permission.
 *
 * The state progresses through a lifecycle:
 * [NOT_INITIALIZED] → [UNAVAILABLE] or [UNAUTHORIZED] or [READY]
 *
 * If the binder dies, the state transitions to [DEAD] and may return to
 * [READY] or [UNAUTHORIZED] when the binder reconnects.
 */
enum class ShizukuState {
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
