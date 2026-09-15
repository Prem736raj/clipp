package com.example

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.LruCache
import androidx.media3.common.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

private const val WAVEFORM_CACHE_VERSION = 2
private const val WAVEFORM_MAGIC = 0x434C5746 // CLWF
private const val WAVEFORM_MIN_BUCKETS = 256
private const val WAVEFORM_MAX_BUCKETS = 4_096
private const val WAVEFORM_BUCKET_MS = 10L
private const val DEFAULT_WAVEFORM_CACHE_BYTES = 24L * 1024L * 1024L

internal data class WaveformData(
    val durationMs: Long,
    val peaks: List<Float>
) {
    val hasMeasuredAudio: Boolean get() = peaks.isNotEmpty()
}

internal fun waveformBucketCount(durationMs: Long): Int {
    if (durationMs <= 0L) return WAVEFORM_MIN_BUCKETS
    return ceil(durationMs.toDouble() / WAVEFORM_BUCKET_MS.toDouble())
        .toInt()
        .coerceIn(WAVEFORM_MIN_BUCKETS, WAVEFORM_MAX_BUCKETS)
}

/**
 * Converts a cached full-source envelope into the bars visible for one timeline item.
 * Peak-preserving range sampling keeps transients visible when the timeline is zoomed out.
 */
internal fun waveformPeaksForTimeline(
    waveform: WaveformData,
    trimStartMs: Long,
    trimEndMs: Long,
    displayDurationMs: Long,
    outputCount: Int,
    isLooped: Boolean = false,
    playbackSpeed: Float = 1f,
    speedCurve: SpeedCurve? = null
): List<Float> {
    return WaveformTimelineSampler(
        waveform = waveform,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        displayDurationMs = displayDurationMs,
        isLooped = isLooped,
        playbackSpeed = playbackSpeed,
        speedCurve = speedCurve
    ).peaks(outputCount).toList()
}

/**
 * Stable render-time sampler for one waveform/timeline configuration.
 *
 * The expensive speed-curve lookup table is created once, and the last sampled
 * primitive peak array is reused when Compose redraws at the same bar count.
 */
