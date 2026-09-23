package com.example.reader

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed class FacePresenceState {
    object Disabled : FacePresenceState()
    object PermissionRequired : FacePresenceState()
    object CameraUnavailable : FacePresenceState()
    object Detecting : FacePresenceState()
    object Attentive : FacePresenceState() // A real face is detected and roughly facing the screen
    object NoFace : FacePresenceState()
    object MultipleFaces : FacePresenceState()
    data class Error(val message: String) : FacePresenceState()
}

/**
 * On-device face presence detection for the "face-assisted reading timer".
 *
 * Detection runs entirely on-device with ML Kit's Face Detection API:
 *  - The model ships inside the app (bundled), so no network and no Google account is needed.
 *  - Frames are analysed and immediately closed. No image is ever stored, cached, or
 *    transmitted; the engine only exposes a presence state.
 *  - "Attentive" means a real face was detected whose Euler Y/X rotation keeps it roughly
 *    pointed at the screen for two consecutive analyses.
 */
class FacePresenceEngine(private val context: Context) {

    private val _presenceState = MutableStateFlow<FacePresenceState>(FacePresenceState.Disabled)
    val presenceState: StateFlow<FacePresenceState> = _presenceState.asStateFlow()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var faceDetector: FaceDetector? = null
    private var isStopped = false

    private var consecutiveFaceFrames = 0
    private var consecutiveNoFaceFrames = 0

    fun startAnalyzing(lifecycleOwner: LifecycleOwner) {
        stopAnalyzing()
        isStopped = false
        _presenceState.value = FacePresenceState.Detecting
        consecutiveFaceFrames = 0
        consecutiveNoFaceFrames = 0

        val executor = Executors.newSingleThreadExecutor()
        cameraExecutor = executor

        faceDetector = buildDetector()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                if (cameraExecutor !== executor || isStopped) {
                    // Cancelled or restarted before the future completed.
                    return@addListener
                }
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(480, 360))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    analyzeFrame(imageProxy)
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageAnalysis
                )
            } catch (_: Exception) {
                _presenceState.value = FacePresenceState.CameraUnavailable
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun buildDetector(): FaceDetector {
        // FAST mode with landmark detection disabled: presence + orientation only, ~real time.
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.20f)
            .enableTracking()
            .build()
        return FaceDetection.getClient(options)
    }

    private fun analyzeFrame(image: ImageProxy) {
        val detector = faceDetector
        if (detector == null || isStopped) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }

        val rotation = image.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (isStopped) return@addOnSuccessListener
                when {
                    faces.isEmpty() -> onNoFace()
                    faces.size > 1 -> {
                        // More than one face: ambiguous attention; require single-reader focus.
                        _presenceState.value = FacePresenceState.MultipleFaces
                        consecutiveFaceFrames = 0
                    }
                    else -> onFaceDetected(faces.first())
                }
            }
            .addOnFailureListener {
                if (!isStopped) {
                    _presenceState.value = FacePresenceState.Error("Face analysis failed on this frame.")
                }
            }
            .addOnCompleteListener { image.close() }
    }

    private fun onFaceDetected(face: com.google.mlkit.vision.face.Face) {
        consecutiveFaceFrames++
        consecutiveNoFaceFrames = 0

        // Attentive = face present and head kept within ±35° of screen-facing on both axes.
        val roughlyFacingScreen =
            Math.abs(face.headEulerAngleY) <= 35f && Math.abs(face.headEulerAngleX) <= 35f

        _presenceState.value = if (consecutiveFaceFrames >= 2 && roughlyFacingScreen) {
            FacePresenceState.Attentive
        } else {
            FacePresenceState.Detecting
        }
    }

    private fun onNoFace() {
        consecutiveNoFaceFrames++
        consecutiveFaceFrames = 0
        if (consecutiveNoFaceFrames >= 4) {
            _presenceState.value = FacePresenceState.NoFace
        } else {
            _presenceState.value = FacePresenceState.Detecting
        }
    }

    fun stopAnalyzing() {
        isStopped = true
        try {
            faceDetector?.close()
        } catch (_: Exception) {}
        faceDetector = null
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        cameraExecutor?.shutdown()
        cameraExecutor = null
        cameraProvider = null
        _presenceState.value = FacePresenceState.Disabled
    }
}
