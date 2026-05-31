package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PasswordEntry

data class TemplateChipData(
    val label: String,
    val value: String,
    val isCustom: Boolean = false
)

private val templates = listOf(
    TemplateChipData("Email", "E1 - "),
    TemplateChipData("Facebook", "Fb1 - "),
    TemplateChipData("Instagram", "Insta 1 - "),
    TemplateChipData("X", "tw1 - "),
    TemplateChipData("WhatsApp", "wa1 - "),
    TemplateChipData("Telegram", "tg1 - "),
    TemplateChipData("LinkedIn", "in1 - "),
    TemplateChipData("Google", "g1 - "),
    TemplateChipData("Microsoft", "ms1 - "),
    TemplateChipData("GitHub", "git1 - "),
    TemplateChipData("Banking", "bank1 - "),
    TemplateChipData("Amazon", "amz1 - "),
    TemplateChipData("PayPal", "pay1 - "),
    TemplateChipData("Custom", "", isCustom = true)
)

@Composable
fun AddEditAccountDialog(
    entryToEdit: PasswordEntry? = null,
    onDismiss: () -> Unit,
    onSave: (platform: String, username: String, passwordRaw: String, notes: String) -> Unit
) {
    var platformName by remember { mutableStateOf(entryToEdit?.platformName ?: "") }
    var username by remember { mutableStateOf(entryToEdit?.emailUsername ?: "") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(entryToEdit?.notes ?: "") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Sub-dialog custom prefix state
    var showCustomPrefixDialog by remember { mutableStateOf(false) }
    var customPrefixInput by remember { mutableStateOf("") }

    // Track errors for field validation
    var platformError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (entryToEdit == null) "Add New Account" else "Edit Account Details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick templates section title & chips
                Text(
                    text = "Quick-Add Templates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        AssistChip(
                            onClick = {
                                if (template.isCustom) {
                                    customPrefixInput = ""
                                    showCustomPrefixDialog = true
                                } else {
                                    platformName = template.value
                                    platformError = false
                                }
                            },
                            label = { Text(template.label) },
                            leadingIcon = if (template.isCustom) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Custom Prefix Icon",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("chip_${template.label.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Platform Name
                OutlinedTextField(
                    value = platformName,
                    onValueChange = {
                        platformName = it
                        platformError = false
                    },
                    label = { Text("Platform Name (e.g. GitHub, Netflix)") },
                    isError = platformError,
                    supportingText = {
                        if (platformError) Text("Platform name is required", color = MaterialTheme.colorScheme.error)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_platform")
                )

                // Username / Email
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = false
                    },
                    label = { Text("Username or Email") },
                    isError = usernameError,
                    supportingText = {
                        if (usernameError) Text("Username/Email is required", color = MaterialTheme.colorScheme.error)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_username")
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("Password") },
                    isError = passwordError,
                    supportingText = {
                        if (passwordError) {
                            Text("Password cannot be empty", color = MaterialTheme.colorScheme.error)
                        } else if (entryToEdit != null) {
                            Text("Leave empty to keep existing password, or enter a new one")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                if (password.isNotEmpty()) {
                    val hasLength = password.length >= 8
                    val hasUpper = password.any { it.isUpperCase() }
                    val hasDigit = password.any { it.isDigit() }
                    val hasSpecial = password.any { !it.isLetterOrDigit() }
                    
                    var score = 0
                    if (hasLength) score++
                    if (hasUpper) score++
                    if (hasDigit) score++
                    if (hasSpecial) score++
                    
                    val strengthLevel = maxOf(1, score)
                    val (progress, progressColor, label) = when (strengthLevel) {
                        1 -> Triple(0.25f, Color(0xFFD32F2F), "Weak")
                        2 -> Triple(0.50f, Color(0xFFF57C00), "Fair")
                        3 -> Triple(0.75f, Color(0xFFFBC02D), "Good")
                        4 -> Triple(1.00f, Color(0xFF388E3C), "Strong")
                        else -> Triple(0.25f, Color(0xFFD32F2F), "Weak")
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_strength_indicator"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Password Strength: $label",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.2f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isPlatformInvalid = platformName.isBlank()
                    val isUsernameInvalid = username.isBlank()
                    val isPasswordInvalid = password.isBlank() && entryToEdit == null // password can only be empty if editing
                    
                    if (isPlatformInvalid) platformError = true
                    if (isUsernameInvalid) usernameError = true
                    if (isPasswordInvalid) passwordError = true

                    if (!isPlatformInvalid && !isUsernameInvalid && !isPasswordInvalid) {
                        onSave(platformName, username, password, notes)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_save_account")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_account")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showCustomPrefixDialog) {
        AlertDialog(
            onDismissRequest = { showCustomPrefixDialog = false },
            title = { Text("Custom Portal Prefix") },
            text = {
                Column {
                    Text("Enter a unique short name/prefix for this portal:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPrefixInput,
                        onValueChange = { customPrefixInput = it },
                        placeholder = { Text("e.g. myportal") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_prefix")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPrefixInput.isNotBlank()) {
                            platformName = "${customPrefixInput.trim()}1 - "
                            platformError = false
                        }
                        showCustomPrefixDialog = false
                    },
                    modifier = Modifier.testTag("btn_save_custom_prefix")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomPrefixDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
