package com.example

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun PrivacyConsentDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("clipp_privacy", Context.MODE_PRIVATE)
    
    var showDialog by remember { mutableStateOf(!prefs.getBoolean("has_seen_consent", false)) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Your Privacy Comes First", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Clipp is designed to protect your privacy. Here is how we handle your data:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("• Zero Content Tracking: We NEVER analyze or upload your video content. Everything stays on your device.", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("• Anonymous Analytics: We collect basic usage stats (e.g. which features are used) to improve the app.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("• Crash Reports: We collect error logs to fix bugs quickly.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("You can change these preferences at any time in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit()
                        .putBoolean("has_seen_consent", true)
                        .putBoolean("analytics_enabled", true)
                        .putBoolean("crash_reporting_enabled", true)
                        .putBoolean("personalized_recommendations", true)
                        .apply()
                    showDialog = false
                    onDismiss()
                }) {
                    Text("Accept All")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit()
                        .putBoolean("has_seen_consent", true)
                        .putBoolean("analytics_enabled", false)
                        .putBoolean("crash_reporting_enabled", false)
                        .putBoolean("personalized_recommendations", false)
                        .apply()
                    showDialog = false
                    onDismiss()
                }) {
                    Text("Decline Optional")
                }
            }
        )
    }
}
