package com.example.reader

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

enum class SoundscapeType(val displayName: String, val iconName: String, val description: String) {
    OFF("Silent", "VolumeOff", "No ambient soundscape"),
    RAIN("Rain on Glass", "WaterDrop", "Gentle rain and soft distant rolling drops"),
    FIREPLACE("Cozy Hearth", "LocalFireDepartment", "Warm crackling fireplace embers"),
    CAFE("Cozy Library Cafe", "Coffee", "Subtle ambient room resonance & warmth"),
    FOREST("Night Forest", "Forest", "Gentle evening breeze & night crickets"),
    BINAURAL_ALPHA("Alpha Waves (40Hz)", "Psychology", "Harmonic focus tones for deep reading flow")
}

class AmbientAudioEngine {

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentSoundscape = MutableStateFlow(SoundscapeType.OFF)
    val currentSoundscape: StateFlow<SoundscapeType> = _currentSoundscape.asStateFlow()

    private val _volume = MutableStateFlow(0.6f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _timerMinutesRemaining = MutableStateFlow(0)
    val timerMinutesRemaining: StateFlow<Int> = _timerMinutesRemaining.asStateFlow()

    private var timerJob: Job? = null

    fun setSoundscape(type: SoundscapeType) {
        if (_currentSoundscape.value == type) return
        stopSoundscape()
        _currentSoundscape.value = type
        if (type != SoundscapeType.OFF) {
            startSoundscape(type)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        audioTrack?.setVolume(clamped)
    }

    fun setSleepTimer(minutes: Int) {
        timerJob?.cancel()
        _timerMinutesRemaining.value = minutes
        if (minutes <= 0) return

        timerJob = scope.launch {
            while (_timerMinutesRemaining.value > 0) {
                delay(60_000L)
                _timerMinutesRemaining.value = _timerMinutesRemaining.value - 1
            }
            // Timer expired -> fade out soundscape
            fadeAndStop()
        }
    }

    private fun fadeAndStop() {
        scope.launch {
            val startVol = _volume.value
            for (step in 10 downTo 0) {
                audioTrack?.setVolume((startVol * step) / 10f)
                delay(150)
            }
            setSoundscape(SoundscapeType.OFF)
            _volume.value = startVol
        }
    }

    private fun startSoundscape(type: SoundscapeType) {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(_volume.value)
            audioTrack?.play()

            job = scope.launch {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var phase = 0.0
                var phase2 = 0.0
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                var b3 = 0.0
                var b4 = 0.0
                var b5 = 0.0
                var b6 = 0.0

                while (isActive) {
                    when (type) {
                        SoundscapeType.RAIN -> {
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                // Pink noise filter
                                b0 = 0.99886 * b0 + white * 0.0555179
                                b1 = 0.99332 * b1 + white * 0.0750759
                                b2 = 0.96900 * b2 + white * 0.1538520
                                b3 = 0.86650 * b3 + white * 0.3104856
                                b4 = 0.55000 * b4 + white * 0.5329522
                                b5 = -0.7616 * b5 - white * 0.0168980
                                val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.06

                                // Random raindrop accents
                                val drop = if (random.nextInt(600) == 0) (random.nextDouble() * 0.4) else 0.0
                                val sample = (pink + drop).coerceIn(-1.0, 1.0)
                                buffer[i] = (sample * 16000).toInt().toShort()
                            }
                        }
                        SoundscapeType.FIREPLACE -> {
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                b0 = 0.992 * b0 + white * 0.08
                                val brown = b0 * 0.12

                                // Fire crackle pop
                                val crackle = if (random.nextInt(400) == 0) {
                                    (random.nextDouble() * 1.5 - 0.75)
                                } else 0.0
                                val sample = (brown + crackle).coerceIn(-1.0, 1.0)
                                buffer[i] = (sample * 18000).toInt().toShort()
                            }
                        }
                        SoundscapeType.CAFE -> {
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                b0 = 0.995 * b0 + white * 0.04
                                b1 = 0.985 * b1 + white * 0.03
                                val murmur = (b0 + b1) * 0.15
                                phase += 2.0 * PI * 180.0 / sampleRate
                                val warmHum = sin(phase) * 0.05
                                val sample = (murmur + warmHum).coerceIn(-1.0, 1.0)
                                buffer[i] = (sample * 14000).toInt().toShort()
                            }
                        }
                        SoundscapeType.FOREST -> {
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0)
                                b0 = 0.997 * b0 + white * 0.02
                                val wind = b0 * 0.1

                                // Crickets high harmonic pulse
                                phase += 2.0 * PI * 4200.0 / sampleRate
                                val cricket = if ((i / 4000) % 3 == 0) sin(phase) * 0.04 else 0.0
                                val sample = (wind + cricket).coerceIn(-1.0, 1.0)
                                buffer[i] = (sample * 14000).toInt().toShort()
                            }
                        }
                        SoundscapeType.BINAURAL_ALPHA -> {
                            for (i in buffer.indices) {
                                // 200 Hz base carrier + 240 Hz harmonic (40 Hz difference)
                                phase += 2.0 * PI * 200.0 / sampleRate
                                phase2 += 2.0 * PI * 240.0 / sampleRate
                                val tone1 = sin(phase) * 0.25
                                val tone2 = sin(phase2) * 0.25
                                val sample = (tone1 + tone2).coerceIn(-1.0, 1.0)
                                buffer[i] = (sample * 20000).toInt().toShort()
                            }
                        }
                        SoundscapeType.OFF -> {
                            buffer.fill(0)
                        }
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSoundscape() {
        job?.cancel()
        job = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    fun release() {
        timerJob?.cancel()
        stopSoundscape()
        scope.cancel()
    }
}
