package com.example

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import java.io.File

data class TrendingAudio(
    val id: String,
    val title: String,
    val durationMs: Long,
    val playCount: String,
    val category: String,
    val uri: String,
    val tags: List<String>,
    var isFavorite: Boolean = false
)

val trendingCategories = listOf("Trending Now", "Popular Songs", "Meme Sounds", "Motivational", "Background Music", "ASMR", "Beats")

fun generateTrendingAudio(): List<TrendingAudio> {
    val list = mutableListOf<TrendingAudio>()
    var idCount = 1
    
    val adjs = listOf("Chill", "Happy", "Sad", "Lo-Fi", "Epic", "Funny", "Ambient", "Deep", "Focus", "Relaxed", "Energetic", "Calm", "Intense", "Mellow", "Upbeat")
    val nouns = listOf("Beat", "Vibes", "Sound", "Noise", "Melody", "Rhythm", "Song", "Track", "BGM", "Tune", "Groove", "Chord", "Bass", "Synth", "Wave")
    
    for (cat in trendingCategories) {
        for (i in 1..10) {
            val adj1 = adjs.random()
            val noun1 = nouns.random()
            val title = "$adj1 $noun1 $idCount"
            list.add(
                TrendingAudio(
                    id = "audio_$idCount",
                    title = title,
                    durationMs = (15000L..120000L).random(),
                    playCount = "${(10..999).random()}.${(0..9).random()}M",
                    category = cat,
                    uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    tags = listOf(cat.lowercase(), adj1.lowercase(), noun1.lowercase())
                )
            )
            idCount++
        }
    }
    return list
}

var globalTrendingAudioCache = generateTrendingAudio()

data class MusicItem(val uri: String, val title: String, val durationMs: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPickerScreen(
    onClose: () -> Unit,
    onMusicSelected: (uri: String, title: String, durationMs: Long) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Add Music") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                    }
                )
                
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Trending") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Music") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Record") })
                }
                
                if (selectedTab == 0) {
                    TrendingMusicTab(onMusicSelected = onMusicSelected)
                } else if (selectedTab == 1) {
                    MyMusicTab(onMusicSelected = onMusicSelected)
                } else {
                    RecordTab(onMusicSelected = onMusicSelected)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingMusicTab(onMusicSelected: (String, String, Long) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(trendingCategories.first()) }
    
    // In a real app we'd fetch this from a server, here we use our mock data.
    var audioList by remember { mutableStateOf(globalTrendingAudioCache) }
    
    val filteredAudio = remember(searchQuery, selectedCategory, audioList) {
        if (searchQuery.isNotBlank()) {
            audioList.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }.sortedByDescending { it.playCount } // String sort is rough but ok for mock
        } else {
            audioList.filter { it.category == selectedCategory }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search by mood, genre or keyword...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            shape = CircleShape,
            singleLine = true
        )

        if (searchQuery.isBlank()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trendingCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredAudio) { audio ->
                var isPlaying by remember { mutableStateOf(false) } // Mock play state
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMusicSelected(audio.uri, audio.title, audio.durationMs) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isPlaying = !isPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = coil.compose.rememberAsyncImagePainter(
                                "https://images.unsplash.com/photo-1614680376573-df3480f0c6ff?q=80&w=200&auto=format&fit=crop"
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Preview",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format("%02d:%02d", (audio.durationMs / 1000) / 60, (audio.durationMs / 1000) % 60)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.LocalFireDepartment, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${audio.playCount} plays",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Fake mock waveform display
                        Row(
                            modifier = Modifier.fillMaxWidth().height(16.dp).padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for(i in 0..20) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(kotlin.random.Random.Default.nextFloat() * 0.8f + 0.2f)
                                        .padding(horizontal = 1.dp)
                                        .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            val newGlobalList = globalTrendingAudioCache.toMutableList()
                            val index = newGlobalList.indexOfFirst { it.id == audio.id }
                            if (index != -1) {
                                newGlobalList[index] = audio.copy(isFavorite = !audio.isFavorite)
                                globalTrendingAudioCache = newGlobalList
                                audioList = newGlobalList
                            }
                        }
                    ) {
                        Icon(
                            if (audio.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (audio.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyMusicTab(onMusicSelected: (String, String, Long) -> Unit) {
    val context = LocalContext.current
    var musicFiles by remember { mutableStateOf<List<MusicItem>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(isReadAudioGranted(context)) }
    
    val reqPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
        if (isGranted) musicFiles = loadMusicFiles(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            musicFiles = loadMusicFiles(context)
        } else {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            reqPerm.launch(perm)
        }
    }

    if (hasPermission) {
        if (musicFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No music files found.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(musicFiles) { music ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMusicSelected(music.uri, music.title, music.durationMs) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(40.dp).padding(8.dp))
                        Column {
                            Text(music.title, style = MaterialTheme.typography.bodyLarge)
                            Text("${music.durationMs / 1000}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { 
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                reqPerm.launch(perm)
            }) {
                Text("Grant Storage Permission")
            }
        }
    }
}

@Composable
fun RecordTab(onMusicSelected: (String, String, Long) -> Unit) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    
    val reqPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            reqPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (hasPermission) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isRecording) {
                    Text("Recording...", color = Color.Red, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(32.dp))
                    IconButton(
                        onClick = {
                            try {
                                recorder?.stop()
                                recorder?.release()
                                recorder = null
                                isRecording = false
                                
                                val uri = android.net.Uri.fromFile(outputFile).toString()
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(outputFile!!.absolutePath)
                                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                val dur = durationStr?.toLongOrNull() ?: 0L
                                retriever.release()
                                
                                if (dur > 0) {
                                    onMusicSelected(uri, "Recording ${System.currentTimeMillis()}", dur)
                                } else {
                                    Toast.makeText(context, "Recording too short", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isRecording = false
                                Toast.makeText(context, "Error stopping", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                } else {
                    Text("Tap to Record", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(32.dp))
                    IconButton(
                        onClick = {
                            try {
                                val file = File(context.cacheDir, "record_${System.currentTimeMillis()}.m4a")
                                outputFile = file
                                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    MediaRecorder()
                                }
                                rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                rec.setOutputFile(file.absolutePath)
                                rec.prepare()
                                rec.start()
                                recorder = rec
                                isRecording = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to start", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
            }
        } else {
            Button(onClick = { reqPerm.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant Microphone Permission")
            }
        }
    }
}

fun isReadAudioGranted(context: android.content.Context): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}

fun loadMusicFiles(context: android.content.Context): List<MusicItem> {
    val list = mutableListOf<MusicItem>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                
                if (duration > 0) {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(MusicItem(uri.toString(), title, duration))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
