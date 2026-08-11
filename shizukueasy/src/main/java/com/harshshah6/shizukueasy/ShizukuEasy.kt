package com.harshshah6.shizukueasy

import android.content.Context
import android.os.IBinder
import android.util.Log
import com.harshshah6.shizukueasy.internal.ConnectionManager
import com.harshshah6.shizukueasy.internal.PermissionManager
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-level, developer-friendly wrapper around the Shizuku API.
 *
 * ShizukuEasy removes the boilerplate of Shizuku setup by managing binder
 * connections, permission handling, and state tracking internally.
 *
 * ## Quick Start (Kotlin)
 * ```kotlin
 * // In your Activity or Application
 * ShizukuEasy.init(this)
 *
 * if (ShizukuEasy.ready) {
 *     // Shizuku is connected and permitted — use it
 * }
 *
 * // When done
 * ShizukuEasy.destroy()
 * ```
 *
 * ## Quick Start (Java)
 * ```java
 * ShizukuEasy.init(this);
 *
 * if (ShizukuEasy.isReady()) {
 *     // Shizuku is connected and permitted
 * }
 *
 * ShizukuEasy.destroy();
 * ```
 *
 * ## Prerequisites
 * Your app must include the Shizuku provider in its `AndroidManifest.xml`:
 * ```xml
 * <provider
 *     android:name="rikka.shizuku.ShizukuProvider"
 *     android:authorities="${applicationId}.shizuku"
 *     android:multiprocess="false"
 *     android:enabled="true"
 *     android:exported="true"
 *     android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
 * ```
 *
 * @see ShizukuState
 * @see ShizukuBackend
 * @see ShizukuServiceFactory
 */
object ShizukuEasy {

    private const val TAG = "ShizukuEasy"

    private var initialized = false
    private val stateListeners = CopyOnWriteArrayList<OnStateChangeListener>()

    @Volatile
    private var currentState: ShizukuState = ShizukuState.NOT_INITIALIZED

    @Volatile
    private var currentBackend: ShizukuBackend = ShizukuBackend.UNKNOWN

    private lateinit var connectionManager: ConnectionManager
    private lateinit var permissionManager: PermissionManager

    // ── Public properties ───────────────────────────────────────────────

    /**
     * The current Shizuku state.
     *
     * Observe changes with [addStateChangeListener].
     */
    @JvmStatic
    val state: ShizukuState
        get() = currentState

    /**
     * `true` if the Shizuku binder is alive and reachable.
     */
    @JvmStatic
    val available: Boolean
        get() = initialized && connectionManager.isBinderAlive

    /**
     * `true` if Shizuku permission has been granted.
     */
    @JvmStatic
    val permissionGranted: Boolean
        get() = initialized && permissionManager.isGranted

    /**
     * `true` if Shizuku is [available] and [permissionGranted].
     *
     * When this is `true`, you can safely call Shizuku APIs and
     * [ShizukuServiceFactory.getSystemService].
     */
    @JvmStatic
    val ready: Boolean
        get() = available && permissionGranted

    /**
     * The detected backend type of the running Shizuku server.
     *
     * Only meaningful when [available] is `true`.
     */
    @JvmStatic
    val backend: ShizukuBackend
        get() = currentBackend

    /**
     * `true` if the Shizuku server is running as root (UID 0).
     */
    @JvmStatic
    val isRoot: Boolean
        get() = currentBackend == ShizukuBackend.ROOT

    /**
     * `true` if the Shizuku server is running as shell/ADB (UID 2000).
     */
    @JvmStatic
    val isShell: Boolean
        get() = currentBackend == ShizukuBackend.ADB

    /**
     * The version of the connected Shizuku server, or -1 if unavailable.
     */
    @JvmStatic
    val serverVersion: Int
        get() = try {
            if (available) Shizuku.getVersion() else -1
        } catch (_: Exception) {
            -1
        }

    // ── Lifecycle ───────────────────────────────────────────────────────

    /**
     * Initializes ShizukuEasy.
     *
     * Call this once from your `Activity.onCreate()` or `Application.onCreate()`.
     * Subsequent calls are safe and will be ignored.
     *
     * This registers binder and permission listeners. The [state] will update
     * automatically as Shizuku connects, disconnects, or permission changes.
     *
     * @param context Any context (application context is extracted internally).
     */
    @JvmStatic
    fun init(context: Context) {
        if (initialized) {
            Log.d(TAG, "Already initialized, ignoring duplicate init() call.")
            return
        }

        // We only need the context to ensure we're initialized; Shizuku uses
        // its own ContentProvider for the actual connection.
        @Suppress("UNUSED_VARIABLE")
        val appContext = context.applicationContext

        connectionManager = ConnectionManager(
            onBinderReceived = ::onBinderReceived,
            onBinderDead = ::onBinderDead
        )

        permissionManager = PermissionManager(
            onPermissionChanged = ::onPermissionChanged
        )

        initialized = true
        connectionManager.start()
        permissionManager.start()

        Log.d(TAG, "Initialized.")
    }

