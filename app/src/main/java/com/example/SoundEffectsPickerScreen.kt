package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class SoundEffectItem(val id: Int, val name: String, val category: String)

object SoundEffectsData {
    val items = mutableListOf<SoundEffectItem>().apply {
        var idCounter = 1
        val transitions = listOf("Whoosh Light", "Whoosh Heavy", "Swoosh Fast", "Impact Deep", "Impact Cinematic", "Rise Sine", "Drop Synth", "Reverse Crash", "Swoosh Smooth", "Impact Hard", "Glitch Stutter", "Swish", "Transition Flare", "Action Hit", "Bass Drop")
        val notifications = listOf("Ding Soft", "Pop Bubble", "Click Sharp", "Chime Elegant", "Alert Warning", "Message Received", "Success Chime", "Error Tone", "Ping Glass", "Notification Bell", "Keyboard Tap", "Subtle Tap", "Tada", "Beep Short", "Bell Ring")
        val comedy = listOf("Drum Roll", "Ba Dum Tss", "Fail Horn", "Boing Cartoon", "Record Scratch", "Fart Short", "Fart Long", "Slide Whistle", "Laugh Track", "Gasp", "Wah Wah Wah", "Mickey Yell", "Quack", "Squeaky Toy", "Spring Jump")
        val nature = listOf("Rain Gentle", "Thunder Crash", "Wind Howul", "Birds Forest", "Ocean Waves", "Fire Crackling", "Cricket Night", "Stream Water", "Storm Heavy", "Leaves Rustle", "Wolf Howl", "Owl Hoot", "Jungle Ambient", "Cave Drip", "River Flow")
        val ambient = listOf("Crowd Noise", "City Traffic", "Office Background", "Coffee Shop", "Subway Rumble", "Park Outdoors", "Restaurant Chatter", "Airport Terminal", "Classroom Mumble", "Helicopter Drone", "Factory Rumble", "Train Station", "Space Hum", "Underwater Bubbles", "Rain on Tin Roof")
        val actions = listOf("Footsteps Dirt", "Footsteps Wood", "Door Open", "Door Close Squeak", "Glass Break", "Explosion Big", "Gunshot Single", "Sword Cling", "Punch Soft", "Punch Hard", "Body Fall", "Coin Drop", "Zip Open", "Paper Crumple", "Car Engine Start")
        val musical = listOf("Countdown Beeps", "Applause Light", "Applause Stadium", "Heartbeat Fast", "Clock Ticking", "Drum Loop", "Guitar Strum", "Piano Chord", "Synth Drone", "Harp Glissando", "Orchestra Hit", "Suspense Drone", "Elevator Music", "Jazz Lick", "Funky Bassline")

        transitions.forEach { add(SoundEffectItem(idCounter++, it, "Transitions")) }
        notifications.forEach { add(SoundEffectItem(idCounter++, it, "Notifications")) }
        comedy.forEach { add(SoundEffectItem(idCounter++, it, "Comedy")) }
        nature.forEach { add(SoundEffectItem(idCounter++, it, "Nature")) }
        ambient.forEach { add(SoundEffectItem(idCounter++, it, "Ambient")) }
        actions.forEach { add(SoundEffectItem(idCounter++, it, "Actions")) }
        musical.forEach { add(SoundEffectItem(idCounter++, it, "Musical")) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundEffectsPickerScreen(
    onClose: () -> Unit,
    onEffectSelected: (uri: String, title: String, durationMs: Long) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = listOf("All") + SoundEffectsData.items.map { it.category }.distinct()
    
    val filteredItems = remember(selectedCategory, searchQuery) {
        SoundEffectsData.items.filter { 
            (selectedCategory == "All" || it.category == selectedCategory) &&
            (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
        }
    }

    var playingEffectId by remember { mutableStateOf<Int?>(null) }
    
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Sound Effects") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                    }
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search effects...") },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                    singleLine = true,
                    shape = CircleShape
                )
                
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = { Text(category) }
                        )
                    }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(filteredItems.size) { i ->
                        val item = filteredItems[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                playingEffectId = if (playingEffectId == item.id) null else item.id 
                                if (playingEffectId != null) {
                                    try {
                                        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
                                    } catch (e: Exception) {}
                                }
                            }) {
                                Icon(
                                    Icons.Filled.PlayArrow, 
                                    contentDescription = "Preview",
                                    tint = if (playingEffectId == item.id) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(item.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Button(onClick = {
                                // Defaulting duration to 2 seconds for effects
                                onEffectSelected("asset:///sound_effect_${item.id}.mp3", item.name, 2000L)
                            }) {
                                Icon(Icons.Filled.Add, "Add")
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                        Divider()
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
