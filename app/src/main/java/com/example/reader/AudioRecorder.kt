package com.example.reader

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    suspend fun recordAudioClip(durationSeconds: Int = 3): FloatArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext FloatArray(0)
            }

            audioRecord?.startRecording()
            isRecording = true

            val totalSamples = SAMPLE_RATE * durationSeconds
            val audioBuffer = ShortArray(totalSamples)
            var samplesRead = 0

            while (isRecording && samplesRead < totalSamples) {
                val read = audioRecord?.read(
                    audioBuffer,
                    samplesRead,
                    totalSamples - samplesRead
                ) ?: -1
                if (read > 0) {
                    samplesRead += read
                } else {
                    break
                }
            }

            stop()

            // Convert 16-bit PCM shorts to normalized float array [-1.0, 1.0]
            val floatArray = FloatArray(samplesRead)
            for (i in 0 until samplesRead) {
                floatArray[i] = audioBuffer[i] / 32768.0f
            }

            return@withContext floatArray
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
            return@withContext FloatArray(0)
        }
    }

    fun stop() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
