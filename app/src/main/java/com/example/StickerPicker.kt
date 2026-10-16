package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val defaultStickers = listOf(
    // Reactions
    StickerModel("r1", "👍", StickerCategory.ANIM_REACTION, TextAnimLoop.PULSE),
    StickerModel("r2", "❤️", StickerCategory.ANIM_REACTION, TextAnimLoop.PULSE),
    StickerModel("r3", "🔥", StickerCategory.ANIM_REACTION, TextAnimLoop.FLICKER),
    StickerModel("r4", "👏", StickerCategory.ANIM_REACTION, TextAnimLoop.BOUNCE),
    StickerModel("r5", "😂", StickerCategory.ANIM_REACTION, TextAnimLoop.SHAKE),
    StickerModel("r6", "💯", StickerCategory.ANIM_REACTION, TextAnimLoop.PULSE),
    StickerModel("r7", "😍", StickerCategory.ANIM_REACTION, TextAnimLoop.FLOAT),
    StickerModel("r8", "🎉", StickerCategory.ANIM_REACTION, TextAnimLoop.BOUNCE),
    StickerModel("r9", "🤯", StickerCategory.ANIM_REACTION, TextAnimLoop.SHAKE),
    StickerModel("r10", "✨", StickerCategory.ANIM_REACTION, TextAnimLoop.GLOW),
    // Arrows
    StickerModel("a1", "⬆️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a2", "⬇️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a3", "⬅️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a4", "➡️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a5", "⤴️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a6", "⤵️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a7", "↔️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    StickerModel("a8", "↕️", StickerCategory.ANIM_ARROW, TextAnimLoop.FLOAT),
    // Social Media
    StickerModel("s1", "👍 Like", StickerCategory.ANIM_SOCIAL, TextAnimLoop.NONE),
    StickerModel("s2", "🔔 Subscribe", StickerCategory.ANIM_SOCIAL, TextAnimLoop.PULSE),
    StickerModel("s3", "➕ Follow", StickerCategory.ANIM_SOCIAL, TextAnimLoop.NONE),
    StickerModel("s4", "💬 Comment", StickerCategory.ANIM_SOCIAL, TextAnimLoop.BOUNCE),
    StickerModel("s5", "🔗 Link", StickerCategory.ANIM_SOCIAL, TextAnimLoop.NONE),
    StickerModel("s6", "🛒 Shop Now", StickerCategory.ANIM_SOCIAL, TextAnimLoop.PULSE),
    StickerModel("s7", "Swipe Up ⬆️", StickerCategory.ANIM_SOCIAL, TextAnimLoop.FLOAT),
    // Speech Bubbles 
    StickerModel("sb1", "💭", StickerCategory.ANIM_SPEECH, TextAnimLoop.FLOAT),
    StickerModel("sb2", "🗯️", StickerCategory.ANIM_SPEECH, TextAnimLoop.SHAKE),
    StickerModel("sb3", "💬", StickerCategory.ANIM_SPEECH, TextAnimLoop.FLOAT),
    StickerModel("sb4", "Hello 👋", StickerCategory.ANIM_SPEECH, TextAnimLoop.SWING),
    StickerModel("sb5", "Bye 👋", StickerCategory.ANIM_SPEECH, TextAnimLoop.SWING),
    // Decorative
    StickerModel("d1", "🌟", StickerCategory.ANIM_DECOR, TextAnimLoop.GLOW),
    StickerModel("d2", "⭐", StickerCategory.ANIM_DECOR, TextAnimLoop.GLOW),
    StickerModel("d3", "💫", StickerCategory.ANIM_DECOR, TextAnimLoop.GLOW),
    StickerModel("d4", "🎇", StickerCategory.ANIM_DECOR, TextAnimLoop.FLICKER),
    StickerModel("d5", "🎆", StickerCategory.ANIM_DECOR, TextAnimLoop.FLICKER),
    StickerModel("d6", "🎈", StickerCategory.ANIM_DECOR, TextAnimLoop.FLOAT),
    StickerModel("d7", "🎀", StickerCategory.ANIM_DECOR, TextAnimLoop.PULSE),
    StickerModel("d8", "🎁", StickerCategory.ANIM_DECOR, TextAnimLoop.BOUNCE),
    StickerModel("d9", "💎", StickerCategory.ANIM_DECOR, TextAnimLoop.PULSE),
    StickerModel("d10", "👑", StickerCategory.ANIM_DECOR, TextAnimLoop.FLOAT),
    // Labels
    StickerModel("l1", "NEW", StickerCategory.ANIM_LABEL, TextAnimLoop.PULSE),
    StickerModel("l2", "SALE", StickerCategory.ANIM_LABEL, TextAnimLoop.PULSE),
    StickerModel("l3", "WOW", StickerCategory.ANIM_LABEL, TextAnimLoop.PULSE),
    StickerModel("l4", "OMG", StickerCategory.ANIM_LABEL, TextAnimLoop.SHAKE),
    StickerModel("l5", "BREAKING", StickerCategory.ANIM_LABEL, TextAnimLoop.FLICKER),
    StickerModel("l6", "LIVE", StickerCategory.ANIM_LABEL, TextAnimLoop.PULSE),
    StickerModel("l7", "WIN", StickerCategory.ANIM_LABEL, TextAnimLoop.GLOW),
    StickerModel("l8", "FAIL", StickerCategory.ANIM_LABEL, TextAnimLoop.SHAKE),
    // Shapes
    StickerModel("sh1", "Circle", StickerCategory.SHAPE),
    StickerModel("sh2", "Rectangle", StickerCategory.SHAPE),
    StickerModel("sh3", "Line", StickerCategory.SHAPE),
    StickerModel("sh4", "Star", StickerCategory.SHAPE, isIcon = true),
    StickerModel("sh5", "Arrow", StickerCategory.SHAPE, isIcon = true)
)

val standardEmojis = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "☺️", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "tired_face:", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾"
)

@Composable
fun StickerPicker(
    onSelectSticker: (StickerModel) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StickerCategory.EMOJI) }
    
    val allEmojisStr = standardEmojis.joinToString("")
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stickers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close")
                }
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search stickers...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(StickerCategory.values().size) { i ->
                    val cat = StickerCategory.values()[i]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.title) }
                    )
                }
            }
            
            val filteredStickers = defaultStickers.filter {
                (selectedCategory == it.category) && (searchQuery.isEmpty() || it.content.contains(searchQuery, ignoreCase = true))
            }
            
            if (selectedCategory == StickerCategory.EMOJI && searchQuery.isEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(standardEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectSticker(StickerModel(java.util.UUID.randomUUID().toString(), emoji, StickerCategory.EMOJI)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStickers) { sticker ->
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectSticker(sticker) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sticker.category == StickerCategory.SHAPE) {
                                if (sticker.content == "Circle") {
                                    Box(modifier = Modifier.size(40.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                                } else if (sticker.content == "Rectangle") {
                                    Box(modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(4.dp)))
                                } else if (sticker.content == "Line") {
                                    Box(modifier = Modifier.size(width = 40.dp, height = 4.dp).background(Color.White, RoundedCornerShape(2.dp)))
                                } else {
                                    Icon(
                                        imageVector = when(sticker.content) {
                                            "Star" -> Icons.Default.Star
                                            "Arrow" -> Icons.AutoMirrored.Filled.ArrowForward
                                            else -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            } else {
                                AnimatedTextPreview(animLoop = sticker.defaultAnimLoop, text = sticker.content)
                            }
                        }
                    }
                }
            }
        }
    }
}
