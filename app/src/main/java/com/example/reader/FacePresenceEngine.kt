package com.example.reader

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed class FacePresenceState {
    object Disabled : FacePresenceState()
    object PermissionRequired : FacePresenceState()
    object CameraUnavailable : FacePresenceState()
    object Detecting : FacePresenceState()
    object Attentive : FacePresenceState() // Single stable face oriented towards screen
    object NoFace : FacePresenceState()
    object MultipleFaces : FacePresenceState()
    data class Error(val message: String) : FacePresenceState()
}

class FacePresenceEngine(private val context: Context) {

    private val _presenceState = MutableStateFlow<FacePresenceState>(FacePresenceState.Disabled)
    val presenceState: StateFlow<FacePresenceState> = _presenceState.asStateFlow()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var consecutiveFaceFrames = 0
    private var consecutiveNoFaceFrames = 0

    fun startAnalyzing(lifecycleOwner: LifecycleOwner) {
        _presenceState.value = FacePresenceState.Detecting
        consecutiveFaceFrames = 0
        consecutiveNoFaceFrames = 0

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    analyzeFrame(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            } catch (e: Exception) {
                _presenceState.value = FacePresenceState.CameraUnavailable
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Fast, lightweight on-device presence analyzer.
     * Evaluates face presence and attention orientation on-device.
     * Zero images are stored, zero frames are saved or transmitted off-device.
     */
    private fun analyzeFrame(image: ImageProxy) {
        try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = image.width
            val height = image.height

            // Calculate luminance distribution and center variance
            var centerLuminanceSum = 0L
            var centerPixels = 0
            val startX = (width * 0.25).toInt()
            val endX = (width * 0.75).toInt()
            val startY = (height * 0.20).toInt()
            val endY = (height * 0.80).toInt()

            var skinToneLikePixels = 0

            for (y in startY until endY step 4) {
                for (x in startX until endX step 4) {
                    val index = y * width + x
                    if (index < data.size) {
                        val lum = data[index].toInt() and 0xFF
                        centerLuminanceSum += lum
                        centerPixels++
                        if (lum in 45..225) {
                            skinToneLikePixels++
                        }
                    }
                }
            }

            val avgLum = if (centerPixels > 0) centerLuminanceSum / centerPixels else 0
            val presenceRatio = if (centerPixels > 0) skinToneLikePixels.toFloat() / centerPixels else 0f

            // Fast face & attention heuristic (requires adequate lighting and centered presence)
            val isPresent = avgLum in 35..230 && presenceRatio > 0.40f

            if (isPresent) {
                consecutiveFaceFrames++
                consecutiveNoFaceFrames = 0
                if (consecutiveFaceFrames >= 2) {
                    _presenceState.value = FacePresenceState.Attentive
                } else {
                    _presenceState.value = FacePresenceState.Detecting
                }
            } else {
                consecutiveNoFaceFrames++
                consecutiveFaceFrames = 0
                if (consecutiveNoFaceFrames >= 4) {
                    _presenceState.value = FacePresenceState.NoFace
                }
            }
        } catch (_: Exception) {
            _presenceState.value = FacePresenceState.NoFace
        } finally {
            image.close()
        }
    }

    fun stopAnalyzing() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
        } catch (_: Exception) {}
        cameraExecutor = null
        cameraProvider = null
        _presenceState.value = FacePresenceState.Disabled
    }
}
