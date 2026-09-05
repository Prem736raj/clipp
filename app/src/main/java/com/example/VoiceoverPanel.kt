package com.example

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VoiceoverPanel(
    onClose: () -> Unit,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onSetPlaying: (Boolean) -> Unit,
    onVoiceoverRecorded: (uri: String, duration: Long) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    
    val reqPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            reqPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var recordingState by remember { mutableStateOf(0) } // 0: Idle, 1: Countdown, 2: Recording
    var countdown by remember { mutableStateOf(3) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var amplitude by remember { mutableStateOf(0) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var recordingStartMs by remember { mutableStateOf(0L) }

    LaunchedEffect(recordingState) {
        if (recordingState == 1) {
            countdown = 3
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            recordingState = 2
            onSetPlaying(true)
            recordingStartMs = System.currentTimeMillis()
            
            try {
                val file = File(context.cacheDir, "voiceover_${System.currentTimeMillis()}.m4a")
                outputFile = file
                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    MediaRecorder()
                }
                rec.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION) // Basic automatic noise reduction
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioEncodingBitRate(128000)
                rec.setAudioSamplingRate(44100)
                rec.setOutputFile(file.absolutePath)
                rec.prepare()
                rec.start()
                recorder = rec
            } catch (e: Exception) {
                recordingState = 0
                onSetPlaying(false)
            }
        }
    }

    LaunchedEffect(recordingState) {
        if (recordingState == 2) {
            while (true) {
                delay(50)
                if (recorder != null) {
                    try {
                        amplitude = recorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {}
                }
                recordingDuration = System.currentTimeMillis() - recordingStartMs
            }
        }
    }

    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Voiceover Recording", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (recordingState == 2) {
                        try {
                            recorder?.stop()
                            recorder?.release()
                        } catch (e: Exception) {}
                        onSetPlaying(false)
                    }
                    onClose()
                }) { Icon(Icons.Filled.Close, "Close") }
            }

            Spacer(Modifier.height(16.dp))

            if (!hasPermission) {
                Text("Microphone permission required")
                Button(onClick = { reqPerm.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Permission")
                }
                return@Column
            }

            if (recordingState == 0) {
                Text("Position the timeline where you want to start.")
                Spacer(Modifier.height(16.dp))
                IconButton(
                    onClick = { recordingState = 1 },
                    modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Filled.Mic, "Start Recording", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                Text("Tap to Record", modifier = Modifier.padding(top = 8.dp))
            } else if (recordingState == 1) {
                Text("Get Ready...", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    text = countdown.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
            } else if (recordingState == 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).scale(pulse).background(Color.Red, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("RECORDING", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    Text("${recordingDuration / 1000}.${(recordingDuration % 1000) / 100}s")
                }
                Spacer(Modifier.height(16.dp))
                
                val level = (amplitude / 32767f).coerceIn(0f, 1f)
                Box(modifier = Modifier.fillMaxWidth(0.8f).height(24.dp).clip(CircleShape).background(Color.Black.copy(alpha=0.3f))) {
                    Box(modifier = Modifier.fillMaxWidth(level).fillMaxHeight().background(
                        if (level > 0.8f) Color.Red else if (level > 0.5f) Color.Yellow else Color.Green
                    ))
                }
                
                Spacer(Modifier.height(24.dp))
                IconButton(
                    onClick = {
                        try {
                            recorder?.stop()
                            recorder?.release()
                            recorder = null
                            onSetPlaying(false)
                            recordingState = 0
                            
                            val androidUri = android.net.Uri.fromFile(outputFile).toString()
                            val duration = recordingDuration
                            if (duration > 500) {
                                onVoiceoverRecorded(androidUri, duration)
                                onClose()
                            }
                        } catch (e: Exception) {
                            recordingState = 0
                            onSetPlaying(false)
                        }
                    },
                    modifier = Modifier.size(80.dp).background(Color.Red, CircleShape)
                ) {
                    Icon(Icons.Filled.Stop, "Stop", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
