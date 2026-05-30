package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PasswordEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    onNavigateBack()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val accounts by viewModel.allAccounts.collectAsState()
    val focusManager = LocalFocusManager.current

    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDefault = currentTheme == "Default"
    val isLight = currentTheme == "Pastel Green Mint" || currentTheme == "Bakery Cozy"

    // Geometric Balance adaptive exact Color Palette
    val darkVioletBg = Color(0xFF0D0B1F) // Deep, sleek background color
    
    val bgColor = if (isDefault) darkVioletBg else MaterialTheme.colorScheme.background
    val cardBg = if (isDefault) Color(0x0DFFFFFF) else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val activePillBg = if (isDefault) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.primary
    val inactivePillBg = if (isDefault) Color(0x0DFFFFFF) else MaterialTheme.colorScheme.surfaceVariant
    val inactivePillBorder = if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textSlate300 = if (isDefault) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurface
    val textSlate400 = if (isDefault) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant

    // Filter and search states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Social", "Work", "Finance")

    // Sort/Filter logic inside remember
    val filteredAccounts = remember(accounts, selectedCategory, searchQuery) {
        accounts.filter { account ->
            val nameLower = account.platformName.lowercase()
            val matchSearch = nameLower.contains(searchQuery.lowercase()) ||
                    account.emailUsername.lowercase().contains(searchQuery.lowercase()) ||
                    account.notes.lowercase().contains(searchQuery.lowercase())
            
            if (!matchSearch) return@filter false
            if (selectedCategory == "All") return@filter true
            
            val isSocial = nameLower.contains("google") || nameLower.contains("facebook") || 
                           nameLower.contains("twitter") || nameLower.contains("instagram") || 
                           nameLower.contains("linkedin") || nameLower.contains("meta") || 
                           nameLower.contains("whatsapp") || nameLower.contains("reddit") || 
                           nameLower.contains("snapchat") || nameLower.contains("tiktok") || 
                           nameLower.contains("gmail") || nameLower.contains("yahoo") || 
                           nameLower.contains("outlook") || nameLower.contains("netflix") || 
                           nameLower.contains("social") || nameLower.contains("email")
                           
            val isWork = nameLower.contains("github") || nameLower.contains("gitlab") || 
                         nameLower.contains("upwork") || nameLower.contains("fiverr") || 
                         nameLower.contains("jira") || nameLower.contains("slack") || 
                         nameLower.contains("office") || nameLower.contains("microsoft") || 
                         nameLower.contains("zoom") || nameLower.contains("work")
                         
            val isFinance = nameLower.contains("binance") || nameLower.contains("bank") || 
                            nameLower.contains("crypto") || nameLower.contains("stripe") || 
                            nameLower.contains("paypal") || nameLower.contains("wallet") || 
                            nameLower.contains("card") || nameLower.contains("coin") || 
                            nameLower.contains("finance") || nameLower.contains("investment")
            
            when (selectedCategory) {
                "Social" -> isSocial
                "Work" -> isWork
                "Finance" -> isFinance
                else -> true
            }
        }
    }

    // Form Overlay Control States
    var showAddEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<PasswordEntry?>(null) }
    var decryptedPasswordForEditing by remember { mutableStateOf("") }
    
    // Settings Overlay Control States
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Delete Confirmation
    var accountToDelete by remember { mutableStateOf<PasswordEntry?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        viewModel.resetAutoLock()
                    }
                }
            },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back/Lock icon inside a beautiful circle button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable(onClick = onNavigateBack)
                            .border(BorderStroke(1.dp, if (isDefault) Color(0x0DFFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), CircleShape)
                            .testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = if (isDefault) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Text(
                        text = "Vault",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDefault) Color.White else MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                }

                // PIN indicator and settings button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Vault Settings",
                            tint = if (isDefault) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Harmonic PIN/Dialpad Action Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .testTag("btn_pin_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Manage PIN/Vault Trigger",
                            tint = if (isDefault) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Elegant modern floating square shape
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDefault) Color(0xFFD6E3FF) else MaterialTheme.colorScheme.primary)
                    .clickable {
                        accountToEdit = null
                        showAddEditDialog = true
                    }
                    .testTag("btn_add_account")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Login",
                    tint = if (isDefault) Color(0xFF002D6E) else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Search Bar (matching `#252836` or theme surface variant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search passwords...",
                            color = textSlate400,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textSlate400,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = textSlate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (isDefault) Color(0xFF252836) else MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = if (isDefault) Color(0xFF252836) else MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = if (isDefault) Color(0xFF252836) else MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = if (isDefault) Color.White else MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (isDefault) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_bar")
                )
            }

            // Category select row (with horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isActive = selectedCategory == category
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) activePillBg else inactivePillBg)
                            .clickable { selectedCategory = category }
                            .border(
                                width = if (isActive) 0.dp else 1.dp,
                                color = if (isActive) Color.Transparent else inactivePillBorder,
                                shape = RoundedCornerShape(20.dp)
                              )
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isActive) {
                                if (isDefault) Color.White else MaterialTheme.colorScheme.onPrimary
                            } else textSlate300,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Password Accounts list
            if (filteredAccounts.isEmpty()) {
                val isSearchOrFilterActive = searchQuery.isNotEmpty() || selectedCategory != "All"
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSearchOrFilterActive) Icons.Default.SearchOff else Icons.Default.VpnKey,
                        contentDescription = "Empty State Icon",
                        tint = if (isDefault) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isSearchOrFilterActive) "No matches found" else "Vault is Empty",
                        color = if (isDefault) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSearchOrFilterActive) 
                            "Try altering your search text or select a different category filter step."
                             else "All saved accounts list here. Click the bottom + button to record a secure credential.",
                        color = textSlate400,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = filteredAccounts,
                        key = { it.id }
                    ) { account ->
                        PasswordCard(
                            entry = account,
                            decryptedPassword = viewModel.decryptPassword(account.passwordEncrypted),
                            onEditClick = {
                                accountToEdit = account
                                decryptedPasswordForEditing = viewModel.decryptPassword(account.passwordEncrypted)
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                accountToDelete = account
                            },
                            backgroundColor = cardBg,
                            borderColor = cardBorder,
                            subtitleColor = textSlate400,
                            isDefault = isDefault
                        )
                    }

                    // Elegant "Stored on Device Only" footer mapping exactly to the Design HTML structure
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, if (isDefault) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), shape = RoundedCornerShape(24.dp))
                                .background(if (isDefault) Color(0x05FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "STORED ON DEVICE ONLY",
                                color = if (isDefault) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp)) // Avoid overlap with custom floating FAB
                    }
                }
            }
        }

        // Add / Edit Form Overlay
        if (showAddEditDialog) {
            AddEditAccountDialog(
                entryToEdit = accountToEdit,
                onDismiss = {
                    showAddEditDialog = false
                    accountToEdit = null
                },
                onSave = { platform, username, passwordRaw, notes ->
                    val resolvedPassword = if (passwordRaw.isEmpty() && accountToEdit != null) {
                        decryptedPasswordForEditing
                    } else {
                        passwordRaw
                    }
                    
                    if (accountToEdit == null) {
                        viewModel.addAccount(platform, username, resolvedPassword, notes)
                        Toast.makeText(context, "$platform added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateAccount(accountToEdit!!.id, platform, username, resolvedPassword, notes)
                        Toast.makeText(context, "$platform updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    showAddEditDialog = false
                    accountToEdit = null
                }
            )
        }

        // Settings Dialog Overlay (Change PIN, Export TXT, Export CSV)
        if (showSettingsDialog) {
            SettingsDialog(
                currentTheme = viewModel.currentTheme.collectAsState().value,
                onThemeChange = { newTheme -> viewModel.updateTheme(newTheme) },
                currentPin = viewModel.secretPin.collectAsState().value,
                isVibrationEnabled = viewModel.isVibrationEnabled.collectAsState().value,
                onToggleVibration = { enabled -> viewModel.toggleVibration(enabled) },
                onDismiss = { showSettingsDialog = false },
                onSavePin = { newPin ->
                    val success = viewModel.updatePin(newPin)
                    if (success) {
                        Toast.makeText(context, "PIN successfully updated to: $newPin", Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        Toast.makeText(context, "Failed: PIN must be at least 3 characters", Toast.LENGTH_SHORT).show()
                        false
                    }
                },
                onExportTxt = {
                    if (accounts.isEmpty()) {
                        Toast.makeText(context, "Nothing to export! Vault is empty.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.shareTextExport(context)
                    }
                },
                onExportCsv = {
                    if (accounts.isEmpty()) {
                        Toast.makeText(context, "Nothing to export! Vault is empty.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.shareCsvExport(context)
                    }
                }
            )
        }

        // Confirm Delete Dialog
        accountToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Credential?") },
                text = { Text("Are you sure you want to permanently delete your credentials for ${entry.platformName}? This action is irreversible.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccountById(entry.id)
                            Toast.makeText(context, "${entry.platformName} deleted", Toast.LENGTH_SHORT).show()
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun getIconForPlatform(platformName: String): @Composable () -> Unit {
    val nameLower = platformName.lowercase()
    return when {
        nameLower.startsWith("fb1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Facebook Icon",
                    tint = Color(0xFF1877F2),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("insta") -> {
            {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Instagram Icon",
                    tint = Color(0xFFE1306C),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("tw1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "X/Twitter Icon",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("wa1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "WhatsApp Icon",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("tg1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Telegram Icon",
                    tint = Color(0xFF0088CC),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("in1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "LinkedIn Icon",
                    tint = Color(0xFF0A66C2),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("e1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Mail Icon",
                    tint = Color(0xFF0088CC),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("g1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Google Icon",
                    tint = Color(0xFFEA4335),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("ms1") -> {
            {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Microsoft Icon",
                    tint = Color(0xFF00A4EF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("git1") -> {
            {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "GitHub Icon",
                    tint = Color(0xFF24292E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("bank1") -> {
            {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Banking Icon",
                    tint = Color(0xFF43A047),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("amz1") -> {
            {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Amazon Icon",
                    tint = Color(0xFFFF9900),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        nameLower.startsWith("pay1") -> {
            {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "PayPal Icon",
                    tint = Color(0xFF003087),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Default Icon",
                    tint = Color(0xFFA8C7FA),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PasswordCard(
    entry: PasswordEntry,
    decryptedPassword: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color,
    subtitleColor: Color,
    isDefault: Boolean = true
) {
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Beautiful rounded card from "Geometric Balance"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(24.dp))
            .testTag("card_${entry.platformName}")
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dynamically styled geometric category symbol box (NOT standard simple circle!)
                val avatarStyle = remember(entry.platformName) {
                    val nameLower = entry.platformName.lowercase()
                    when {
                        nameLower.contains("google") || nameLower.contains("gmail") -> {
                            Pair(Color(0x333B82F6), Color(0xFF60A5FA)) // G Blue
                        }
                        nameLower.contains("binance") || nameLower.contains("bitcoin") || nameLower.contains("crypto") -> {
                            Pair(Color(0x33F97316), Color(0xFFFB923C)) // Binance Orange
                        }
                        nameLower.contains("github") || nameLower.contains("gitlab") || nameLower.contains("work") -> {
                            Pair(Color(0x3394A3B8), Color(0xFFE2E8F0)) // Github slate
                        }
                        else -> {
                            Pair(
                                if (isDefault) Color(0x337C4DFF) else Color(0x1F006D5B),
                                if (isDefault) Color(0xFFA8C7FA) else Color(0xFF006D5B)
                            ) // Theme Teal/Violet default
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp)) // sleek geometric corner look
                        .background(avatarStyle.first)
                ) {
                    val iconComposable = getIconForPlatform(entry.platformName)
                    iconComposable()
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = entry.platformName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDefault) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.emailUsername,
                        fontSize = 13.sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Password show toggler
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.testTag("btn_toggle_pw_${entry.platformName}")
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password view",
                        tint = subtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Encryption content container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDefault) Color(0x0DFFFFFF) else MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, if (isDefault) Color(0x0FFFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPasswordVisible) decryptedPassword else "••••••••",
                        fontSize = 15.sp,
                        fontWeight = if (isPasswordVisible) FontWeight.Medium else FontWeight.Bold,
                        color = if (isDefault) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = if (isPasswordVisible) 0.5.sp else 2.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_password_${entry.platformName}")
                    )
                    
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Vault Password", decryptedPassword)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("btn_copy_pw_${entry.platformName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Password",
                            tint = subtitleColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Stored Notes
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${entry.notes}",
                    fontSize = 12.sp,
                    color = subtitleColor.copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val actionAccentColor = if (isDefault) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.primary

                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Vault Username", entry.emailUsername)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Username copied to Clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_copy_user_${entry.platformName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Copy User ID",
                        tint = actionAccentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy User", 
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = actionAccentColor
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Edit Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("btn_edit_${entry.platformName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        tint = if (isDefault) Color(0xFF60A5FA) else Color(0xFF1E88E5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("btn_delete_${entry.platformName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = if (isDefault) Color(0xFFF87171) else Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentPin: String,
    isVibrationEnabled: Boolean,
    onToggleVibration: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Boolean,
    onExportTxt: () -> Unit,
    onExportCsv: () -> Unit
) {
    var newPin by remember { mutableStateOf(currentPin) }
    val context = LocalContext.current

    val isLight = currentTheme == "Pastel Green Mint" || currentTheme == "Bakery Cozy"
    val headerColor = MaterialTheme.colorScheme.primary
    val standardTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dialogBg = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Text(
                text = "Vault Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = standardTextColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // APPEARANCE SECTION
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Application Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = standardTextColor
                    )
                    Text(
                        text = "Select from handcrafted visual profiles instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )

                    // Step 2 Theme Selector Option Buttons
                    val themes = listOf("Default", "Bakery Cozy", "Pastel Green Mint")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { themeName ->
                            val isSelected = currentTheme == themeName
                            val buttonShape = RoundedCornerShape(10.dp)
                            
                            // Unselected State vs Selected State ("System" in reference)
                            val btnBg = if (isSelected) Color(0xFFC8E6C9) else Color.White
                            val btnBorderColor = if (isSelected) Color.Transparent else Color(0xFFE0E0E0)
                            val btnTextColor = if (isSelected) Color(0xFF0D533A) else Color(0xFF555555)
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(buttonShape)
                                    .background(btnBg)
                                    .then(
                                        if (isSelected) Modifier else Modifier.border(BorderStroke(1.dp, btnBorderColor), buttonShape)
                                    )
                                    .clickable { onThemeChange(themeName) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("theme_btn_$themeName")
                            ) {
                                Text(
                                    text = themeName,
                                    color = btnTextColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Toggle Switch (light grey track with a darker grey thumb when unselected)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vibration Feedback",
                                fontSize = 14.sp,
                                color = standardTextColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tactile response on calculator tap",
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = onToggleVibration,
                            colors = SwitchDefaults.colors(
                                uncheckedTrackColor = Color(0xFFE0E0E0), // light grey track
                                uncheckedThumbColor = Color(0xFF757575), // darker grey thumb
                                checkedTrackColor = Color(0xFFB2DFDB),   // soft pastel/teal green
                                checkedThumbColor = Color(0xFF006D5B)    // dark green/teal thumb
                            ),
                            modifier = Modifier.testTag("switch_vibration")
                        )
                    }
                }

                HorizontalDivider(color = if (isLight) Color(0xFFE0E0E0) else Color(0x1AFFFFFF))

                // CURRENCY SECTION
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Standard Vault Currency",
                            fontSize = 14.sp,
                            color = standardTextColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Choose default preference symbols",
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                    Text(
                        text = "USD ($)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor
                    )
                }

                HorizontalDivider(color = if (isLight) Color(0xFFE0E0E0) else Color(0x1AFFFFFF))

                // DATA SECTION
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Unlock Sequence (PIN)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = standardTextColor
                    )
                    
                    Text(
                        text = "Type this value on the standard calculator screen to enter your vault. Default is 0.000.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { input ->
                            // Only allow characters that are on a standard calculator keyboard (digits and dot) to prevent lockout
                            newPin = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("New Trigger Sequence") },
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isLight) OutlinedTextFieldDefaults.colors(
                            focusedTextColor = standardTextColor,
                            unfocusedTextColor = standardTextColor,
                            focusedBorderColor = headerColor,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ) else OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_pin")
                    )

                    Button(
                        onClick = {
                            val saved = onSavePin(newPin)
                            if (saved) onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLight) Color(0xFF006D5B) else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("btn_save_pin")
                    ) {
                        Text("Save Trigger")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Export Options",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = standardTextColor
                    )

                    Text(
                        text = "Export decrypted credentials safely to a CSV or readable Plain TXT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onExportTxt()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF475569)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_txt")
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = "TXT")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Plain TXT", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onExportCsv()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(imageVector = Icons.Default.GridOn, contentDescription = "CSV")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel CSV", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = if (isLight) Color(0xFFE0E0E0) else Color(0x1AFFFFFF))

                // About Section Header
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                )

                // Light-grey/blue tinted surface Card with About details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLight) Color(0xFFF3F4F6) else Color(0xFFECEFF1)
                    ),
                    border = BorderStroke(1.dp, if (isLight) Color(0xFFE5E7EB) else Color(0xFFCFD8DC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // App Title renamed to "Calculator" (Bold, prominent dark text centered)
                        Text(
                            text = "Calculator",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLight) Color(0xFF0F172A) else Color(0xFF1A237E),
                            textAlign = TextAlign.Center
                        )

                        // Version Tag: Version 1.3.0 (Small, muted grey text directly underneath)
                        Text(
                            text = "Version 1.3.0",
                            fontSize = 12.sp,
                            color = Color(0xFF78909C),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Credits Line: Centered text
                        Text(
                            text = "Made with ❤️ by Akagami no Shanks 🏴‍☠️ – Bangladesh 🇧🇩",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF37474F),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Wide, blue rounded material button labeled Report a Bug / Request a Feature
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.instagram.com/akagami_no_shanks.exe?igsh=MXFvYW1hajZmenZsMg==")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open instagram link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_report_bug")
                        ) {
                            Text(
                                text = "Report a Bug / Request a Feature",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Subtext below the button: DM the captain for bug reports & feature requests 🏴‍☠️ in small, centered italicized text
                        Text(
                            text = "DM the captain for bug reports & feature requests 🏴‍☠️",
                            fontSize = 11.sp,
                            color = Color(0xFF546E7A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_settings_close")
            ) {
                Text("Close", color = if (isLight) headerColor else MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
