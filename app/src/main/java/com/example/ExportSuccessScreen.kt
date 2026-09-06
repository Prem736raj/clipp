package com.example

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlin.math.sin
import com.example.utils.ReviewManager

data class ConfettiParticle(
    val startX: Float,
    val speed: Float,
    val wobbleSpeed: Float,
    val color: Color
)

@Composable
fun ConfettiAnimation() {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while(progress < 1f) {
            delay(16)
            progress += 0.005f
        }
    }
    
    val colors = listOf(Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFFFFEB3B), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
    val particles = remember {
        List(150) { 
            ConfettiParticle(
                startX = kotlin.random.Random.Default.nextFloat(),
                speed = kotlin.random.Random.Default.nextFloat() * 1f + 0.5f,
                wobbleSpeed = kotlin.random.Random.Default.nextFloat() * 15f,
                color = colors.random()
            )
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = (progress * 3f * p.speed) - 0.2f
            val x = p.startX + sin(progress * p.wobbleSpeed) * 0.05f
            if (y > -0.1f && y < 1.1f) {
                drawCircle(
                    color = p.color,
                    radius = 12f,
                    center = Offset(x.toFloat() * size.width, y.toFloat() * size.height)
                )
            }
        }
    }
}

data class SharePlatform(val name: String, val color: Color, val iconText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSuccessScreen(
    thumbnailUri: String?,
    estSizeMB: Float,
    videoDurationMs: Long,
    isAudioOnly: Boolean,
    exportAsGif: Boolean,
    onClose: () -> Unit
) {
    var savedToGallery by remember { mutableStateOf(true) } // Mock auto-save
    val context = LocalContext.current
    
    var showPreReviewDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        com.example.utils.AnalyticsManager.trackVideoExported(com.example.utils.ReviewManager.lastSessionDurationMs)
        if (ReviewManager.incrementExportCountAndCheckReview(context)) {
            delay(1500)
            showPreReviewDialog = true
        }
    }

    PreReviewDialogs(
        showPreReview = showPreReviewDialog,
        onDismissPreReview = { showPreReviewDialog = false },
        showFeedback = showFeedbackDialog,
        onDismissFeedback = { showFeedbackDialog = false },
        onShowFeedback = { showFeedbackDialog = true }
    )

    val platforms = listOf(
        SharePlatform("Instagram", Color(0xFFE1306C), "Ig"),
        SharePlatform("TikTok", Color(0xFF000000), "Tk"),
        SharePlatform("YouTube", Color(0xFFFF0000), "Yt"),
        SharePlatform("WhatsApp", Color(0xFF25D366), "Wa"),
        SharePlatform("Facebook", Color(0xFF1877F2), "Fb"),
        SharePlatform("Snapchat", Color(0xFFFFFC00), "Sc"),
        SharePlatform("Twitter / X", Color(0xFF1DA1F2), "X"),
        SharePlatform("Telegram", Color(0xFF0088CC), "Tg")
    )
    
    var lastSharedPlatform by remember { mutableStateOf<SharePlatform?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiAnimation()
        
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Check, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isAudioOnly) "Audio Exported!" else if (exportAsGif) "GIF Generated!" else "Video Exported!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(if (savedToGallery) "Automatically saved to Gallery" else "Ready to share", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            if (thumbnailUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(thumbnailUri),
                                    contentDescription = "Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp, vertical=2.dp)) {
                                Text(String.format("%02d:%02d", (videoDurationMs / 1000) / 60, (videoDurationMs / 1000) % 60), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${String.format("%.1f", estSizeMB)} MB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Share to Socials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (lastSharedPlatform != null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Last shared:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = { /* open */ },
                                label = { Text(lastSharedPlatform!!.name) },
                                icon = {
                                    Box(modifier = Modifier.size(16.dp).background(lastSharedPlatform!!.color, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(lastSharedPlatform!!.iconText, color = Color.White, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                    }
                                }
                            )
                        }
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(platforms.size) { i ->
                            val p = platforms[i]
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { lastSharedPlatform = p }) {
                                Box(
                                    modifier = Modifier.size(64.dp).background(p.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.iconText, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(p.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { /* Share to all sequentially */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.DynamicFeed, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share to All", fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { /* Share video */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (exportAsGif) "Share as GIF" else "Share Video")
                        }
                        
                        OutlinedButton(
                            onClick = { /* Copy Link */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Link")
                        }
                        
                        OutlinedButton(
                            onClick = { savedToGallery = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(if (savedToGallery) Icons.Filled.Check else Icons.Filled.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Gallery")
                        }
                    }
                }
            }
        }
    }
}
