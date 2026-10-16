package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSetupScreen(
    onBack: () -> Unit,
    onCreateProject: (name: String, aspectRatio: String, resolution: String, frameRate: String) -> Unit
) {
    val defaultName = remember { "Project_" + SimpleDateFormat("MMMdd", Locale.getDefault()).format(Date()) }
    var projectName by remember { mutableStateOf(defaultName) }
    
    var aspectRatio by remember { mutableStateOf("9:16") }
    var resolution by remember { mutableStateOf("1080p") }
    var frameRate by remember { mutableStateOf("30fps") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Project Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { onCreateProject(projectName, aspectRatio, resolution, frameRate) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Create Project", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Project Name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Project Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }

            // Aspect Ratio
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Aspect Ratio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val ratios = listOf(
                    AspectRatioItem("9:16", "TikTok, Reels", 9f/16f),
                    AspectRatioItem("16:9", "YouTube", 16f/9f),
                    AspectRatioItem("1:1", "Instagram Post", 1f),
                    AspectRatioItem("4:5", "Instagram Feed", 4f/5f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(ratios) { item ->
                        AspectRatioCard(
                            item = item,
                            isSelected = aspectRatio == item.ratio,
                            onClick = { aspectRatio = item.ratio }
                        )
                    }
                }
            }

            // Resolution
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("720p", "1080p", "4K").forEach { res ->
                        SelectionCard(
                            text = res,
                            isSelected = resolution == res,
                            onClick = { resolution = res },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Frame Rate
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Frame Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("24fps", "30fps", "60fps").forEach { fps ->
                        SelectionCard(
                            text = fps,
                            isSelected = frameRate == fps,
                            onClick = { frameRate = fps },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

data class AspectRatioItem(val ratio: String, val label: String, val ratioFloat: Float)

@Composable
fun AspectRatioCard(item: AspectRatioItem, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .aspectRatio(item.ratioFloat)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = item.ratio, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(text = item.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun SelectionCard(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, color = textColor)
    }
}