internal class WaveformTimelineSampler(
    private val waveform: WaveformData,
    trimStartMs: Long,
    trimEndMs: Long,
    private val displayDurationMs: Long,
    private val isLooped: Boolean = false,
    playbackSpeed: Float = 1f,
    speedCurve: SpeedCurve? = null,
    speedTimelineFactory: (Long, SpeedCurve) -> SpeedCurveTimeline = { durationMs, curve ->
        SpeedCurveTimeline(durationMs, curve)
    }
) {
    private val safeDurationMs = waveform.durationMs.coerceAtLeast(1L)
    private val safeStartMs = trimStartMs.coerceIn(0L, safeDurationMs)
    private val safeEndMs = trimEndMs.coerceIn(safeStartMs, safeDurationMs)
    private val trimDurationMs = safeEndMs - safeStartMs
    private val safePlaybackSpeed = playbackSpeed.coerceIn(0.1f, 10f)
    private val speedTimeline = speedCurve
        ?.takeIf { !isLooped && trimDurationMs > 0L }
        ?.let { speedTimelineFactory(trimDurationMs, it) }

    private var cachedOutputCount = -1
    private var cachedPeaks = FloatArray(0)

    fun peaks(outputCount: Int): FloatArray {
        if (!waveform.hasMeasuredAudio || outputCount <= 0 || displayDurationMs <= 0L || trimDurationMs <= 0L) {
            return FloatArray(0)
        }
        if (outputCount == cachedOutputCount) return cachedPeaks

        val sampled = FloatArray(outputCount)
        for (index in 0 until outputCount) {
            val displayStartMs = (displayDurationMs.toDouble() * index / outputCount).toLong()
            val displayEndMs = (displayDurationMs.toDouble() * (index + 1) / outputCount)
                .toLong()
                .coerceAtLeast(displayStartMs + 1L)

            sampled[index] = if (isLooped) {
                val spanMs = displayEndMs - displayStartMs
                if (spanMs >= trimDurationMs) {
                    maxPeak(safeStartMs, safeEndMs)
                } else {
                    val startOffset = displayStartMs % trimDurationMs
                    val endOffset = displayEndMs % trimDurationMs
                    if (startOffset < endOffset) {
                        maxPeak(safeStartMs + startOffset, safeStartMs + endOffset)
                    } else {
                        max(
                            maxPeak(safeStartMs + startOffset, safeEndMs),
                            maxPeak(safeStartMs, safeStartMs + endOffset.coerceAtLeast(1L))
                        )
                    }
                }
            } else {
                val sourceStart = sourceTimeForDisplay(displayStartMs)
                val sourceEnd = sourceTimeForDisplay(displayEndMs)
                maxPeak(
                    minOf(sourceStart, sourceEnd),
                    maxOf(sourceStart, sourceEnd).coerceAtLeast(sourceStart + 1L)
                )
            }
        }

        cachedOutputCount = outputCount
        cachedPeaks = sampled
        return sampled
    }

    fun playbackTimeForSourceOffset(sourceOffsetMs: Long): Long {
        val boundedSourceMs = sourceOffsetMs.coerceIn(0L, trimDurationMs.coerceAtLeast(0L))
        if (trimDurationMs <= 0L) return 0L
        return speedTimeline?.playbackTimeAtSourceFraction(
            boundedSourceMs.toFloat() / trimDurationMs.toFloat()
        ) ?: (boundedSourceMs / safePlaybackSpeed).toLong()
    }

    private fun maxPeak(sourceStartMs: Long, sourceEndMs: Long): Float {
        val start = sourceStartMs.coerceIn(0L, safeDurationMs)
        val end = sourceEndMs.coerceIn(start + 1L, safeDurationMs.coerceAtLeast(start + 1L))
        val lastIndex = waveform.peaks.lastIndex
        if (lastIndex < 0) return 0f
        val startIndex = floor(start.toDouble() / safeDurationMs.toDouble() * waveform.peaks.size)
            .toInt()
            .coerceIn(0, lastIndex)
        val endExclusive = ceil(end.toDouble() / safeDurationMs.toDouble() * waveform.peaks.size)
            .toInt()
            .coerceIn(startIndex + 1, waveform.peaks.size)
        var peak = 0f
        for (index in startIndex until endExclusive) peak = max(peak, waveform.peaks[index])
        return peak.coerceIn(0f, 1f)
    }

    private fun sourceTimeForDisplay(displayMs: Long): Long {
        val boundedDisplayMs = displayMs.coerceIn(0L, displayDurationMs)
        if (isLooped) {
            return safeStartMs + (boundedDisplayMs % trimDurationMs)
        }
        val sourceFraction = if (speedTimeline != null) {
            speedTimeline.sourceFractionAtPlaybackTime(boundedDisplayMs)
        } else {
            (boundedDisplayMs.toDouble() * safePlaybackSpeed / trimDurationMs.toDouble())
                .toFloat()
                .coerceIn(0f, 1f)
        }
        return safeStartMs + (trimDurationMs * sourceFraction).toLong()
    }
}