    /**
     * Tears down ShizukuEasy and releases all listeners.
     *
     * Call this from `Activity.onDestroy()` or when you no longer need Shizuku.
     * After calling this, [state] returns to [ShizukuState.NOT_INITIALIZED].
     */
    @JvmStatic
    fun destroy() {
        if (!initialized) return

        connectionManager.stop()
        permissionManager.stop()
        stateListeners.clear()

        initialized = false
        currentBackend = ShizukuBackend.UNKNOWN
        updateState(ShizukuState.NOT_INITIALIZED)

        Log.d(TAG, "Destroyed.")
    }

    // ── Permission ──────────────────────────────────────────────────────

    /**
     * Requests Shizuku permission.
     *
     * If permission is already granted, the [callback] fires immediately
     * with `true`. Otherwise the Shizuku permission dialog is shown.
     *
     * @param callback Receives the permission result. May be `null` if you
     *   only want to trigger the dialog and observe via [addStateChangeListener].
     */
    @JvmStatic
    fun requestPermission(callback: OnPermissionResultListener?) {
        check(initialized) { "Call ShizukuEasy.init() before requesting permission." }
        permissionManager.requestPermission(callback)
    }

    /**
     * Requests Shizuku permission without a callback.
     *
     * Observe the result through [addStateChangeListener] or by checking
     * [permissionGranted] after the dialog is dismissed.
     */
    @JvmStatic
    fun requestPermission() {
        requestPermission(null)
    }

    /**
     * Whether the user has permanently denied Shizuku permission.
     *
     * If `true`, the user must manually grant permission from the Shizuku app.
     */
    @JvmStatic
    val permissionDeniedForever: Boolean
        get() = initialized && permissionManager.shouldShowRationale

    // ── State observation ───────────────────────────────────────────────

    /**
     * Adds a listener that is notified when [state] changes.
     *
     * The listener is called on the thread where the state change occurs
     * (typically the main thread).
     *
     * @param listener The listener to add.
     */
    @JvmStatic
    fun addStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.add(listener)
    }

    /**
     * Removes a previously added state change listener.
     *
     * @param listener The listener to remove.
     */
    @JvmStatic
    fun removeStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.remove(listener)
    }

    // ── System services ─────────────────────────────────────────────────

    /**
     * Obtains a system service interface through Shizuku.
     *
     * Convenience delegate to [ShizukuServiceFactory.getSystemService].
     *
     * @param T The AIDL interface type.
     * @param serviceName The system service name (e.g., "package", "activity").
     * @param converter Converts the wrapped [IBinder] to the desired interface.
     * @return The service interface proxy.
     * @throws IllegalStateException if [ready] is false.
     */
    @JvmStatic
    fun <T> getSystemService(serviceName: String, converter: (IBinder) -> T): T {
        return ShizukuServiceFactory.getSystemService(serviceName, converter)
    }

    // ── Internal callbacks ──────────────────────────────────────────────

    private fun onBinderReceived() {
        Log.d(TAG, "Binder received.")
        currentBackend = detectBackend()

        if (permissionManager.isGranted) {
            updateState(ShizukuState.READY)
        } else {
            updateState(ShizukuState.UNAUTHORIZED)
        }
    }

    private fun onBinderDead() {
        Log.d(TAG, "Binder dead.")
        currentBackend = ShizukuBackend.UNKNOWN
        updateState(ShizukuState.DEAD)
    }

    private fun onPermissionChanged(granted: Boolean) {
        Log.d(TAG, "Permission changed: granted=$granted")
        if (connectionManager.isBinderAlive) {
            updateState(if (granted) ShizukuState.READY else ShizukuState.UNAUTHORIZED)
        }
    }

    private fun detectBackend(): ShizukuBackend {
        return try {
            ShizukuBackend.fromUid(Shizuku.getUid())
        } catch (_: Exception) {
            ShizukuBackend.UNKNOWN
        }
    }

    private fun updateState(newState: ShizukuState) {
        if (currentState == newState) return
        currentState = newState
        stateListeners.forEach { it.onStateChanged(newState) }
    }
}
