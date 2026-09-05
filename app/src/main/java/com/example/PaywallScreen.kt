package com.example

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billing.BillingManager
import com.example.billing.SubscriptionTier

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isYearly by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Upgrade to Clipp Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Unlock your full creative potential.", style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Monthly")
                            Switch(checked = isYearly, onCheckedChange = { isYearly = it }, modifier = Modifier.padding(horizontal = 8.dp))
                            Text("Yearly (Save 30%)")
                        }
                    }
                }

                // Tiers
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TierCard(
                            title = "Free",
                            price = "$0",
                            features = listOf("Core editing tools", "1080p export", "No watermark", "Basic effects & filters", "5 AI captions / day", "Basic templates"),
                            buttonText = "Current Plan",
                            onClick = { },
                            isHighlighted = false
                        )
                    }
                    item {
                        TierCard(
                            title = "Pro",
                            price = if (isYearly) "$39.99/yr" else "$4.99/mo",
                            subtitle = "7-day free trial",
                            features = listOf("4K Export", "All Premium Effects & Transitions", "Unlimited AI Captions", "Premium Templates", "Cloud Backup (5GB)", "Priority Export Speed"),
                            buttonText = "Start Free Trial",
                            onClick = { 
                                billingManager.launchBillingFlow(context as Activity, SubscriptionTier.PRO, !isYearly)
                                onDismiss()
                            },
                            isHighlighted = true
                        )
                    }
                    item {
                        TierCard(
                            title = "Pro+",
                            price = if (isYearly) "$79.99/yr" else "$9.99/mo",
                            features = listOf("Unlimited Cloud Storage", "Team Collaboration", "Template Marketplace", "All AI Features Unlimited", "Priority Support"),
                            buttonText = "Upgrade to Pro+",
                            onClick = { 
                                billingManager.launchBillingFlow(context as Activity, SubscriptionTier.PRO_PLUS, !isYearly)
                                onDismiss()
                            },
                            isHighlighted = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TierCard(
    title: String,
    price: String,
    subtitle: String? = null,
    features: List<String>,
    buttonText: String,
    onClick: () -> Unit,
    isHighlighted: Boolean
) {
    val borderColor = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                if (subtitle != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(
                        Icons.Filled.Check, 
                        contentDescription = "Included", 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClick, 
                modifier = Modifier.fillMaxWidth(),
                colors = if (isHighlighted) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(buttonText)
            }
        }
    }
}
