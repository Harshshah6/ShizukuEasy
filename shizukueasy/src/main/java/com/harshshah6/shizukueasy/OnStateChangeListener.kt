package com.harshshah6.shizukueasy

/**
 * Callback for Shizuku state changes.
 *
 * @deprecated Use [OnStatusChangeListener] instead.
 */
@Deprecated(
    message = "Use OnStatusChangeListener instead.",
    replaceWith = ReplaceWith("OnStatusChangeListener")
)
public fun interface OnStateChangeListener {
    /**
     * Called when the Shizuku state changes.
     * @param newState The new [ShizukuState].
     */
    @Suppress("DEPRECATION")
    public fun onStateChanged(newState: ShizukuState)
}
