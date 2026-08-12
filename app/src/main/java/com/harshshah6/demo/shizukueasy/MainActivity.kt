package com.harshshah6.demo.shizukueasy

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.harshshah6.shizukueasy.OnStatusChangeListener
import com.harshshah6.shizukueasy.ShizukuEasy
import com.harshshah6.shizukueasy.ShizukuStatus

/**
 * Demo activity showcasing the ShizukuEasy high-level API.
 *
 * Demonstrates:
 * - Initialization and status observation
 * - Permission requesting
 * - High-level package capability
 * - Clean error handling with ShizukuResult
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textConnectionState: TextView
    private lateinit var textPermissionState: TextView
    private lateinit var textBackend: TextView
    private lateinit var textServerVersion: TextView
    private lateinit var textReady: TextView
    private lateinit var btnRequestPermission: MaterialButton
    private lateinit var btnListPackages: MaterialButton
    private lateinit var btnForceStop: MaterialButton
    private lateinit var cardResult: MaterialCardView
    private lateinit var textResult: TextView

    private val statusListener = OnStatusChangeListener { newStatus ->
        runOnUiThread { updateUi(newStatus) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()

        // Initialize ShizukuEasy — one call, that's it
        ShizukuEasy.init(this)
        ShizukuEasy.addStatusListener(statusListener)

        btnRequestPermission.setOnClickListener { onRequestPermissionClicked() }
        btnListPackages.setOnClickListener { onListPackagesClicked() }
        btnForceStop.setOnClickListener { onForceStopClicked() }

        // React to readiness
        ShizukuEasy.onReady(Runnable {
            runOnUiThread {
                showResult("Shizuku is ready! Backend: ${ShizukuEasy.backend.name}")
            }
        })

        updateUi(ShizukuEasy.status)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuEasy.removeStatusListener(statusListener)
        ShizukuEasy.destroy()
    }

    private fun bindViews() {
        textConnectionState = findViewById(R.id.text_connection_state)
        textPermissionState = findViewById(R.id.text_permission_state)
        textBackend = findViewById(R.id.text_backend)
        textServerVersion = findViewById(R.id.text_server_version)
        textReady = findViewById(R.id.text_ready)
        btnRequestPermission = findViewById(R.id.btn_request_permission)
        btnListPackages = findViewById(R.id.btn_list_packages)
        btnForceStop = findViewById(R.id.btn_force_stop)
        cardResult = findViewById(R.id.card_result)
        textResult = findViewById(R.id.text_result)
    }

    private fun updateUi(status: ShizukuStatus) {
        textConnectionState.text = status.connection.name
        textPermissionState.text = status.permission.name
        textBackend.text = status.backend.name

        val version = ShizukuEasy.serverVersion
        textServerVersion.text = if (version >= 0) version.toString()
            else getString(R.string.value_unknown)

        textReady.text = yesNo(status.isReady)

        btnRequestPermission.isEnabled = status.isAvailable && !status.isAuthorized
        btnListPackages.isEnabled = status.isReady
        btnForceStop.isEnabled = status.isReady
    }

    private fun onRequestPermissionClicked() {
        ShizukuEasy.requestPermission { granted ->
            runOnUiThread {
                updateUi(ShizukuEasy.status)
                showResult(if (granted) "Permission granted!" else "Permission denied.")
            }
        }
    }

    /**
     * Demonstrates the high-level package capability.
     * No IPackageManager, no binder, no SystemServiceHelper.
     */
    private fun onListPackagesClicked() {
        ShizukuEasy.packages.getInstalled()
            .onSuccess { packages ->
                val summary = buildString {
                    appendLine("${packages.size} packages installed")
                    appendLine()
                    packages.take(10).forEach { pkg ->
                        appendLine("• $pkg")
                    }
                    if (packages.size > 10) {
                        appendLine("… and ${packages.size - 10} more")
                    }
                }
                runOnUiThread { showResult(summary) }
            }
            .onFailure { error ->
                runOnUiThread { showResult("Error: ${error.message}") }
            }
    }

    /**
     * Demonstrates the activity capability — force stop the demo's own package.
     */
    private fun onForceStopClicked() {
        val target = "com.android.calculator2" // safe demo target
        ShizukuEasy.activities.forceStop(target)
            .onSuccess {
                runOnUiThread { showResult("Force-stopped: $target") }
            }
            .onFailure { error ->
                runOnUiThread { showResult("Error: ${error.message}") }
            }
    }

    private fun showResult(message: String) {
        cardResult.visibility = View.VISIBLE
        textResult.text = message
    }

    private fun yesNo(value: Boolean): String {
        return getString(if (value) R.string.value_yes else R.string.value_no)
    }
}