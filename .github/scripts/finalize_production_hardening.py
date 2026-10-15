from pathlib import Path
import re

# Use the compiler-safe Java package-info form for the package-wide Media3 opt-in.
kt_package_info = Path("app/src/main/java/com/example/package-info.kt")
if kt_package_info.exists():
    kt_package_info.unlink()
Path("app/src/main/java/com/example/package-info.java").write_text(
    "@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)\n"
    "package com.example;\n",
    encoding="utf-8",
)

# Keep the persistence regression valid under the canonical timeline model: layerOrder
# must refer to an actual visual layer.
test_path = Path("app/src/test/java/com/example/ProductionHardeningTest.kt")
test_text = test_path.read_text(encoding="utf-8")
replacement = "\n".join([
    '        val liveText = TextOverlay(',
    '            id = "live-text",',
    '            text = "Current draft",',
    '            startTimeOnTimelineMs = 0L,',
    '            durationMs = 1_000L',
    '        )',
    '        val liveState = EditorState(',
    '            canvasSettings = CanvasSettingsState(backgroundColorValue = Color.Blue.value.toLong()),',
    '            texts = listOf(liveText),',
    '            layerOrder = listOf(liveText.id)',
    '        )',
    '',
    '        val json = serializeEditorHistorySnapshot('
])
test_text, count = re.subn(
    r'        val liveState = EditorState\(\n.*?\n        val json = serializeEditorHistorySnapshot\(',
    replacement,
    test_text,
    count=1,
    flags=re.DOTALL,
)
if count != 1:
    raise SystemExit(f"Expected one liveState regression fixture, found {count}")
old_assert = '        assertEquals(listOf("live-layer"), restored.layerOrder)\n'
new_assert = (
    '        assertEquals("Current draft", restored.texts.single().text)\n'
    '        assertEquals(listOf("live-text"), restored.layerOrder)\n'
)
if test_text.count(old_assert) != 1:
    raise SystemExit("Expected one layerOrder assertion")
test_path.write_text(test_text.replace(old_assert, new_assert, 1), encoding="utf-8")

# Fix the two SuspiciousIndentation lint errors in waveform rendering without
# changing behavior.
editor_path = Path("app/src/main/java/com/example/EditorScreen.kt")
editor_text = editor_path.read_text(encoding="utf-8")
old_waveform = '''            if (renderPeaks.isNotEmpty()) {
            val centerY = height / 2f
            val halfHeight = (height * 0.44f).coerceAtLeast(1f)
                renderPeaks.forEachIndexed { index, peak ->
                    val x = ((index + 0.5f) / renderPeaks.size) * totalWidth
                val amplitude = (peak.coerceIn(0f, 1f) * halfHeight).coerceAtLeast(0.5f)
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(x, centerY - amplitude),
                    end = androidx.compose.ui.geometry.Offset(x, centerY + amplitude),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                }
            } else {
            // Honest loading/no-audio state: never synthesize random amplitudes.
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = androidx.compose.ui.geometry.Offset(0f, height / 2f),
                end = androidx.compose.ui.geometry.Offset(totalWidth, height / 2f),
                strokeWidth = 1.dp.toPx()
            )
            }
'''
new_waveform = '''            if (renderPeaks.isNotEmpty()) {
                val centerY = height / 2f
                val halfHeight = (height * 0.44f).coerceAtLeast(1f)
                renderPeaks.forEachIndexed { index, peak ->
                    val x = ((index + 0.5f) / renderPeaks.size) * totalWidth
                    val amplitude = (peak.coerceIn(0f, 1f) * halfHeight).coerceAtLeast(0.5f)
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x, centerY - amplitude),
                        end = androidx.compose.ui.geometry.Offset(x, centerY + amplitude),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            } else {
                // Honest loading/no-audio state: never synthesize random amplitudes.
                drawLine(
                    color = color.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(0f, height / 2f),
                    end = androidx.compose.ui.geometry.Offset(totalWidth, height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
'''
if editor_text.count(old_waveform) != 1:
    raise SystemExit(f"Expected one malformed waveform block, found {editor_text.count(old_waveform)}")
editor_path.write_text(editor_text.replace(old_waveform, new_waveform, 1), encoding="utf-8")

# Keep temporary hardening infrastructure out of the final product commit.
Path(".github/scripts/finalize_production_hardening.py").unlink(missing_ok=True)
