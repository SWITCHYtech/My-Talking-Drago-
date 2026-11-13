package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class PetSoundEffect {
    GIGGLE,
    BOING,
    YUMMY,
    BLEH,
    SPICY,
    COIN,
    SPLASH,
    KICK,
    SNORE,
    PURR,
    MEOW
}

class PetSoundEngine(context: Context? = null) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playbackMutex = Mutex()
    private val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isTalking = MutableStateFlow(false)
    val isTalking: StateFlow<Boolean> = _isTalking.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _mouthAmplitude = MutableStateFlow(0f)
    val mouthAmplitude: StateFlow<Float> = _mouthAmplitude.asStateFlow()

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    private var lastEffectMs: Long = 0L

    @Volatile
    private var audioOutputDisabled: Boolean = false

    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private val recordedSamples = mutableListOf<Short>()

    private val recordSampleRate = 16000
    // ~1.40x speed/pitch shift creates the classic playful Talking Tom high-pitched voice
    private val repeatPitchFactor = 1.40f

    fun playEffect(effect: PetSoundEffect) {
        if (!soundEnabled || audioOutputDisabled) return
        val now = System.currentTimeMillis()
        if (now - lastEffectMs < 90L || playbackMutex.isLocked || _isTalking.value) return
        lastEffectMs = now

        scope.launch {
            if (!playbackMutex.tryLock()) return@launch
            try {
                val sampleRate = resolveOutputSampleRate()
                val samples = synthesizeEffect(effect, sampleRate)
                playPcmInternal(samples, sampleRate, driveMouth = false)
            } catch (_: Throwable) {
                // Gracefully ignore audio hardware failures
            } finally {
                playbackMutex.unlock()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening(onAutoStop: () -> Unit, onMicError: (String) -> Unit) {
        if (_isListening.value || _isTalking.value) return
        val minBuf = AudioRecord.getMinBufferSize(
            recordSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            onMicError("Microphone is not available on this device.")
            return
        }

        val bufferSize = maxOf(minBuf * 2, 4096)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                recordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: SecurityException) {
            onMicError("Microphone permission is needed so Dragoș can hear you.")
            return
        } catch (_: Throwable) {
            onMicError("Could not access the microphone right now.")
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { recorder.release() }
            onMicError("Microphone could not initialize.")
            return
        }

        synchronized(recordedSamples) {
            recordedSamples.clear()
        }

        _isListening.value = true
        _micAmplitude.value = 0f

        recordJob = scope.launch {
            val readBuf = ShortArray(1024)
            val maxSamples = recordSampleRate * 8 // 8 seconds max clip
            try {
                recorder.startRecording()
                while (isActive && _isListening.value) {
                    val read = recorder.read(readBuf, 0, readBuf.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val s = readBuf[i].toDouble()
                            sumSq += s * s
                        }
                        val rms = sqrt(sumSq / read) / 14000.0
                        _micAmplitude.value = rms.toFloat().coerceIn(0f, 1f)

                        val shouldStop = synchronized(recordedSamples) {
                            for (i in 0 until read) {
                                recordedSamples.add(readBuf[i])
                            }
                            recordedSamples.size >= maxSamples
                        }
                        if (shouldStop) {
                            _isListening.value = false
                            onAutoStop()
                            break
                        }
                    } else {
                        delay(15)
                    }
                }
            } catch (_: Throwable) {
                _isListening.value = false
            } finally {
                _micAmplitude.value = 0f
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }
        }
    }

    fun stopListeningAndRepeat(onComplete: () -> Unit, onTooShort: () -> Unit) {
        if (!_isListening.value) return
        _isListening.value = false
        scope.launch {
            recordJob?.join()
            val clip: ShortArray = synchronized(recordedSamples) {
                trimSilence(recordedSamples.toShortArray())
            }
            if (clip.size < recordSampleRate / 6) {
                _mouthAmplitude.value = 0f
                onTooShort()
                return@launch
            }
            if (!soundEnabled || audioOutputDisabled) {
                animateSilentMouth(clip.size)
                onComplete()
                return@launch
            }

            _isTalking.value = true
            playbackJob?.cancel()
            playbackJob = scope.launch {
                playbackMutex.withLock {
                    try {
                        val outRate = resolveOutputSampleRate()
                        val resampled = resampleWithPitch(clip, recordSampleRate, outRate, repeatPitchFactor)
                        val played = playPcmInternal(resampled, outRate, driveMouth = true)
                        if (!played) {
                            animateSilentMouth(clip.size)
                        }
                    } finally {
                        _mouthAmplitude.value = 0f
                        _isTalking.value = false
                        onComplete()
                    }
                }
            }
        }
    }

    private suspend fun animateSilentMouth(clipLength: Int) {
        _isTalking.value = true
        val steps = (clipLength / 640).coerceIn(6, 40)
        try {
            for (i in 0 until steps) {
                _mouthAmplitude.value = if (i % 2 == 0) 0.75f else 0.35f
                delay(65)
            }
        } finally {
            _mouthAmplitude.value = 0f
            _isTalking.value = false
        }
    }

    fun cancelVoice() {
        _isListening.value = false
        _isTalking.value = false
        _micAmplitude.value = 0f
        _mouthAmplitude.value = 0f
        recordJob?.cancel()
        playbackJob?.cancel()
    }

    private fun trimSilence(samples: ShortArray): ShortArray {
        if (samples.isEmpty()) return samples
        val threshold = 420
        var start = 0
        while (start < samples.size && abs(samples[start].toInt()) < threshold) start++
        var end = samples.size - 1
        while (end > start && abs(samples[end].toInt()) < threshold) end--
        if (end <= start) return ShortArray(0)
        val padStart = maxOf(0, start - 800)
        val padEnd = minOf(samples.size - 1, end + 1200)
        return samples.copyOfRange(padStart, padEnd + 1)
    }

    private fun resolveOutputSampleRate(): Int {
        val native = runCatching {
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        }.getOrDefault(44100)
        return if (native in 8000..48000) native else 44100
    }

    private fun resampleWithPitch(
        input: ShortArray,
        inRate: Int,
        outRate: Int,
        pitchFactor: Float
    ): ShortArray {
        if (input.isEmpty()) return input
        val step = (inRate.toDouble() / outRate.toDouble()) * pitchFactor
        val outLen = ((input.size - 1) / step).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * step
            val idx = pos.toInt().coerceIn(0, input.size - 1)
            val next = min(idx + 1, input.size - 1)
            val frac = pos - idx
            out[i] = (input[idx] * (1.0 - frac) + input[next] * frac).toInt().toShort()
        }
        return out
    }

    private suspend fun playPcmInternal(
        samples: ShortArray,
        sampleRate: Int,
        driveMouth: Boolean
    ): Boolean {
        if (samples.isEmpty() || audioOutputDisabled) return false
        val outputs = runCatching { audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
        if (outputs != null && outputs.isEmpty()) {
            audioOutputDisabled = true
            return false
        }
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            audioOutputDisabled = true
            return false
        }

        val bufferSize = maxOf(minBuf * 2, 8192)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            audioOutputDisabled = true
            return false
        }

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track.release() }
            audioOutputDisabled = true
            return false
        }

        try {
            track.play()
            val chunkSize = 1024
            var offset = 0
            while (offset < samples.size) {
                val count = min(chunkSize, samples.size - offset)
                if (driveMouth) {
                    var sum = 0.0
                    for (i in offset until offset + count) {
                        val v = samples[i].toDouble()
                        sum += v * v
                    }
                    val rms = sqrt(sum / count) / 9000.0
                    _mouthAmplitude.value = rms.toFloat().coerceIn(0.08f, 1f)
                }
                val written = track.write(samples, offset, count)
                if (written < 0) break
                offset += count
            }
            delay(60)
            return true
        } catch (_: Throwable) {
            return false
        } finally {
            if (driveMouth) _mouthAmplitude.value = 0f
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            }
            runCatching { track.release() }
        }
    }

    private fun synthesizeEffect(effect: PetSoundEffect, sampleRate: Int): ShortArray {
        return when (effect) {
            PetSoundEffect.GIGGLE -> multiChirp(sampleRate, listOf(520.0 to 0.07, 680.0 to 0.07, 820.0 to 0.08, 940.0 to 0.1))
            PetSoundEffect.BOING -> sweep(sampleRate, 240.0, 760.0, 0.24, 0.45)
            PetSoundEffect.YUMMY -> multiChirp(sampleRate, listOf(440.0 to 0.07, 554.0 to 0.07, 659.0 to 0.12))
            PetSoundEffect.BLEH -> sweep(sampleRate, 320.0, 140.0, 0.30, 0.40)
            PetSoundEffect.SPICY -> sweep(sampleRate, 900.0, 1450.0, 0.25, 0.35)
            PetSoundEffect.COIN -> multiChirp(sampleRate, listOf(987.77 to 0.06, 1318.51 to 0.14))
            PetSoundEffect.SPLASH -> multiChirp(sampleRate, listOf(600.0 to 0.05, 780.0 to 0.05, 1040.0 to 0.09))
            PetSoundEffect.KICK -> sweep(sampleRate, 190.0, 65.0, 0.13, 0.55)
            PetSoundEffect.SNORE -> sweep(sampleRate, 130.0, 110.0, 0.35, 0.20)
            PetSoundEffect.PURR -> multiChirp(sampleRate, listOf(380.0 to 0.09, 440.0 to 0.09, 520.0 to 0.12))
            PetSoundEffect.MEOW -> {
                val up = sweep(sampleRate, 460.0, 900.0, 0.13, 0.36)
                val down = sweep(sampleRate, 900.0, 520.0, 0.22, 0.34)
                ShortArray(up.size + down.size).also { out ->
                    up.copyInto(out, 0)
                    down.copyInto(out, up.size)
                }
            }
        }
    }

    private fun sweep(
        sampleRate: Int,
        startHz: Double,
        endHz: Double,
        durationSec: Double,
        volume: Double
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            val env = sin(PI * t)
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun multiChirp(sampleRate: Int, notes: List<Pair<Double, Double>>): ShortArray {
        val chunks = notes.map { (freq, dur) -> sweep(sampleRate, freq, freq * 1.06, dur, 0.36) }
        val total = chunks.sumOf { it.size }
        val out = ShortArray(total)
        var idx = 0
        for (c in chunks) {
            c.copyInto(out, idx)
            idx += c.size
        }
        return out
    }

    fun release() {
        cancelVoice()
        scope.cancel()
    }
}
