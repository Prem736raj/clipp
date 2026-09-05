package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class VideoTemplate(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val duration: String,
    val uses: Int,
    val slots: Int = 3,
    val textPlaceholders: List<String> = listOf("Title Here", "Subtitle Here"),
    val description: String = "",
    val isCustom: Boolean = false,
    val version: Int = 1,
    val originalId: String? = null
)

val categories = listOf("My Templates", "Trending", "Social Media", "Business", "Personal", "Education", "Music", "Aesthetic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onUseTemplate: (VideoTemplate) -> Unit = {},
    onSlideshowCreator: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedTemplate by remember { mutableStateOf<VideoTemplate?>(null) }
    var showSocialFormatPanel by remember { mutableStateOf(false) }
    
    val templates = TemplateRepo.templates
    
    val filteredTemplates = remember(templates, searchQuery, selectedCategory) {
        val filteredBySearch = templates.filter { 
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
        }
        
        if (selectedCategory == "My Templates") {
            filteredBySearch.filter { it.isCustom }.sortedByDescending { it.uses }
        } else if (selectedCategory == "Trending" && searchQuery.isBlank()) {
            filteredBySearch.filter { !it.isCustom }.sortedByDescending { it.uses }.take(20)
        } else if (selectedCategory == "Trending") {
             filteredBySearch.filter { !it.isCustom }.sortedByDescending { it.uses }
        } else {
            filteredBySearch.filter { it.category == selectedCategory }.sortedByDescending { it.uses }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Templates", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Actions
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { showSocialFormatPanel = true },
                        leadingIcon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Social Format (Reframer)") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer, labelColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                item {
                    AssistChip(
                        onClick = onSlideshowCreator,
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Slideshow Maker") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, labelColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search templates...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isBlank()) {
                    val trendingTags = listOf("#vlog", "#reels", "#cinematic", "#aesthetic", "#tutorial")
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Trending:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    items(trendingTags) { tag ->
                        AssistChip(
                            onClick = { searchQuery = tag.removePrefix("#") },
                            label = { Text(tag) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                }
            }
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(template, onClick = { selectedTemplate = template })
                }
            }
        }
        
        if (selectedTemplate != null) {
            TemplateDetailModal(
                template = selectedTemplate!!,
                onClose = { selectedTemplate = null },
                onUse = {
                    onUseTemplate(it)
                    selectedTemplate = null
                }
            )
        }
        
        // At the bottom of TemplatesScreen
        if (showSocialFormatPanel) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showSocialFormatPanel = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SocialFormatExportPanel(
                        onClose = { showSocialFormatPanel = false },
                        onExportComplete = { showSocialFormatPanel = false }
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateCard(template: VideoTemplate, onClick: () -> Unit) {
    // Infinite transition for breathing/Ken Burns effect
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns_${template.id}")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween((5000..9000).random(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f) // Vertical video aspect ratio
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = template.imageUrl,
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        template.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    template.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(template.duration, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${template.uses / 1000}k uses", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun TemplateDetailModal(
    template: VideoTemplate,
    onClose: () -> Unit,
    onUse: (VideoTemplate) -> Unit
) {
    // Infinite transition for breathing/Ken Burns effect
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns_detail_${template.id}")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = template.imageUrl,
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )
            
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        template.category,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    template.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(template.duration, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${template.uses / 1000}k people used this", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onUse(template) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Use Template", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

object TemplateRepo {
    private val _templates = mutableStateListOf<VideoTemplate>().apply { 
        addAll(generateTemplates())
    }
    
    val templates: List<VideoTemplate> get() = _templates
    
    fun addTemplate(template: VideoTemplate) {
        val existingIndex = _templates.indexOfFirst { it.originalId == template.originalId && template.originalId != null }
        if (existingIndex != -1) {
            _templates[existingIndex] = template.copy(version = _templates[existingIndex].version + 1)
        } else {
            _templates.add(0, template) // Add to top
        }
    }
}

fun generateTemplates(): List<VideoTemplate> {
    val imagePool = listOf(
        "https://images.unsplash.com/photo-1516280440502-a035d8e75185?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1542038784-5f564dc78ef7?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1505236858219-8359eb29e329?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?q=80&w=800&auto=format&fit=crop"
    )
    
    val combinations = mutableListOf<VideoTemplate>()
    
    var idCounter = 1
    
    // Social Media
    val socialTitles = listOf("Trendy TikTok Dance Focus", "Instagram Reel Cinematic", "Shorts Pop Recap", "Vlog Intro Aesthetic", "Quick Tips Reel")
    for (i in 1..10) {
        combinations.add(VideoTemplate("st$idCounter", socialTitles.random(), "Social Media", imagePool.random(), listOf("0:15", "0:07", "0:10").random(), (1000..500000).random()))
        idCounter++
    }
    
    // Business
    val businessTitles = listOf("Product Showcase Pro", "Real Estate Tour", "Modern App Promo", "Sale Ad Attention Grabber", "Client Testimonial", "Corporate Presentation Minimal")
    for (i in 1..10) {
        combinations.add(VideoTemplate("bt$idCounter", businessTitles.random(), "Business", imagePool.random(), listOf("0:30", "0:15", "1:00").random(), (500..50000).random()))
        idCounter++
    }
    
    // Personal
    val personalTitles = listOf("Birthday Celebration Memories", "Travel Vlog Paradise", "Wedding Highlights Cinematic", "Friends Weekend Recap", "Year in Review", "Family Moments Warm")
    for (i in 1..10) {
        combinations.add(VideoTemplate("pt$idCounter", personalTitles.random(), "Personal", imagePool.random(), listOf("1:00", "0:30", "2:00").random(), (2000..80000).random()))
        idCounter++
    }
    
    // Education
    val eduTitles = listOf("Step-by-step Tutorial", "Explainer Clean Minimal", "Online Course Intro", "Language Lesson Setup", "Science Experiment Shorts")
    for (i in 1..10) {
        combinations.add(VideoTemplate("et$idCounter", eduTitles.random(), "Education", imagePool.random(), listOf("1:00", "0:45", "3:00").random(), (1000..30000).random()))
        idCounter++
    }
    
    // Music
    val musicTitles = listOf("Beat Sync Auto", "Lyrical Video Aesthetic", "Bass Drop Visualizer", "Music Festival Recap", "Indie Artist Promo")
    for (i in 1..10) {
        combinations.add(VideoTemplate("mt$idCounter", musicTitles.random(), "Music", imagePool.random(), listOf("0:15", "0:30", "1:00").random(), (10000..900000).random()))
        idCounter++
    }
    
    // Aesthetic
    val aestheticTitles = listOf("Dark Moody Cinematic", "Light Minimal Air", "Vintage Film 8mm", "Neon Cyberpunk Night", "Nature Breathing Slow")
    for (i in 1..10) {
        combinations.add(VideoTemplate("at$idCounter", aestheticTitles.random(), "Aesthetic", imagePool.random(), listOf("0:05", "0:10", "0:15").random(), (5000..400000).random()))
        idCounter++
    }
    
    return combinations.shuffled()
}
