package com.example.gamepadcontroller

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private var selectedGame = "Fortnite"

    private val games = arrayOf(
        "Fortnite",
        "PUBG",
        "Minecraft",
        "Roblox",
        "Genshin Impact",
        "Apex Legends",
        "CS2",
        "Valorant",
        "League of Legends",
        "Dota 2",
        "Overwatch",
        "FF14",
        "WoW"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.widget.Button>(R.id.connectBtn).setOnClickListener {
            showGuide()
        }

        findViewById<android.widget.Button>(R.id.gameSelectBtn).setOnClickListener {
            showGamePicker()
        }

        updateStatus("🎮 Controller ready")
    }

    private fun showGuide() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📖 How to use")
            .setMessage(
                "1. Pair your Bluetooth gamepad via Settings\n" +
                "2. Open your mobile game\n" +
                "3. Choose the game profile\n" +
                "4. Press buttons on the controller\n\n" +
                "This app listens to the physical gamepad and maps it to the selected game."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showGamePicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Choose game")
            .setItems(games) { _, which ->
                selectedGame = games[which]
                findViewById<android.widget.TextView>(R.id.gameNameText).text = "Game: $selectedGame"
                updateStatus("Selected: $selectedGame")
            }
            .show()
    }

    private fun updateStatus(message: String) {
        findViewById<android.widget.TextView>(R.id.statusText).text = message
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val button = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "A"
            KeyEvent.KEYCODE_BUTTON_B -> "B"
            KeyEvent.KEYCODE_BUTTON_X -> "X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_BUTTON_START -> "START"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
            else -> null
        }

        if (button != null) {
            updateStatus("🎮 $button pressed -> $selectedGame")
            Log.d("GamepadController", "Button $button for $selectedGame")
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val button = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "A"
            KeyEvent.KEYCODE_BUTTON_B -> "B"
            KeyEvent.KEYCODE_BUTTON_X -> "X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_BUTTON_START -> "START"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
            else -> null
        }

        if (button != null) {
            updateStatus("🎮 $button released")
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val leftX = event.getAxisValue(MotionEvent.AXIS_X)
        val leftY = event.getAxisValue(MotionEvent.AXIS_Y)
        val rightX = event.getAxisValue(MotionEvent.AXIS_Z)
        val rightY = event.getAxisValue(MotionEvent.AXIS_RZ)

        if (Math.abs(leftX) > 0.2f || Math.abs(leftY) > 0.2f) {
            updateStatus("🎮 Left stick: x=${String.format("%.2f", leftX)} y=${String.format("%.2f", leftY)}")
        }
        if (Math.abs(rightX) > 0.2f || Math.abs(rightY) > 0.2f) {
            updateStatus("🎮 Right stick: x=${String.format("%.2f", rightX)} y=${String.format("%.2f", rightY)}")
        }

        return super.onGenericMotionEvent(event)
    }
}
