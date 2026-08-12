package com.harshshah6.shizukueasy

import android.content.Context
import android.os.IBinder
import android.util.Log
import com.harshshah6.shizukueasy.advanced.AdvancedShizukuApi
import com.harshshah6.shizukueasy.capabilities.ActivityCapability
import com.harshshah6.shizukueasy.capabilities.PackageCapability
import com.harshshah6.shizukueasy.capabilities.PowerCapability
import com.harshshah6.shizukueasy.capabilities.ShellCapability
import com.harshshah6.shizukueasy.capabilities.UserCapability
import com.harshshah6.shizukueasy.internal.ConnectionManager
import com.harshshah6.shizukueasy.internal.PermissionManager
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Shizuku without the boilerplate.
 *
 * ShizukuEasy manages binder connections, permission handling, and state tracking
 * so you can focus on using Shizuku's capabilities.
 *
 * ## Quick Start
 * ```kotlin
 * // Initialize once (Application.onCreate or Activity.onCreate)
 * ShizukuEasy.init(this)
 *
 * // Check readiness
 * if (ShizukuEasy.isReady) {
 *     val packages = ShizukuEasy.packages.getInstalled()
 * }
 *
 * // Or react to readiness
 * ShizukuEasy.onReady {
 *     val packages = ShizukuEasy.packages.getInstalled()
 * }
 * ```
 *
 * ## API Layers
 * - **Simple**: [isReady], [isAvailable], [isAuthorized], [requestPermission], [onReady]
 * - **Capabilities**: [packages], [users], [activities], [power], [shell]
 * - **Advanced**: [advanced] — raw binder access, system services, UserService
 *
 * @see ShizukuStatus
 * @see ShizukuBackend
 */
public object ShizukuEasy {

    private const val TAG = "ShizukuEasy"

    @Volatile
    private var initialized = false

    @Volatile
    private var currentConnectionState: ConnectionState = ConnectionState.NOT_INITIALIZED

    @Volatile
    private var currentPermissionState: PermissionState = PermissionState.UNKNOWN

    @Volatile
    private var currentBackend: ShizukuBackend = ShizukuBackend.UNKNOWN

    private val statusListeners = CopyOnWriteArrayList<OnStatusChangeListener>()
    private val readyCallbacks = CopyOnWriteArrayList<() -> Unit>()

    private lateinit var connectionManager: ConnectionManager
    private lateinit var permissionManager: PermissionManager

    // ── Simple API ──────────────────────────────────────────────────────

    /**
     * The current composite status.
     *
     * Combines connection, permission, and backend state.
     */
    @JvmStatic
    public val status: ShizukuStatus
        get() = ShizukuStatus(
            connection = currentConnectionState,
            permission = currentPermissionState,
            backend = currentBackend
        )

    /** `true` if the Shizuku binder is alive and reachable. */
    @JvmStatic
    public val isAvailable: Boolean
        get() = currentConnectionState == ConnectionState.CONNECTED

    /** `true` if Shizuku permission has been granted. */
    @JvmStatic
    public val isAuthorized: Boolean
        get() = currentPermissionState == PermissionState.GRANTED

    /**
     * `true` if Shizuku is connected and permission is granted.
     *
     * When this is `true`, you can safely use capability APIs and [advanced].
     */
    @JvmStatic
    public val isReady: Boolean
        get() = isAvailable && isAuthorized

    /** The detected backend type of the running Shizuku server. */
    @JvmStatic
    public val backend: ShizukuBackend
        get() = currentBackend

    /** The Shizuku server version, or -1 if unavailable. */
    @JvmStatic
    public val serverVersion: Int
        get() = try {
            if (isAvailable) Shizuku.getVersion() else -1
        } catch (_: Exception) {
            -1
        }

    // ── Lifecycle ───────────────────────────────────────────────────────

    /**
     * Initializes ShizukuEasy.
     *
     * Call this once from `Application.onCreate()` or `Activity.onCreate()`.
     * Subsequent calls are safe and will be ignored.
     *
     * This registers binder and permission listeners. Status updates
     * automatically as Shizuku connects, disconnects, or permission changes.
     *
     * @param context Any context (application context is extracted internally).
     */
    @JvmStatic
    public fun init(context: Context) {
        if (initialized) {
            Log.d(TAG, "Already initialized, ignoring duplicate init() call.")
            return
        }

        // Ensure we hold the application context only.
        @Suppress("UNUSED_VARIABLE")
        val appContext = context.applicationContext

        connectionManager = ConnectionManager(::onConnectionStateChanged)
        permissionManager = PermissionManager(::onPermissionStateChanged)

        initialized = true
        connectionManager.start()
        permissionManager.start()

        Log.d(TAG, "Initialized.")
    }

    /**
     * Tears down ShizukuEasy and releases all listeners.
     *
     * After calling this, [status] resets to [ShizukuStatus.INITIAL].
     * For most apps, calling this is unnecessary — listeners are lightweight.
     */
    @JvmStatic
    public fun destroy() {
        if (!initialized) return

        connectionManager.stop()
        permissionManager.stop()
        statusListeners.clear()
        readyCallbacks.clear()

        initialized = false
        currentConnectionState = ConnectionState.NOT_INITIALIZED
        currentPermissionState = PermissionState.UNKNOWN
        currentBackend = ShizukuBackend.UNKNOWN

        Log.d(TAG, "Destroyed.")
    }

    // ── Permission ──────────────────────────────────────────────────────

