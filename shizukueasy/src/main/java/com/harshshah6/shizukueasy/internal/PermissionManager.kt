package com.harshshah6.shizukueasy.internal

import android.content.pm.PackageManager
import com.harshshah6.shizukueasy.OnPermissionResultListener
import com.harshshah6.shizukueasy.PermissionState
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku permission checking and requesting.
 *
 * Wraps the raw Shizuku permission listener and dispatches results to
 * pending [OnPermissionResultListener] callbacks.
 */
internal class PermissionManager(
    private val onPermissionStateChanged: (PermissionState) -> Unit
) {
    private companion object {
        const val REQUEST_CODE = 51738
    }

    private val pendingCallbacks = mutableListOf<OnPermissionResultListener>()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                val state = if (granted) PermissionState.GRANTED else evaluateDeniedState()
                onPermissionStateChanged(state)
                dispatchPendingCallbacks(granted)
            }
        }

    /** The current permission state. */
    val permissionState: PermissionState
        get() = when {
            isGranted -> PermissionState.GRANTED
            shouldShowRationale -> PermissionState.DENIED_FOREVER
            else -> PermissionState.UNKNOWN
        }

    /** Whether the Shizuku permission is currently granted. */
    val isGranted: Boolean
        get() = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

    /** Whether the user has permanently denied permission. */
    val shouldShowRationale: Boolean
        get() = try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (_: Exception) {
            false
        }

    /** Starts listening for permission results. */
    fun start() {
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
    }

    /** Stops listening and clears pending callbacks. */
    fun stop() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        synchronized(pendingCallbacks) {
            pendingCallbacks.clear()
        }
    }

    /**
     * Requests Shizuku permission.
     *
     * If permission is already granted, the callback fires immediately.
     * Otherwise, the Shizuku permission dialog is shown and the callback
     * fires when the user responds.
     */
    fun requestPermission(callback: OnPermissionResultListener?) {
        if (isGranted) {
            callback?.onPermissionResult(true)
            onPermissionStateChanged(PermissionState.GRANTED)
            return
        }

        if (callback != null) {
            synchronized(pendingCallbacks) {
                pendingCallbacks.add(callback)
            }
        }

        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            callback?.onPermissionResult(false)
            synchronized(pendingCallbacks) {
                pendingCallbacks.remove(callback)
            }
        }
    }

    private fun evaluateDeniedState(): PermissionState {
        return if (shouldShowRationale) PermissionState.DENIED_FOREVER else PermissionState.DENIED
    }

    private fun dispatchPendingCallbacks(granted: Boolean) {
        val callbacks: List<OnPermissionResultListener>
        synchronized(pendingCallbacks) {
            callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
        }
        callbacks.forEach { it.onPermissionResult(granted) }
    }
}
