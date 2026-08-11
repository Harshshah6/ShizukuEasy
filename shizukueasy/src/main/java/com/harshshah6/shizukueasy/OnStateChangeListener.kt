package com.harshshah6.shizukueasy

/**
 * Callback for Shizuku state changes.
 *
 * This is a functional interface, usable as a lambda in both Kotlin and Java.
 *
 * **Kotlin:**
 * ```kotlin
 * ShizukuEasy.addStateChangeListener { state ->
 *     Log.d("Shizuku", "State: $state")
 * }
 * ```
 *
 * **Java:**
 * ```java
 * ShizukuEasy.addStateChangeListener(state -> {
 *     Log.d("Shizuku", "State: " + state);
 * });
 * ```
 */
fun interface OnStateChangeListener {
    /**
     * Called when the Shizuku state changes.
     * @param newState The new [ShizukuState].
     */
    fun onStateChanged(newState: ShizukuState)
}
