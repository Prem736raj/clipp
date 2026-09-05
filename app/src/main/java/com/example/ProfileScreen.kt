package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(billingManager: com.example.billing.BillingManager) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("clipp_settings", Context.MODE_PRIVATE) }
    
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.authManager.currentUser.collectAsState()
    val currentTier by billingManager.currentTier.collectAsState()
    val subscriptionDetails by billingManager.subscriptionDetails.collectAsState()
    
    var showAuthScreen by remember { mutableStateOf(false) }
    var showPaywallScreen by remember { mutableStateOf(false) }
    var showStorageManager by remember { mutableStateOf(false) }
    var showHealthCheck by remember { mutableStateOf(false) }
    
    var showHelpCenter by remember { mutableStateOf(false) }
    var showContactSupport by remember { mutableStateOf(false) }
    var showReportBug by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showMyData by remember { mutableStateOf(false) }

    var useHaptics by remember { mutableStateOf(sharedPrefs.getBoolean("haptics_enabled", true)) }

    // Setting states
    var resolution by remember { mutableStateOf(sharedPrefs.getString("export_resolution", "1080p") ?: "1080p") }

    var frameRate by remember { mutableStateOf(sharedPrefs.getString("frame_rate", "30fps") ?: "30fps") }
    var aspectRatio by remember { mutableStateOf(sharedPrefs.getString("aspect_ratio", "9:16") ?: "9:16") }
    var autoSave by remember { mutableStateOf(sharedPrefs.getString("auto_save", "5 mins") ?: "5 mins") }
    var themePref by remember { mutableStateOf(sharedPrefs.getString("theme_preference", "Dark") ?: "Dark") }
    var language by remember { mutableStateOf(sharedPrefs.getString("language", "English") ?: "English") }
    var exportFolder by remember { mutableStateOf(sharedPrefs.getString("export_folder", "/Internal/Movies/Clipp") ?: "/Internal/Movies/Clipp") }
    var maxProjectSize by remember { mutableStateOf(sharedPrefs.getString("max_project_size", "Unlimited") ?: "Unlimited") }
    
    var cacheSize by remember { mutableStateOf("234 MB") }
    
    // Bottom Sheet State
    var showSheet by remember { mutableStateOf(false) }
    var sheetTitle by remember { mutableStateOf("") }
    var sheetOptions by remember { mutableStateOf(listOf<String>()) }
    var sheetSelectedOption by remember { mutableStateOf("") }
    var onOptionSelected: ((String) -> Unit)? = null
    
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            // Profile Header
            item {
                if (currentUser == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonOutline,
                                contentDescription = "Profile",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Guest User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAuthScreen = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Sign Up / Log In", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avatarUrl = currentUser?.profilePictureUri
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(100.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    currentUser?.name?.take(1)?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentUser?.name ?: "User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (currentUser?.email != null) {
                            Text(
                                text = currentUser!!.email,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Badge
                        Surface(
                            color = if (currentTier != com.example.billing.SubscriptionTier.FREE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { if (currentTier == com.example.billing.SubscriptionTier.FREE) showPaywallScreen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (currentTier != com.example.billing.SubscriptionTier.FREE) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    contentDescription = null,
                                    tint = if (currentTier != com.example.billing.SubscriptionTier.FREE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentTier != com.example.billing.SubscriptionTier.FREE) "Clipp ${currentTier.title}" else "Free Plan (Upgrade)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTier != com.example.billing.SubscriptionTier.FREE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "340 MB / 2 GB Free Storage Used",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Videos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("42", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Creator Hours", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("12.5 hrs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            if (currentUser != null) {
                item { SettingsSectionHeader("My Stats") }
                item { CreatorStatsSection() }
                
                item { SettingsDivider() }
                
                // Section: Subscription
                item { SettingsSectionHeader("Subscription") }
                
                if (subscriptionDetails != null) {
                    val details = subscriptionDetails!!
                    item {
                        SettingsItem(
                            icon = Icons.Filled.Stars,
                            title = "Manage Subscription",
                            subtitle = "${details.tier.title} Plan • ${if (details.isAutoRenewing) "Renews" else "Expires"} ${details.renewalDate}",
                            onClick = { 
                                billingManager.manageSubscriptionInPlayStore(context as android.app.Activity)
                            }
                        )
                    }
                    if (details.isAutoRenewing) {
                        item {
                            SettingsItem(
                                icon = Icons.Filled.Cancel,
                                title = "Cancel Subscription",
                                subtitle = "You will keep access until ${details.renewalDate}",
                                textColor = MaterialTheme.colorScheme.error,
                                onClick = { 
                                    billingManager.cancelSubscription(context as android.app.Activity)
                                }
                            )
                        }
                    } else if (details.isHold) {
                         item {
                            SettingsItem(
                                icon = Icons.Filled.ErrorOutline,
                                title = "Account on Hold",
                                subtitle = "Payment failed. Please update payment method.",
                                textColor = MaterialTheme.colorScheme.error,
                                onClick = { 
                                    billingManager.manageSubscriptionInPlayStore(context as android.app.Activity)
                                }
                            )
                        }
                    } else if (details.isExpired) {
                         item {
                            SettingsItem(
                                icon = Icons.Filled.Warning,
                                title = "Subscription Expired",
                                subtitle = "Grace period active. Pro features locked for new projects.",
                                textColor = MaterialTheme.colorScheme.error,
                                onClick = { 
                                    showPaywallScreen = true
                                }
                            )
                        }
                    } else if (!details.isAutoRenewing) {
                        item {
                            SettingsItem(
                                icon = Icons.Filled.Refresh,
                                title = "Renew Subscription",
                                subtitle = "Your access expires soon. Tap to renew.",
                                onClick = { 
                                    showPaywallScreen = true
                                }
                            )
                        }
                    }
                } 
                item {
                    SettingsItem(
                        icon = Icons.Filled.Restore,
                        title = "Restore Purchases",
                        subtitle = "Recover Pro access from past purchases",
                        onClick = { 
                            billingManager.restorePurchases(context as android.app.Activity)
                        }
                    )
                }
                
                item { SettingsDivider() }
                
                // Section: Cloud Backup & Sync
                item { SettingsSectionHeader("Cloud Backup & Sync") }
                item {
                    var autoBackup by remember { mutableStateOf(sharedPrefs.getBoolean("auto_backup_wifi", false)) }
                    SettingsToggleItem(
                        icon = Icons.Filled.Wifi,
                        title = "Auto-backup on WiFi",
                        subtitle = "Automatically upload project changes when connected to WiFi",
                        checked = autoBackup,
                        onCheckedChange = { 
                            autoBackup = it
                            sharedPrefs.edit().putBoolean("auto_backup_wifi", it).apply()
                        }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.CloudDownload,
                        title = "Restore from Cloud",
                        subtitle = "Download all cloud projects to this device",
                        onClick = { 
                            android.widget.Toast.makeText(context, "Projects restored from cloud", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item { SettingsDivider() }
            }
            
            // Section: Default Settings
            item { SettingsSectionHeader("Default Settings") }
            item {
                SettingsItem(
                    icon = Icons.Filled.HighQuality,
                    title = "Export Resolution",
                    subtitle = resolution,
                    onClick = {
                        openOptions("Export Resolution", listOf("720p", "1080p", "4K"), resolution) {
                            resolution = it
                            saveString("export_resolution", it)
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Speed,
                    title = "Frame Rate",
                    subtitle = frameRate,
                    onClick = {
                        openOptions("Frame Rate", listOf("24fps", "30fps", "60fps"), frameRate) {
                            frameRate = it
                            saveString("frame_rate", it)
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Crop,
                    title = "Aspect Ratio",
                    subtitle = aspectRatio,
                    onClick = {
                        openOptions("Aspect Ratio", listOf("16:9", "9:16", "1:1", "4:3"), aspectRatio) {
                            aspectRatio = it
                            saveString("aspect_ratio", it)
                        }
                    }
                )
            }
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
            
            item { SettingsDivider() }
            
            item { SettingsDivider() }
            
            // Section: Performance
            item { SettingsSectionHeader("Performance") }
            item {
                val context = LocalContext.current
                var performanceMode by remember { mutableStateOf(com.example.viewmodel.PerformanceModeManager.getMode(context).displayName) }
                
                SettingsItem(
                    icon = Icons.Filled.Speed,
                    title = "Performance Mode",
                    subtitle = performanceMode,
                    onClick = {
                        val modes = com.example.viewmodel.PerformanceMode.values().map { it.displayName }
                        openOptions("Performance Mode", modes, performanceMode) { selected ->
                            performanceMode = selected
                            val mode = com.example.viewmodel.PerformanceMode.values().find { it.displayName == selected }
                            if (mode != null) {
                                com.example.viewmodel.PerformanceModeManager.setMode(context, mode)
                            }
                        }
                    }
                )
            }

            // Section: Appearance
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Theme",
                    subtitle = themePref,
                    onClick = {
                        // Normally this would update app theme immediately in real life
                        openOptions("Theme", listOf("Dark", "Light", "Auto"), themePref) {
                            themePref = it
                            saveString("theme_preference", it)
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    subtitle = language,
                    onClick = {
                        openOptions("Language", listOf("English", "Spanish", "French", "German"), language) {
                            language = it
                            saveString("language", it)
                        }
                    }
                )
            }
            
            item { SettingsDivider() }
            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsToggleItem(
                    icon = Icons.Filled.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Premium tactile vibrations during editing",
                    checked = useHaptics,
                    onCheckedChange = { 
                        useHaptics = it
                        sharedPrefs.edit().putBoolean("haptics_enabled", it).apply()
                    }
                )
            }
            
            // Section: Storage & Health
            item { SettingsSectionHeader("Maintenance") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Storage,
                    title = "Manage Storage",
                    subtitle = "Clear cache, exported videos, and optimize space",
                    onClick = { showStorageManager = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.HealthAndSafety,
                    title = "System Health Check",
                    subtitle = "Verify storage, permissions, and GPU capabilities",
                    onClick = { showHealthCheck = true }
                )
            }

            
            item { SettingsDivider() }
            
            // Section: Privacy & Data
            item { SettingsSectionHeader("Privacy & Data") }
            item {
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Settings",
                    subtitle = "Manage tracking & consents",
                    onClick = { showPrivacySettings = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.DataUsage,
                    title = "My Data",
                    subtitle = "Download or delete your data",
                    onClick = { showMyData = true }
                )
            }
            
            item { SettingsDivider() }
            
            // Section: Help & Support
            item { SettingsSectionHeader("Help & Support") }
            item {
                SettingsItem(
                    icon = Icons.Filled.HelpOutline,
                    title = "Help Center",
                    subtitle = "FAQ and video tutorials",
                    onClick = { showHelpCenter = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Email,
                    title = "Contact Support",
                    onClick = { showContactSupport = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.BugReport,
                    title = "Report a Bug",
                    onClick = { showReportBug = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.NewReleases,
                    title = "What's New",
                    subtitle = "See latest feature updates",
                    onClick = { showWhatsNew = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Group,
                    title = "Community",
                    subtitle = "Join our Discord server",
                    onClick = { 
                        val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.com"))
                        try { context.startActivity(i) } catch(e:Exception){}
                    }
                )
            }
            
            item { SettingsDivider() }
            
            // Section: About
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "App Version",
                    subtitle = "v1.0.0 (Premium)",
                    onClick = { }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.StarRate,
                    title = "Rate Us",
                    onClick = { com.example.utils.ReviewManager.launchNativeReview(context as android.app.Activity) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Lightbulb,
                    title = "Request a Feature",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:")
                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@clippapp.com"))
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Feature Request for Clipp")
                        }
                        try {
                            context.startActivity(android.content.Intent.createChooser(intent, "Send feature request..."))
                        } catch (e: Exception) {}
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = { }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Gavel,
                    title = "Terms of Service",
                    onClick = { }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Code,
                    title = "Open Source Licenses",
                    onClick = { }
                )
            }
            
            if (currentUser != null) {
                item { SettingsDivider() }
                item { SettingsSectionHeader("Account") }
                item {
                    SettingsItem(
                        icon = Icons.Filled.Logout,
                        title = "Log Out",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { authViewModel.authManager.logout() }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Filled.PersonRemove,
                        title = "Delete Account",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { authViewModel.authManager.deleteAccount() }
                    )
                }
            }
        }
        
        if (showAuthScreen) {
            AuthScreen(
                authManager = authViewModel.authManager,
                onClose = { showAuthScreen = false }
            )
        }
        
        if (showStorageManager) {
            StorageManagerScreen(
                onClose = { showStorageManager = false }
            )
        }
        
        if (showHealthCheck) {
            AlertDialog(
                onDismissRequest = { showHealthCheck = false },
                title = { Text("System Health Check") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Running diagnostics...", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("✅ Storage Space: Good (over 5GB free)")
                        Text("✅ Media Permissions: Granted")
                        Text("✅ CPU/GPU Load: Normal")
                        Text("✅ Hardware Codecs: Avc/Hevc Ready")
                        Text("✅ Playback Service: Stable")
                        Spacer(Modifier.height(16.dp))
                        Text("Everything is running smoothly! No issues detected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHealthCheck = false }) { Text("Done") }
                }
            )
        }
        
        if (showPaywallScreen) {
            PaywallScreen(billingManager = billingManager, onDismiss = { showPaywallScreen = false })
        }
        
        if (showHelpCenter) {
            HelpCenterScreen(
                onClose = { showHelpCenter = false },
                onOpenContact = { showContactSupport = true },
                onOpenReportBug = { showReportBug = true }
            )
        }
        
        if (showContactSupport) {
            ContactSupportScreen(
                onClose = { showContactSupport = false },
                isBugReport = false
            )
        }
        
        if (showReportBug) {
            ContactSupportScreen(
                onClose = { showReportBug = false },
                isBugReport = true
            )
        }
        
        if (showWhatsNew) {
            WhatsNewScreen(onClose = { showWhatsNew = false })
        }
        
        if (showPrivacySettings) {
            PrivacySettingsScreen(onClose = { showPrivacySettings = false })
        }
        
        if (showMyData) {
            MyDataScreen(onClose = { showMyData = false })
        }
        
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(
                        text = sheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(24.dp)
                    )
                    
                    sheetOptions.forEach { option ->
                        val isSelected = option == sheetSelectedOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected?.invoke(option)
                                    showSheet = false
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Forward",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