    /**
     * Requests Shizuku permission.
     *
     * If permission is already granted, the [callback] fires immediately
     * with `true`. Otherwise the Shizuku permission dialog is shown.
     *
     * @param callback Receives the permission result. Pass `null` to trigger
     *   the dialog and observe via [addStatusListener].
     */
    @JvmStatic
    public fun requestPermission(callback: OnPermissionResultListener?) {
        check(initialized) { "Call ShizukuEasy.init() before requesting permission." }
        permissionManager.requestPermission(callback)
    }

    /** Requests Shizuku permission without a callback. */
    @JvmStatic
    public fun requestPermission() {
        requestPermission(null)
    }

    // ── Ready callback ──────────────────────────────────────────────────

    /**
     * Registers a callback that fires when Shizuku becomes ready.
     *
     * If already ready, the callback fires immediately. Otherwise it fires
     * the first time [isReady] becomes `true`. The callback is automatically
     * removed after it fires.
     *
     * @param callback Called when Shizuku is ready to use.
     */
    @JvmStatic
    public fun onReady(callback: Runnable) {
        if (isReady) {
            callback.run()
            return
        }
        readyCallbacks.add { callback.run() }
    }

    // ── Status observation ──────────────────────────────────────────────

    /**
     * Adds a listener that is notified when [status] changes.
     *
     * @param listener The listener to add.
     */
    @JvmStatic
    public fun addStatusListener(listener: OnStatusChangeListener) {
        statusListeners.add(listener)
    }

    /**
     * Removes a previously added status listener.
     *
     * @param listener The listener to remove.
     */
    @JvmStatic
    public fun removeStatusListener(listener: OnStatusChangeListener) {
        statusListeners.remove(listener)
    }

    // ── High-level capabilities ─────────────────────────────────────────

    /** Package management operations (install checks, enable/disable, etc.). */
    @JvmStatic
    public val packages: PackageCapability = PackageCapability { status }

    /** User management operations. */
    @JvmStatic
    public val users: UserCapability = UserCapability { status }

    /** Activity management operations (force stop, etc.). */
    @JvmStatic
    public val activities: ActivityCapability = ActivityCapability { status }

    /** Power management operations (reboot, shutdown). */
    @JvmStatic
    public val power: PowerCapability = PowerCapability { status }

    /** Shell command execution. */
    @JvmStatic
    public val shell: ShellCapability = ShellCapability { status }

    // ── Advanced API ────────────────────────────────────────────────────

    /**
     * Advanced Shizuku API for experienced developers.
     *
     * Provides raw binder access, system service resolution, and UserService
     * management. Most developers should use [packages], [shell], etc. instead.
     */
    @JvmStatic
    public val advanced: AdvancedShizukuApi = AdvancedShizukuApi { status }

    // ── Backward compatibility (deprecated) ─────────────────────────────

    /** @deprecated Use [isAvailable] instead. */
    @Deprecated("Use isAvailable instead.", replaceWith = ReplaceWith("isAvailable"))
    @JvmStatic
    public val available: Boolean get() = isAvailable

    /** @deprecated Use [isReady] instead. */
    @Deprecated("Use isReady instead.", replaceWith = ReplaceWith("isReady"))
    @JvmStatic
    public val ready: Boolean get() = isReady

    /** @deprecated Use [isAuthorized] instead. */
    @Deprecated("Use isAuthorized instead.", replaceWith = ReplaceWith("isAuthorized"))
    @JvmStatic
    public val permissionGranted: Boolean get() = isAuthorized

    /** @deprecated Use [advanced.getSystemService] instead. */
    @Deprecated(
        "Use ShizukuEasy.advanced.getSystemService() instead.",
        replaceWith = ReplaceWith("advanced.getSystemService(serviceName, converter)")
    )
    @JvmStatic
    public fun <T> getSystemService(serviceName: String, converter: (IBinder) -> T): T {
        return advanced.getSystemService(serviceName, converter)
    }

    // ── Internal callbacks ──────────────────────────────────────────────

    private fun onConnectionStateChanged(newState: ConnectionState) {
        Log.d(TAG, "Connection state: $newState")
        currentConnectionState = newState

        when (newState) {
            ConnectionState.CONNECTED -> {
                currentBackend = detectBackend()
                // Re-evaluate permission now that we're connected
                currentPermissionState = permissionManager.permissionState
            }
            ConnectionState.DEAD -> {
                currentBackend = ShizukuBackend.UNKNOWN
            }
            ConnectionState.DISCONNECTED -> {
                currentBackend = ShizukuBackend.UNKNOWN
                currentPermissionState = PermissionState.UNKNOWN
            }
            ConnectionState.NOT_INITIALIZED -> {
                // Should not happen via callback
            }
        }

        notifyStatusChanged()
    }

    private fun onPermissionStateChanged(newState: PermissionState) {
        Log.d(TAG, "Permission state: $newState")
        currentPermissionState = newState
        notifyStatusChanged()
    }

    private fun detectBackend(): ShizukuBackend {
        return try {
            ShizukuBackend.fromUid(Shizuku.getUid())
        } catch (_: Exception) {
            ShizukuBackend.UNKNOWN
        }
    }

    private fun notifyStatusChanged() {
        val currentStatus = status
        statusListeners.forEach { it.onStatusChanged(currentStatus) }

        // Fire and remove ready callbacks
        if (currentStatus.isReady && readyCallbacks.isNotEmpty()) {
            val callbacks = readyCallbacks.toList()
            readyCallbacks.clear()
            callbacks.forEach { it.invoke() }
        }
    }
}
