package com.harshshah6.shizukueasy

/**
 * Callback for [ShizukuStatus] changes.
 *
 * This is a functional interface, usable as a lambda in both Kotlin and Java.
 *
 * **Kotlin:**
 * ```kotlin
 * ShizukuEasy.addStatusListener { status ->
 *     if (status.isReady) { /* ... */ }
 * }
 * ```
 *
 * **Java:**
 * ```java
 * ShizukuEasy.addStatusListener(status -> {
 *     if (status.isReady()) { /* ... */ }
 * });
 * ```
 */
public fun interface OnStatusChangeListener {
    /**
     * Called when the Shizuku status changes.
     * @param newStatus The new [ShizukuStatus].
     */
    public fun onStatusChanged(newStatus: ShizukuStatus)
}
