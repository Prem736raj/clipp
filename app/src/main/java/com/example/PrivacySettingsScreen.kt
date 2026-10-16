package com.example

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("clipp_privacy", Context.MODE_PRIVATE)
    
    var analyticsEnabled by remember { mutableStateOf(prefs.getBoolean("analytics_enabled", false)) }
    var crashReportingEnabled by remember { mutableStateOf(prefs.getBoolean("crash_reporting_enabled", true)) }
    
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Your Content is Private", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Clipp does not upload or analyze your video content. Media processing and project data stay on this device; cloud sync is not available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Text("Data Collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                
                SwitchSettingItem(
                    title = "Local Usage Statistics",
                    subtitle = "Keep editing counts and feature usage on this device for the creator dashboard.",
                    checked = analyticsEnabled,
                    onCheckedChange = { 
                        analyticsEnabled = it
                        com.example.utils.AnalyticsManager.setAnalyticsEnabled(it)
                    }
                )
                
                SwitchSettingItem(
                    title = "Local Crash Recovery",
                    subtitle = "Keep the latest local crash message so you can recover or include it in a support report. Nothing is sent automatically.",
                    checked = crashReportingEnabled,
                    onCheckedChange = { 
                        crashReportingEnabled = it
                        prefs.edit().putBoolean("crash_reporting_enabled", it).apply()
                    }
                )
                
                Spacer(Modifier.height(24.dp))
                Text("Legal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                
                SettingsItem(
                    icon = Icons.Filled.Policy,
                    title = "Privacy Policy",
                    subtitle = "Read our human-friendly privacy policy",
                    onClick = { showPrivacyPolicy = true }
                )
            }
        }
        
        if (showPrivacyPolicy) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
        }
    }
}

@Composable
fun SwitchSettingItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Last Updated: September 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("1. Your Media is Yours\nWe do not upload, analyze, or view your videos, photos, or audio. Processing happens locally on your device.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("2. Local App Data\nOptional usage statistics and crash-recovery messages are stored in the app's private storage. Clipp does not transmit them automatically.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("3. Third-Party Services\nCloud sync, accounts, AI services, and subscriptions are not active in this build.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("4. Data Retention\nLocal projects and preferences remain until you delete them. Use My Data to export or delete local project data.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
