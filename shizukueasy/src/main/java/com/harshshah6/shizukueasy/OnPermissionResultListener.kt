package com.harshshah6.shizukueasy

/**
 * Callback for the result of a Shizuku permission request.
 *
 * This is a functional interface, usable as a lambda in both Kotlin and Java.
 *
 * **Kotlin:**
 * ```kotlin
 * ShizukuEasy.requestPermission { granted ->
 *     if (granted) { /* ... */ }
 * }
 * ```
 *
 * **Java:**
 * ```java
 * ShizukuEasy.requestPermission(granted -> {
 *     if (granted) { /* ... */ }
 * });
 * ```
 */
fun interface OnPermissionResultListener {
    /**
     * Called with the permission request result.
     * @param granted `true` if the user granted Shizuku permission.
     */
    fun onPermissionResult(granted: Boolean)
}
