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
import com.harshshah6.shizukueasy.OnStateChangeListener
import com.harshshah6.shizukueasy.ShizukuEasy
import com.harshshah6.shizukueasy.ShizukuState
import rikka.shizuku.Shizuku

/**
 * Demo activity showcasing ShizukuEasy usage.
 *
 * Demonstrates initialization, state observation, permission requesting,
 * and running a simple Shizuku operation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textState: TextView
    private lateinit var textAvailable: TextView
    private lateinit var textPermission: TextView
    private lateinit var textBackend: TextView
    private lateinit var textServerVersion: TextView
    private lateinit var textReady: TextView
    private lateinit var btnRequestPermission: MaterialButton
    private lateinit var btnRunTest: MaterialButton
    private lateinit var cardResult: MaterialCardView
    private lateinit var textResult: TextView

    private val stateListener = OnStateChangeListener { newState ->
        runOnUiThread { updateUi(newState) }
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

        // Initialize ShizukuEasy
        ShizukuEasy.init(this)
        ShizukuEasy.addStateChangeListener(stateListener)

        btnRequestPermission.setOnClickListener { onRequestPermissionClicked() }
        btnRunTest.setOnClickListener { onRunTestClicked() }

        // Show initial state
        updateUi(ShizukuEasy.state)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuEasy.removeStateChangeListener(stateListener)
        ShizukuEasy.destroy()
    }

    private fun bindViews() {
        textState = findViewById(R.id.text_state)
        textAvailable = findViewById(R.id.text_available)
        textPermission = findViewById(R.id.text_permission)
        textBackend = findViewById(R.id.text_backend)
        textServerVersion = findViewById(R.id.text_server_version)
        textReady = findViewById(R.id.text_ready)
        btnRequestPermission = findViewById(R.id.btn_request_permission)
        btnRunTest = findViewById(R.id.btn_run_test)
        cardResult = findViewById(R.id.card_result)
        textResult = findViewById(R.id.text_result)
    }

    private fun updateUi(state: ShizukuState) {
        textState.text = state.name
        textAvailable.text = yesNo(ShizukuEasy.available)
        textPermission.text = yesNo(ShizukuEasy.permissionGranted)
        textBackend.text = ShizukuEasy.backend.name
        textReady.text = yesNo(ShizukuEasy.ready)

        val version = ShizukuEasy.serverVersion
        textServerVersion.text = if (version >= 0) version.toString() else getString(R.string.value_unknown)

        btnRequestPermission.isEnabled = ShizukuEasy.available && !ShizukuEasy.permissionGranted
        btnRunTest.isEnabled = ShizukuEasy.ready
    }

    private fun onRequestPermissionClicked() {
        if (ShizukuEasy.permissionDeniedForever) {
            showResult(getString(R.string.msg_permission_denied_forever))
            return
        }

        ShizukuEasy.requestPermission { granted ->
            runOnUiThread {
                updateUi(ShizukuEasy.state)
                showResult(if (granted) "Permission granted!" else "Permission denied.")
            }
        }
    }

    private fun onRunTestClicked() {
        try {
            val version = Shizuku.getVersion()
            val uid = Shizuku.getUid()
            val backend = ShizukuEasy.backend.name
            showResult(getString(R.string.msg_test_success, version, uid, backend))
        } catch (e: Exception) {
            showResult(getString(R.string.msg_test_error, e.message ?: e.javaClass.simpleName))
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