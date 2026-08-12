package com.harshshah6.shizukueasy

/**
 * The connection state between the application and the Shizuku server.
 *
 * This represents whether the Shizuku binder is alive and reachable,
 * independent of permission state.
 */
public enum class ConnectionState {
    /** [ShizukuEasy.init] has not been called. */
    NOT_INITIALIZED,

    /** Shizuku is not running or unreachable. */
    DISCONNECTED,

    /** The Shizuku binder is alive and reachable. */
    CONNECTED,

    /** The Shizuku binder has died. Waiting for reconnection. */
    DEAD
}
