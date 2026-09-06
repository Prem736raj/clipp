package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(onClose: () -> Unit, isBugReport: Boolean = false) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(if (isBugReport) "Bug Report" else "General Inquiry") }
    var description by remember { mutableStateOf("") }
    
    val categories = listOf("General Inquiry", "Billing/Subscription", "Feature Request", "Bug Report", "Other")
    
    // For Bug Report auto-inclusion
    val deviceInfo = "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                     "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                     "App Version: 1.0.0"
                     
    val crashLog = if (isBugReport) {
        val prefs = context.getSharedPreferences("clipp_crash_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getString("last_crash_log", "No recent crashes recorded.") ?: "No recent crashes recorded."
    } else ""

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text(if (isBugReport) "Report a Bug" else "Contact Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                if (isBugReport) {
                    Text("We're sorry you experienced an issue. Please describe what you were doing when the bug occurred, and we'll look into it right away.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }
                
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { selectedCategory = cat; expanded = false }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Description", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Please provide as much detail as possible...") },
                    shape = RoundedCornerShape(8.dp)
                )
                
                if (isBugReport) {
                    Spacer(Modifier.height(16.dp))
                    Text("System Info (Automatically Included)", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Text(deviceInfo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Crash Log", style = MaterialTheme.typography.labelLarge)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Text(crashLog.take(200) + if (crashLog.length > 200) "..." else "", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).fillMaxWidth())
                    }
                }
                
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val body = StringBuilder()
                        body.append(description)
                        body.append("\n\n---")
                        body.append("\nCategory: $selectedCategory")
                        if (isBugReport) {
                            body.append("\n\n$deviceInfo")
                            body.append("\n\nCrash Log:\n$crashLog")
                        }
                        
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@clippapp.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Clipp ${if (isBugReport) "Bug Report" else "Support"}: $selectedCategory")
                            putExtra(Intent.EXTRA_TEXT, body.toString())
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        } catch (e: Exception) {}
                        
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Send via Email")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
