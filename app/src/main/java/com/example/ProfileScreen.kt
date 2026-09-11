package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onThemeChanged: (String) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("clipp_prefs", Context.MODE_PRIVATE) }

    var showStorageManager by remember { mutableStateOf(false) }
    var showHelpCenter by remember { mutableStateOf(false) }
    var showContactSupport by remember { mutableStateOf(false) }
    var showReportBug by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showMyData by remember { mutableStateOf(false) }

    var useHaptics by remember { mutableStateOf(sharedPrefs.getBoolean("haptics_enabled", true)) }
    var autoSave by remember { mutableStateOf(sharedPrefs.getString("auto_save", "30 secs") ?: "30 secs") }
    var themePref by remember {
        mutableStateOf(
            when (sharedPrefs.getString("theme_preference", "System")) {
                "Dark" -> "Dark"
                "Light" -> "Light"
                else -> "Auto"
            }
        )
    }
    var performanceMode by remember { mutableStateOf(com.example.viewmodel.PerformanceModeManager.getMode(context).displayName) }

    var showSheet by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("") }
    var sheetOptions by remember { mutableStateOf(emptyList<String>()) }
    var sheetSelectedOption by remember { mutableStateOf("") }
    var onOptionSelected by remember { mutableStateOf<(String) -> Unit>({}) }

    fun openOptions(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
        sheetTitle = title
        sheetOptions = options
        sheetSelectedOption = selected
        onOptionSelected = onSelect
        showSheet = true
    }

    fun saveString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.PersonOutline,
                            contentDescription = "Profile",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Text("Guest User", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(4.dp))
                    Text("Local-only workspace", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Projects and media references stay on this device. Accounts, cloud sync, subscriptions, and AI tools are not active in this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(32.dp))
            }

            item { SettingsSectionHeader("Editor") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Save,
                    title = "Auto-save Interval",
                    subtitle = autoSave,
                    onClick = {
                        openOptions("Auto-save Interval", listOf("30 secs", "1 min", "5 mins", "10 mins", "Off"), autoSave) {
                            autoSave = it
                            saveString("auto_save", it)
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Speed,
                    title = "Performance Mode",
                    subtitle = performanceMode,
                    onClick = {
                        val modes = com.example.viewmodel.PerformanceMode.values().map { it.displayName }
                        openOptions("Performance Mode", modes, performanceMode) { selected ->
                            performanceMode = selected
                            com.example.viewmodel.PerformanceMode.values()
                                .firstOrNull { it.displayName == selected }
                                ?.let { com.example.viewmodel.PerformanceModeManager.setMode(context, it) }
                        }
                    }
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Theme",
                    subtitle = themePref,
                    onClick = {
                        openOptions("Theme", listOf("Dark", "Light", "Auto"), themePref) {
                            themePref = it
                            val normalized = if (it == "Auto") "System" else it
                            saveString("theme_preference", normalized)
                            onThemeChanged(normalized)
                        }
                    }
                )
            }

            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsToggleItem(
                    icon = Icons.Filled.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Use tactile feedback during editing",
                    checked = useHaptics,
                    onCheckedChange = {
                        useHaptics = it
                        sharedPrefs.edit().putBoolean("haptics_enabled", it).apply()
                    }
                )
            }

            item { SettingsSectionHeader("Maintenance") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Storage,
                    title = "Manage Storage",
                    subtitle = "View local usage and clear app cache",
                    onClick = { showStorageManager = true }
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("Privacy & Data") }
            item {
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Settings",
                    subtitle = "Manage local statistics and crash recovery",
                    onClick = { showPrivacySettings = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    title = "My Data",
                    subtitle = "Export or delete local project data",
                    onClick = { showMyData = true }
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("Help & Support") }
            item {
                SettingsItem(Icons.Filled.HelpOutline, "Help Center", "FAQ and current capabilities") { showHelpCenter = true }
            }
            item {
                SettingsItem(Icons.Filled.Email, "Contact Support") { showContactSupport = true }
            }
            item {
                SettingsItem(Icons.Filled.BugReport, "Report a Bug") { showReportBug = true }
            }
            item {
                SettingsItem(Icons.Filled.NewReleases, "What's New", "See shipped changes") { showWhatsNew = true }
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(Icons.Filled.Info, "App Version", "v${BuildConfig.VERSION_NAME}") { }
            }
            item {
                SettingsItem(Icons.Filled.StarRate, "Rate Us") {
                    (context as? Activity)?.let { com.example.utils.ReviewManager.launchNativeReview(it) }
                }
            }
            item {
                SettingsItem(Icons.Filled.Lightbulb, "Request a Feature") {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@clippapp.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feature Request for Clipp")
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Send feature request...")) }
                }
            }
        }

        if (showStorageManager) StorageManagerScreen(onClose = { showStorageManager = false })
        if (showHelpCenter) {
            HelpCenterScreen(
                onClose = { showHelpCenter = false },
                onOpenContact = { showContactSupport = true },
                onOpenReportBug = { showReportBug = true }
            )
        }
        if (showContactSupport) ContactSupportScreen(onClose = { showContactSupport = false }, isBugReport = false)
        if (showReportBug) ContactSupportScreen(onClose = { showReportBug = false }, isBugReport = true)
        if (showWhatsNew) WhatsNewScreen(onClose = { showWhatsNew = false })
        if (showPrivacySettings) PrivacySettingsScreen(onClose = { showPrivacySettings = false })
        if (showMyData) MyDataScreen(onClose = { showMyData = false })

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(
                        sheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(24.dp)
                    )
                    sheetOptions.forEach { option ->
                        val isSelected = option == sheetSelectedOption
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onOptionSelected(option)
                                showSheet = false
                            }.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(option, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                            if (isSelected) {
                                androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            subtitle?.let {
                Spacer(Modifier.size(2.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.material3.Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Forward",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Spacer(Modifier.size(2.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
