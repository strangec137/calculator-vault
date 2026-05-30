package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PasswordEntry
import com.example.data.PasswordRepository
import com.example.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PasswordRepository(db.passwordDao())

    // SharedPreferences setup
    private val prefs = application.getSharedPreferences("calculator_vault_prefs", Context.MODE_PRIVATE)
    private val pinKey = "vault_secret_pin"
    private val defaultPin = "0.000"
    private val themeKey = "vault_app_theme"
    private val vibrationKey = "is_vibration_enabled"

    // Master secret PIN state
    val secretPin = MutableStateFlow(prefs.getString(pinKey, defaultPin) ?: defaultPin)

    // Persistent application theme state
    val currentTheme = MutableStateFlow(prefs.getString(themeKey, "Default") ?: "Default")

    // Vibration/Haptic feedback state
    val isVibrationEnabled = MutableStateFlow(prefs.getBoolean(vibrationKey, false))

    // Navigation trigger event to calculator (for auto-lock)
    private val _navigateToCalculator = MutableSharedFlow<Unit>()
    val navigateToCalculator: SharedFlow<Unit> = _navigateToCalculator.asSharedFlow()

    private var autoLockJob: Job? = null

    fun resetAutoLock() {
        autoLockJob?.cancel()
        autoLockJob = viewModelScope.launch {
            delay(5 * 60 * 1000L) // 5 minutes inactivity
            _navigateToCalculator.emit(Unit)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        prefs.edit().putBoolean(vibrationKey, enabled).apply()
        isVibrationEnabled.value = enabled
    }

    private fun triggerVibration() {
        if (isVibrationEnabled.value) {
            try {
                val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(30)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTheme(newTheme: String) {
        prefs.edit().putString(themeKey, newTheme).apply()
        currentTheme.value = newTheme
    }

    // Reactive list of saved accounts
    val allAccounts: StateFlow<List<PasswordEntry>> = repository.allPasswords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Navigation trigger event to secret screen
    private val _navigateToVault = MutableSharedFlow<Unit>()
    val navigateToVault: SharedFlow<Unit> = _navigateToVault.asSharedFlow()

    // Calculator engine states
    val displayText = MutableStateFlow("0")
    val expressionPreview = MutableStateFlow("")

    /**
     * Set/update secret PIN
     */
    fun updatePin(newPin: String): Boolean {
        if (newPin.isBlank() || newPin.length < 3) return false
        val oldPin = secretPin.value
        val list = allAccounts.value
        
        viewModelScope.launch {
            try {
                for (entry in list) {
                    val decrypted = SecurityUtils.decrypt(entry.passwordEncrypted, oldPin)
                    val reEncrypted = SecurityUtils.encrypt(decrypted, newPin)
                    val updatedEntry = entry.copy(passwordEncrypted = reEncrypted)
                    repository.update(updatedEntry)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            prefs.edit().putString(pinKey, newPin).apply()
            secretPin.value = newPin
        }
        return true
    }

    /**
     * Helper to evaluate raw mathematical expression strings with proper operator precendence.
     */
    fun evaluateExpression(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code) || eat('×'.code)) x *= parseFactor()
                    else if (eat('/'.code) || eat('÷'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    } else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, this.pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected character: " + ch.toChar())
                }

                while (eat('%'.code)) {
                    x /= 100.0
                }

                return x
            }
        }.parse()
    }

    /**
     * Calculator logic parser supporting full sequential formulas
     */
    fun onCalculatorKeyPress(key: String) {
        triggerVibration()
        val current = displayText.value
        when (key) {
            "AC" -> {
                displayText.value = "0"
                expressionPreview.value = ""
            }
            "⌫" -> {
                if (current != "0") {
                    if (current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" * ") || current.endsWith(" / ")) {
                        displayText.value = current.dropLast(3)
                    } else {
                        displayText.value = current.dropLast(1)
                    }
                    if (displayText.value.isEmpty()) {
                        displayText.value = "0"
                    }
                }
                checkPinMatch()
            }
            "+", "-", "*", "/" -> {
                val operatorSymbol = when (key) {
                    "*" -> " * "
                    "/" -> " / "
                    "+" -> " + "
                    "-" -> " - "
                    else -> " $key "
                }
                
                if (current == "0") {
                    if (key == "-") {
                        displayText.value = "-"
                    } else {
                        displayText.value = "0$operatorSymbol"
                    }
                } else {
                    if (current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" * ") || current.endsWith(" / ")) {
                        displayText.value = current.dropLast(3) + operatorSymbol
                    } else {
                        displayText.value = current + operatorSymbol
                    }
                }
                checkPinMatch()
            }
            "%" -> {
                if (current != "0") {
                    displayText.value = current + "%"
                }
                checkPinMatch()
            }
            "()" -> {
                val openCount = current.count { it == '(' }
                val closeCount = current.count { it == ')' }
                if (openCount > closeCount) {
                    if (current.endsWith("(") || current.endsWith(" + ") || current.endsWith(" - ") || current.endsWith(" * ") || current.endsWith(" / ")) {
                        displayText.value = if (current == "0") "(" else current + "("
                    } else {
                        displayText.value = current + ")"
                    }
                } else {
                    displayText.value = if (current == "0") "(" else current + "("
                }
                checkPinMatch()
            }
            "." -> {
                val lastToken = current.split(" ").lastOrNull() ?: ""
                if (!lastToken.contains(".")) {
                    displayText.value = current + "."
                }
                checkPinMatch()
            }
            "+/-" -> {
                if (current != "0") {
                    if (current.startsWith("-")) {
                        displayText.value = current.substring(1)
                    } else {
                        displayText.value = "-$current"
                    }
                }
                checkPinMatch()
            }
            "=" -> {
                try {
                    val result = evaluateExpression(current)
                    if (result.isNaN() || result.isInfinite()) {
                        displayText.value = "Error"
                    } else {
                        expressionPreview.value = "$current ="
                        displayText.value = formatDouble(result)
                    }
                } catch (e: Exception) {
                    displayText.value = "Error"
                }
                checkPinMatch()
            }
            else -> {
                if (current == "0" || current == "Error") {
                    displayText.value = key
                } else {
                    displayText.value = current + key
                }
                checkPinMatch()
            }
        }
    }

    /**
     * Checks if current input perfectly matches secret PIN to unlock the Vault
     */
    private fun checkPinMatch() {
        if (displayText.value == secretPin.value) {
            unlockDirectly()
        }
    }

    /**
     * Direct unlock to transition screen and reset state
     */
    fun unlockDirectly() {
        viewModelScope.launch {
            _navigateToVault.emit(Unit)
            // Clear state slightly to avoid immediate loops on back button
            displayText.value = "0"
            expressionPreview.value = ""
            resetAutoLock()
        }
    }

    private fun formatDouble(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    /**
     * Helper to decrypt stored password using current PIN
     */
    fun decryptPassword(encryptedText: String): String {
        return SecurityUtils.decrypt(encryptedText, secretPin.value)
    }

    /**
     * Room inserts/deletes
     */
    fun addAccount(platform: String, email: String, passwordRaw: String, notes: String) {
        viewModelScope.launch {
            try {
                val encryptedPw = SecurityUtils.encrypt(passwordRaw, secretPin.value)
                val entry = PasswordEntry(
                    platformName = platform,
                    emailUsername = email,
                    passwordEncrypted = encryptedPw,
                    notes = notes
                )
                repository.insert(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAccount(id: Int, platform: String, email: String, passwordRaw: String, notes: String) {
        viewModelScope.launch {
            try {
                val encryptedPw = SecurityUtils.encrypt(passwordRaw, secretPin.value)
                val entry = PasswordEntry(
                    id = id,
                    platformName = platform,
                    emailUsername = email,
                    passwordEncrypted = encryptedPw,
                    notes = notes
                )
                repository.update(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAccountById(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteById(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * File Export Functionality
     */
    fun shareTextExport(context: Context) {
        val list = allAccounts.value
        if (list.isEmpty()) return

        val builder = java.lang.StringBuilder()
        builder.append("=== SECURE PASSWORD MANAGER EXPORT ===\n")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        builder.append("Exported on: $timestamp\n")
        builder.append("--------------------------------------------------\n\n")

        for (entry in list) {
            val decryptedPassword = decryptPassword(entry.passwordEncrypted)
            builder.append("Platform: ${entry.platformName}\n")
            builder.append("Username/Email: ${entry.emailUsername}\n")
            builder.append("Password: $decryptedPassword\n")
            if (entry.notes.isNotBlank()) {
                builder.append("Notes: ${entry.notes}\n")
            }
            builder.append("--------------------------------------------------\n")
        }

        try {
            val folder = File(context.cacheDir, "exports")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "Password_Vault_Export.txt")
            file.writeText(builder.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.calculator.pwv.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Secure Password Vault TXT Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = "Share Vault TXT Export"
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareCsvExport(context: Context) {
        val list = allAccounts.value
        if (list.isEmpty()) return

        val builder = java.lang.StringBuilder()
        builder.append("Platform,Username/Email,Password,Notes\n")

        for (entry in list) {
            val decryptedPw = decryptPassword(entry.passwordEncrypted)
            val line = listOf(entry.platformName, entry.emailUsername, decryptedPw, entry.notes)
                .joinToString(",") { str -> 
                    // Escape quotes and wrap
                    val escaped = str.replace("\"", "\"\"")
                    "\"$escaped\""
                }
            builder.append(line).append("\n")
        }

        try {
            val folder = File(context.cacheDir, "exports")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "Password_Vault_Export.csv")
            file.writeText(builder.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.calculator.pwv.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Secure Password Vault CSV Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = "Share Vault CSV Export"
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
