package com.example

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.utils.ReviewManager

@Composable
fun PreReviewDialogs(
    showPreReview: Boolean,
    onDismissPreReview: () -> Unit,
    showFeedback: Boolean,
    onDismissFeedback: () -> Unit,
    onShowFeedback: () -> Unit
) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }

    if (showPreReview) {
        AlertDialog(
            onDismissRequest = onDismissPreReview,
            title = { Text("Enjoying Clipp?") },
            text = { Text("We'd love to know how your experience has been so far!") },
            confirmButton = {
                TextButton(onClick = {
                    onDismissPreReview()
                    ReviewManager.launchNativeReview(context as Activity)
                }) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Yes!")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismissPreReview()
                    onShowFeedback()
                }) {
                    Icon(Icons.Filled.ThumbDown, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Not really")
                }
            }
        )
    }
    
    if (showFeedback) {
        AlertDialog(
            onDismissRequest = { 
                onDismissFeedback()
                ReviewManager.setReviewShown(context) 
            },
            title = { Text("How can we improve?") },
            text = {
                Column {
                    Text("Your feedback helps us make Clipp better.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Tell us what went wrong...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDismissFeedback()
                    ReviewManager.setReviewShown(context)
                    ReviewManager.sendFeedbackEmail(context, feedbackText)
                }) {
                    Text("Submit Feedback")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDismissFeedback()
                    ReviewManager.setReviewShown(context) 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
