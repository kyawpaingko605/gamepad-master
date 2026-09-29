package com.example.gamepadmaster

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.example.gamepadmaster.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val executor = Executors.newSingleThreadExecutor()
    
    private var isConnected = false
    private var outputStream: OutputStream? = null
    private var socket: Socket? = null
    private var serverIP = "192.168.1.50"
    private val serverPort = 12345

    // Game list
    private val games = arrayOf(
        "Fortnite", "Valorant", "CS2", "Apex Legends",
        "League of Legends", "Dota 2", "PUBG", "Minecraft",
        "Roblox", "Genshin Impact", "Overwatch", "FF14", "WoW", "Default"
    )
    private var selectedGame = "Fortnite"

    companion object {
        private const val TAG = "GamepadMaster"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupButtons()
        setupGameSelection()
    }

    private fun setupUI() {
        binding.statusText.text = "Disconnected"
        binding.gameNameText.text = "Game: $selectedGame"
    }

    private fun setupGameSelection() {
        binding.gameSelectBtn?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Select Game")
                .setItems(games) { _, which ->
                    selectedGame = games[which]
                    binding.gameNameText.text = "Game: $selectedGame"
                    sendGameConfig(selectedGame)
                    Log.d(TAG, "Selected game: $selectedGame")
                }
                .show()
        }
    }

    private fun setupButtons() {
        // Connection button
        binding.connectBtn.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                showConnectionDialog()
            }
        }

        // D-Pad
        setupButtonListener(binding.btnDpadUp, "DPAD_UP")
        setupButtonListener(binding.btnDpadDown, "DPAD_DOWN")
        setupButtonListener(binding.btnDpadLeft, "DPAD_LEFT")
        setupButtonListener(binding.btnDpadRight, "DPAD_RIGHT")

        // Action Buttons
        setupButtonListener(binding.btnA, "BUTTON_A")
        setupButtonListener(binding.btnB, "BUTTON_B")
        setupButtonListener(binding.btnX, "BUTTON_X")
        setupButtonListener(binding.btnY, "BUTTON_Y")

        // Shoulder Buttons
        setupButtonListener(binding.btnL1, "BUTTON_L1")
        setupButtonListener(binding.btnL2, "BUTTON_L2")
        setupButtonListener(binding.btnR1, "BUTTON_R1")
        setupButtonListener(binding.btnR2, "BUTTON_R2")

        // Special Buttons
        setupButtonListener(binding.btnSelect, "BUTTON_SELECT")
        setupButtonListener(binding.btnStart, "BUTTON_START")
        setupButtonListener(binding.btnMenu, "BUTTON_HOME")

        // Joysticks
        setupJoystickListener(binding.joystickLeft, "JOYSTICK_LEFT")
        setupJoystickListener(binding.joystickRight, "JOYSTICK_RIGHT")

        // Settings button
        binding.settingsBtn?.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupButtonListener(button: MaterialButton, buttonCode: String) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendGamepadEvent(buttonCode, 1)
                    Log.d(TAG, "$buttonCode pressed")
                }
                MotionEvent.ACTION_UP -> {
                    sendGamepadEvent(buttonCode, 0)
                    Log.d(TAG, "$buttonCode released")
                }
            }
            true
        }
    }

    private fun setupJoystickListener(joystick: MaterialButton, joystickName: String) {
        joystick.setOnTouchListener { view, event ->
            val centerX = view.width / 2f
            val centerY = view.height / 2f
            
            var x = ((event.x - centerX) / (view.width / 2f) * 100).toInt()
            var y = ((event.y - centerY) / (view.height / 2f) * 100).toInt()
            
            // Clamp values
            x = x.coerceIn(-100, 100)
            y = y.coerceIn(-100, 100)
            
            sendJoystickEvent(joystickName, x, y)
            true
        }
    }

    private fun showConnectionDialog() {
        val input = TextInputEditText(this)
        input.hint = "192.168.x.x"
        input.setText(serverIP)

        MaterialAlertDialogBuilder(this)
            .setTitle("Connect to Server")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                serverIP = input.text.toString()
                connect(serverIP, serverPort)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setItems(arrayOf("Auto Connect", "Button Sensitivity", "Help")) { _, which ->
                when (which) {
                    0 -> toggleAutoConnect()
                    1 -> showSensitivityDialog()
                    2 -> showHelpDialog()
                }
            }
            .show()
    }

    private fun toggleAutoConnect() {
        val prefs = getSharedPreferences("gamepad", Context.MODE_PRIVATE)
        val autoConnect = prefs.getBoolean("auto_connect", false)
        prefs.edit().putBoolean("auto_connect", !autoConnect).apply()
        Log.d(TAG, "Auto connect: ${!autoConnect}")
    }

    private fun showSensitivityDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Joystick Sensitivity")
            .setItems(arrayOf("Low", "Medium", "High")) { _, which ->
                Log.d(TAG, "Sensitivity: $which")
            }
            .show()
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Help")
            .setMessage(
                "1. Start the server on your PC\n" +
                "2. Enter PC IP address\n" +
                "3. Select your game\n" +
                "4. Press Connect\n" +
                "5. Start playing!\n\n" +
                "For PC IP, run on server:\n" +
                "python advanced_gamepad_server.py"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun connect(ip: String, port: Int) {
        scope.launch {
            withContext(Dispatchers.Default) {
                try {
                    socket = Socket(ip, port)
                    outputStream = socket?.getOutputStream()
                    isConnected = true

                    runOnUiThread {
                        binding.statusText.text = "Connected to $ip"
                        binding.connectBtn.text = "Disconnect"
                        updateConnectionUI(true)
                    }
                    
                    Log.d(TAG, "Connected to $ip:$port")
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed: ${e.message}")
                    runOnUiThread {
                        binding.statusText.text = "Connection Failed: ${e.message}"
                        showErrorDialog(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    private fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
            isConnected = false
            binding.statusText.text = "Disconnected"
            binding.connectBtn.text = "Connect"
            updateConnectionUI(false)
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    private fun sendGamepadEvent(buttonCode: String, value: Int) {
        if (!isConnected) return

        scope.launch(Dispatchers.Default) {
            try {
                val json = """{"type":"button","code":"$buttonCode","value":$value}"""
                outputStream?.apply {
                    write((json + "\n").toByteArray())
                    flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
            }
        }
    }

    private fun sendJoystickEvent(joystickName: String, x: Int, y: Int) {
        if (!isConnected) return

        scope.launch(Dispatchers.Default) {
            try {
                val json = """{"type":"joystick","name":"$joystickName","x":$x,"y":$y}"""
                outputStream?.apply {
                    write((json + "\n").toByteArray())
                    flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Joystick error: ${e.message}")
            }
        }
    }

    private fun sendGameConfig(gameName: String) {
        if (!isConnected) return

        scope.launch(Dispatchers.Default) {
            try {
                val json = """{"type":"config","command":"set_game","game":"$gameName"}"""
                outputStream?.apply {
                    write((json + "\n").toByteArray())
                    flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Config error: ${e.message}")
            }
        }
    }

    private fun updateConnectionUI(connected: Boolean) {
        if (connected) {
            binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_info)
        } else {
            binding.statusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Connection Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        executor.shutdown()
        scope.cancel()
    }
}
