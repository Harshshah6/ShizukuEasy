package com.harshshah6.shizukueasy.internal

import com.harshshah6.shizukueasy.ConnectionState
import rikka.shizuku.Shizuku

/**
 * Manages the Shizuku binder lifecycle.
 *
 * Registers sticky binder-received and binder-dead listeners and notifies
 * the provided callbacks when state transitions occur.
 */
internal class ConnectionManager(
    private val onConnectionStateChanged: (ConnectionState) -> Unit
) {
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        onConnectionStateChanged(ConnectionState.CONNECTED)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onConnectionStateChanged(ConnectionState.DEAD)
    }

    /** Whether the binder is currently alive. */
    val isBinderAlive: Boolean
        get() = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }

    /** The current connection state. */
    val connectionState: ConnectionState
        get() = when {
            isBinderAlive -> ConnectionState.CONNECTED
            else -> ConnectionState.DISCONNECTED
        }

    /**
     * Starts listening for binder lifecycle events.
     *
     * Uses sticky listeners so the callback fires immediately if the binder
     * is already connected at the time of registration.
     */
    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    /** Stops listening for binder lifecycle events. */
    fun stop() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}
