package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val voicePresets = listOf(
    "None", "Deep Voice", "Chipmunk", "Robot", "Echo/Cave", 
    "Telephone", "Whisper", "Monster/Demon", "Baby Voice", 
    "Old Radio", "Narrator"
)

fun applyVoicePreset(preset: String, effects: AudioEffects): AudioEffects {
    val intensity = effects.voiceEffectIntensity
    return when (preset) {
        "None" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0f, reverbAmount = 0f, eqPreset = "Flat")
        "Deep Voice" -> effects.copy(pitchSemitones = -4f * intensity, speed = 1f, distortion = 0f, reverbAmount = 0.1f * intensity)
        "Chipmunk" -> effects.copy(pitchSemitones = 6f * intensity, speed = 1.3f, distortion = 0f, reverbAmount = 0f)
        "Robot" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0.8f * intensity, reverbAmount = 0.2f * intensity, eqPreset = "Bass Boost")
        "Echo/Cave" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0f, reverbAmount = 0.9f * intensity)
        "Telephone" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0.3f * intensity, eqPreset = "Vocal Enhance")
        "Whisper" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0.1f, eqPreset = "Treble Boost", reverbAmount = 0.1f)
        "Monster/Demon" -> effects.copy(pitchSemitones = -6f * intensity, speed = 0.9f, distortion = 0.6f * intensity, reverbAmount = 0.4f * intensity)
        "Baby Voice" -> effects.copy(pitchSemitones = 4f * intensity, speed = 1.1f, distortion = 0f, reverbAmount = 0.1f * intensity)
        "Old Radio" -> effects.copy(pitchSemitones = 0f, speed = 1f, distortion = 0.5f * intensity, eqPreset = "Treble Boost", reverbAmount = 0.1f)
        "Narrator" -> effects.copy(pitchSemitones = -2f * intensity, speed = 1f, distortion = 0f, reverbAmount = 0.3f * intensity, eqPreset = "Bass Boost")
        else -> effects
    }
}

@Composable
fun VoiceChangerPanel(
    effects: AudioEffects,
    onEffectsChanged: (AudioEffects) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Voice Presets", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            voicePresets.forEach { preset ->
                val isSelected = preset == effects.voicePreset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha=0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val newFX = effects.copy(voicePreset = preset)
                            onEffectsChanged(applyVoicePreset(preset, newFX))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        preset, 
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (effects.voicePreset != "None") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Intensity: ${(effects.voiceEffectIntensity * 100).toInt()}%", modifier = Modifier.width(100.dp))
                Slider(
                    value = effects.voiceEffectIntensity,
                    onValueChange = { i -> 
                        val newFX = effects.copy(voiceEffectIntensity = i)
                        onEffectsChanged(applyVoicePreset(effects.voicePreset, newFX))
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Custom Voice Tuner", style = MaterialTheme.typography.labelMedium)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Pitch (${String.format("%.1f", effects.pitchSemitones)})", modifier = Modifier.width(100.dp))
            Slider(
                value = effects.pitchSemitones,
                onValueChange = { i -> onEffectsChanged(effects.copy(pitchSemitones = i)) },
                valueRange = -12f..12f,
                steps = 24,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Speed (${String.format("%.2fx", effects.speed)})", modifier = Modifier.width(100.dp))
            Slider(
                value = effects.speed,
                onValueChange = { i -> onEffectsChanged(effects.copy(speed = i)) },
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Reverb (${(effects.reverbAmount * 100).toInt()})", modifier = Modifier.width(100.dp))
            Slider(
                value = effects.reverbAmount,
                onValueChange = { i -> onEffectsChanged(effects.copy(reverbAmount = i)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Distortion (${(effects.distortion * 100).toInt()})", modifier = Modifier.width(100.dp))
            Slider(
                value = effects.distortion,
                onValueChange = { i -> onEffectsChanged(effects.copy(distortion = i)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