internal class WaveformDiskCache(
    private val directory: File,
    private val maxBytes: Long = DEFAULT_WAVEFORM_CACHE_BYTES
) {
    init {
        if (!directory.exists()) directory.mkdirs()
    }

    fun read(key: String): WaveformData? {
        val file = fileFor(key)
        if (!file.isFile) return null
        return runCatching {
            DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
                if (input.readInt() != WAVEFORM_MAGIC) error("Invalid waveform cache magic")
                if (input.readInt() != WAVEFORM_CACHE_VERSION) error("Unsupported waveform cache version")
                val durationMs = input.readLong()
                val count = input.readInt()
                if (durationMs <= 0L || count !in 0..WAVEFORM_MAX_BUCKETS) error("Invalid waveform cache payload")
                val peaks = List(count) { input.readFloat().coerceIn(0f, 1f) }
                file.setLastModified(System.currentTimeMillis())
                WaveformData(durationMs, peaks)
            }
        }.onFailure { file.delete() }.getOrNull()
    }

    fun write(key: String, waveform: WaveformData) {
        if (!directory.exists() && !directory.mkdirs()) return
        val target = fileFor(key)
        val temp = File(directory, "$key.tmp")
        runCatching {
            DataOutputStream(BufferedOutputStream(temp.outputStream())).use { output ->
                output.writeInt(WAVEFORM_MAGIC)
                output.writeInt(WAVEFORM_CACHE_VERSION)
                output.writeLong(waveform.durationMs)
                output.writeInt(waveform.peaks.size)
                waveform.peaks.forEach(output::writeFloat)
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            trimToSize()
        }.onFailure { temp.delete() }
    }

    fun fileFor(key: String): File = File(directory, "$key.wfm")

    private fun trimToSize() {
        val files = directory.listFiles { file -> file.isFile && file.extension == "wfm" }
            ?.sortedBy { it.lastModified() }
            .orEmpty()
        var total = files.sumOf(File::length)
        for (file in files) {
            if (total <= maxBytes) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}

internal class WaveformRepository private constructor(
    context: Context,
    cacheDirectory: File = File(context.cacheDir, "waveforms")
) {
    private val appContext = context.applicationContext
    private val diskCache = WaveformDiskCache(cacheDirectory)
    private val memoryCache = LruCache<String, WaveformData>(96)
    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun load(sourceUri: String, declaredDurationMs: Long): WaveformData = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val key = buildCacheKey(uri, declaredDurationMs)
        memoryCache.get(key)?.let { return@withContext it }

        val mutex = keyLocks.getOrPut(key) { Mutex() }
        try {
            mutex.withLock {
                memoryCache.get(key)?.let { return@withLock it }
                diskCache.read(key)?.let {
                    memoryCache.put(key, it)
                    return@withLock it
                }
                val extracted = extractMeasuredWaveform(uri, declaredDurationMs)
                currentCoroutineContext().ensureActive()
                diskCache.write(key, extracted)
                memoryCache.put(key, extracted)
                extracted
            }
        } finally {
            keyLocks.remove(key, mutex)
        }
    }

    internal fun clearMemoryCacheForTests() = memoryCache.evictAll()
    internal fun cacheFileForTests(sourceUri: String, declaredDurationMs: Long): File =
        diskCache.fileFor(buildCacheKey(Uri.parse(sourceUri), declaredDurationMs))

    private fun buildCacheKey(uri: Uri, declaredDurationMs: Long): String {
        var length = -1L
        var modified = -1L
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            val file = File(uri.path!!)
            length = file.length()
            modified = file.lastModified()
        } else {
            runCatching {
                appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    length = descriptor.length
                }
            }
        }
        val raw = "$WAVEFORM_CACHE_VERSION|$uri|$declaredDurationMs|$length|$modified"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private suspend fun extractMeasuredWaveform(uri: Uri, declaredDurationMs: Long): WaveformData {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(appContext, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return WaveformData(declaredDurationMs.coerceAtLeast(1L), emptyList())

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return WaveformData(declaredDurationMs.coerceAtLeast(1L), emptyList())
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            } else {
                declaredDurationMs.coerceAtLeast(1L) * 1_000L
            }
            val durationMs = max(1L, durationUs / 1_000L)
            val accumulator = WaveformPeakAccumulator(durationUs, waveformBucketCount(durationMs))

            if (mime == "audio/raw") {
                val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val encoding = inputFormat.intOrDefault(MediaFormat.KEY_PCM_ENCODING, C.ENCODING_PCM_16BIT)
                val bufferSize = inputFormat.intOrDefault(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024).coerceAtLeast(64 * 1024)
                val buffer = ByteBuffer.allocateDirect(bufferSize)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    accumulator.consume(
                        buffer = buffer,
                        offset = 0,
                        size = size,
                        presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L),
                        sampleRate = sampleRate,
                        channelCount = channels,
                        encoding = encoding
                    )
                    extractor.advance()
                }
            } else {
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(inputFormat, null, null, 0)
                codec.start()
                var inputEnded = false
                var outputEnded = false
                var outputFormat = inputFormat
                val info = MediaCodec.BufferInfo()
                while (!outputEnded) {
                    currentCoroutineContext().ensureActive()
                    if (!inputEnded) {
                        val inputIndex = codec.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                            val sampleSize = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    extractor.sampleFlags
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                        else -> if (outputIndex >= 0) {
                            if (info.size > 0) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex)
                                if (outputBuffer != null) {
                                    accumulator.consume(
                                        buffer = outputBuffer,
                                        offset = info.offset,
                                        size = info.size,
                                        presentationTimeUs = info.presentationTimeUs.coerceAtLeast(0L),
                                        sampleRate = outputFormat.intOrDefault(MediaFormat.KEY_SAMPLE_RATE, 44_100),
                                        channelCount = outputFormat.intOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 1),
                                        encoding = outputFormat.intOrDefault(MediaFormat.KEY_PCM_ENCODING, C.ENCODING_PCM_16BIT)
                                    )
                                }
                            }
                            outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }

            WaveformData(durationMs, accumulator.finish())
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun MediaFormat.intOrDefault(key: String, fallback: Int): Int =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(fallback) else fallback

    companion object {
        @Volatile private var shared: WaveformRepository? = null

        fun get(context: Context): WaveformRepository = shared ?: synchronized(this) {
            shared ?: WaveformRepository(context.applicationContext).also { shared = it }
        }

        internal fun createForTests(context: Context, cacheDirectory: File): WaveformRepository =
            WaveformRepository(context, cacheDirectory)
    }
}

