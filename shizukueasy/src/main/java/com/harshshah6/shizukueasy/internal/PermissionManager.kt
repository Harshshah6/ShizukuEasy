package com.harshshah6.shizukueasy.internal

import android.content.pm.PackageManager
import com.harshshah6.shizukueasy.OnPermissionResultListener
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku permission checking and requesting.
 *
 * Wraps the raw Shizuku permission listener and dispatches results to
 * pending [OnPermissionResultListener] callbacks.
 */
internal class PermissionManager(
    private val onPermissionChanged: (Boolean) -> Unit
) {
    private companion object {
        const val REQUEST_CODE = 51738 // arbitrary unique code
    }

    private val pendingCallbacks = mutableListOf<OnPermissionResultListener>()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                onPermissionChanged(granted)
                dispatchPendingCallbacks(granted)
            }
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
     *
     * @param callback Receives the result of the permission request.
     */
    fun requestPermission(callback: OnPermissionResultListener?) {
        if (isGranted) {
            callback?.onPermissionResult(true)
            onPermissionChanged(true)
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

    private fun dispatchPendingCallbacks(granted: Boolean) {
        val callbacks: List<OnPermissionResultListener>
        synchronized(pendingCallbacks) {
            callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
        }
        callbacks.forEach { it.onPermissionResult(granted) }
    }
}
