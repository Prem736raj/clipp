package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OperationsErrorDialog(
    title: String,
    errorMessage: String,
    suggestedFix: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onReportIssue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("What went wrong:", fontWeight = FontWeight.SemiBold)
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("Suggested Fix:", fontWeight = FontWeight.SemiBold)
                Text(suggestedFix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReportIssue) {
                    Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Report Issue")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