internal class WaveformPeakAccumulator(
    private val durationUs: Long,
    bucketCount: Int
) {
    private val peaks = FloatArray(bucketCount.coerceIn(1, WAVEFORM_MAX_BUCKETS))

    fun consume(
        buffer: ByteBuffer,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int
    ) {
        if (size <= 0 || sampleRate <= 0 || channelCount <= 0 || durationUs <= 0L) return
        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val bytesPerFrame = bytesPerSample * channelCount
        if (bytesPerFrame <= 0) return
        val frameCount = size / bytesPerFrame
        if (frameCount <= 0) return
        val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        input.position(offset.coerceIn(0, input.limit()))
        input.limit((offset + frameCount * bytesPerFrame).coerceAtMost(input.capacity()))

        repeat(frameCount) { frameIndex ->
            var framePeak = 0f
            repeat(channelCount) {
                val sample = when (encoding) {
                    C.ENCODING_PCM_8BIT -> ((input.get().toInt() and 0xff) - 128) / 128f
                    C.ENCODING_PCM_16BIT -> input.short / 32768f
                    C.ENCODING_PCM_24BIT -> {
                        val b0 = input.get().toInt() and 0xff
                        val b1 = input.get().toInt() and 0xff
                        val b2 = input.get().toInt()
                        val value = b0 or (b1 shl 8) or (b2 shl 16)
                        value / 8_388_608f
                    }
                    C.ENCODING_PCM_32BIT -> input.int / 2_147_483_648f
                    C.ENCODING_PCM_FLOAT -> input.float.coerceIn(-1f, 1f)
                    else -> input.short / 32768f
                }
                framePeak = max(framePeak, abs(sample).coerceIn(0f, 1f))
            }
            val timeUs = presentationTimeUs + frameIndex * 1_000_000L / sampleRate
            val bucket = ((timeUs.toDouble() / durationUs.toDouble()) * peaks.size)
                .toInt()
                .coerceIn(0, peaks.lastIndex)
            peaks[bucket] = max(peaks[bucket], framePeak)
        }
    }

    fun finish(): List<Float> {
        // Fill tiny timestamp gaps from decoder packet boundaries without inventing
        // amplitudes across genuinely silent regions.
        for (index in 1 until peaks.lastIndex) {
            if (peaks[index] == 0f && peaks[index - 1] > 0f && peaks[index + 1] > 0f) {
                peaks[index] = minOf(peaks[index - 1], peaks[index + 1])
            }
        }
        return peaks.map { it.coerceIn(0f, 1f) }
    }
}
