package com.example

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.AnalyticsManager
import com.example.utils.StatsDashboardData
import com.example.utils.TimeRange

@Composable
fun CreatorStatsSection() {
    val stats by AnalyticsManager.dashboardData.collectAsState()
    var timeRange by remember { mutableStateOf(TimeRange.WEEK) }
    
    if (stats == null) return
    val s = stats!!
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (s.streak > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${s.streak} Day Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Time toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRangeFilter("This Week", timeRange == TimeRange.WEEK) { 
                timeRange = TimeRange.WEEK
                AnalyticsManager.setTimeRange(TimeRange.WEEK)
            }
            TimeRangeFilter("This Month", timeRange == TimeRange.MONTH) { 
                timeRange = TimeRange.MONTH
                AnalyticsManager.setTimeRange(TimeRange.MONTH)
            }
            TimeRangeFilter("All Time", timeRange == TimeRange.ALL_TIME) { 
                timeRange = TimeRange.ALL_TIME
                AnalyticsManager.setTimeRange(TimeRange.ALL_TIME)
            }
        }
        
        // Key Metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "Videos", value = s.created.toString(), icon = Icons.Filled.MovieCreation)
            StatCard(modifier = Modifier.weight(1f), title = "Exported", value = s.exported.toString(), icon = Icons.Filled.FileDownload)
            val formatTime = formatStatsDuration(s.editingTimeMs)
            StatCard(modifier = Modifier.weight(1.2f), title = "Time", value = formatTime, icon = Icons.Filled.Timer)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Chart
        Text("Activity (Last 30 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        ActivityChart(s.dailyActivity)
        
        Spacer(Modifier.height(16.dp))
        
        // Top Features
        if (s.features.isNotEmpty()) {
            Text("Top Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val topFeatures = s.features.entries.sortedByDescending { it.value }.take(5)
            topFeatures.forEach { (feat, count) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(feat, style = MaterialTheme.typography.bodyMedium)
                    Text("$count uses", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        
        // Badges
        if (s.badges.isNotEmpty()) {
            Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.badges.forEach { badgeName ->
                    BadgeItem(badgeName)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun TimeRangeFilter(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier.height(90.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActivityChart(dailyActivity: List<Float>) {
    val maxAct = dailyActivity.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dailyActivity.forEach { acts ->
            val heightRatio = acts / maxAct
            val animatedHeight by animateFloatAsState(
                targetValue = heightRatio,
                animationSpec = tween(durationMillis = 500)
            )
            
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(animatedHeight.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(if (acts > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun BadgeItem(badgeName: String) {
    val icon = when(badgeName) {
        "First Video" -> Icons.Filled.Star
        "10 Videos" -> Icons.Filled.MilitaryTech
        "100 Videos" -> Icons.Filled.EmojiEvents
        "Night Owl Editor" -> Icons.Filled.DarkMode
        "Speed Editor" -> Icons.Filled.Speed
        "AI Master" -> Icons.Filled.AutoAwesome
        else -> Icons.Filled.WorkspacePremium
    }
    
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(badgeName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

private fun formatStatsDuration(ms: Long): String {
    val totalMins = ms / 60000
    if (totalMins < 60) return "${totalMins}m"
    val hours = totalMins / 60
    val mins = totalMins % 60
    return "${hours}h ${mins}m"
}
