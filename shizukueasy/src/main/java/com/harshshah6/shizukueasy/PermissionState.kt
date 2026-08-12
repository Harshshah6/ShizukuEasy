package com.harshshah6.shizukueasy

/**
 * The permission state for the Shizuku API.
 *
 * This represents whether the application has been granted permission
 * to use Shizuku, independent of connection state.
 */
public enum class PermissionState {
    /** Permission state has not been determined (Shizuku not connected). */
    UNKNOWN,

    /** Shizuku permission has been granted. */
    GRANTED,

    /** Shizuku permission has been denied. */
    DENIED,

    /**
     * Shizuku permission has been permanently denied.
     *
     * The user must manually grant permission from the Shizuku app.
     */
    DENIED_FOREVER
}
