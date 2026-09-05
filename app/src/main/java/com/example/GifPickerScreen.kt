package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.example.data.GiphyApiClient
import com.example.data.GiphyGif
import kotlinx.coroutines.launch

@Composable
fun GifPickerScreen(
    onClose: () -> Unit,
    onGifSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GiphyGif>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search GIFs", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search Tenor / Giphy...") },
                trailingIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            val apiKey = BuildConfig.GIPHY_API_KEY
                            if (apiKey.isNullOrBlank() || apiKey == "MY_GIPHY_API_KEY") {
                                errorMessage = "GIPHY API Key is missing. Add it in Secrets."
                                return@launch
                            }
                            isLoading = true
                            errorMessage = null
                            try {
                                val response = GiphyApiClient.apiService.searchGifs(apiKey = apiKey, query = query)
                                results = response.data
                            } catch (e: Exception) {
                                errorMessage = "Error searching GIFs: \${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                singleLine = true
            )
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(results.size) { i ->
                        val gif = results[i]
                        val url = gif.images.fixed_height.url
                        
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(url)
                                .decoderFactory(
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        ImageDecoderDecoder.Factory()
                                    } else {
                                        GifDecoder.Factory()
                                    }
                                )
                                .build(),
                            contentDescription = "GIF",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onGifSelected(url)
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
