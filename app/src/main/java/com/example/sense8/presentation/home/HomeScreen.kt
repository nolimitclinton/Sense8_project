package com.example.sense8.presentation.home

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sense8.R
import com.example.sense8.data.manager.objectDetection.ObjectDetectionManagerImpl
import com.example.sense8.domain.model.Detection
import com.example.sense8.presentation.common.ImageButton
import com.example.sense8.presentation.home.components.CameraOverlay
import com.example.sense8.presentation.home.components.CameraPreview
import com.example.sense8.presentation.home.components.ObjectCounter
import com.example.sense8.presentation.home.components.RequestPermissions
import com.example.sense8.presentation.home.components.ThresholdLevelSlider
import com.example.sense8.presentation.utils.Dimens
import com.example.sense8.presentation.utils.Routes
import com.example.sense8.utils.CameraFrameAnalyzer
import com.example.sense8.utils.Constants
import com.example.sense8.utils.ImageScalingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel()

    RequestPermissions()

    val isImageSavedStateFlow by viewModel.isImageSavedStateFlow.collectAsState()
    val previewSizeState = remember { mutableStateOf(IntSize(0, 0)) }
    var boundingBoxCoordinatesState = remember { mutableStateListOf<RectF>() }
    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }

    var scaleFactorX = 1f
    var scaleFactorY = 1f

    // Initialize TextToSpeech
    var textToSpeechEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    // State to hold the recognized command
    var recognizedCommand by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        textToSpeechEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine?.language = Locale.getDefault()
            }
        }

        startPersistentVoiceCommandListener(context) { command ->
            recognizedCommand = command
        }
    }

    // Handle navigation based on the recognized command
    LaunchedEffect(recognizedCommand) {
        recognizedCommand?.let { command ->
            when (command.lowercase(Locale.getDefault())) {
                "navigation" -> {
                    textToSpeechEngine?.shutdown()  // Stop TTS before navigating
                    // Optionally, stop the SpeechRecognizer if needed
                    stopVoiceCommandListener()

                    navController.navigate(Routes.ROUTE_NAVIGATION_SCREEN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                "object detection" -> {
                    navController.navigate("default_object_detection_screen") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                else -> {
                    Toast.makeText(context, "Unrecognized command", Toast.LENGTH_SHORT).show()
                }
            }
            recognizedCommand = null // Reset after handling
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        var detections by remember { mutableStateOf(emptyList<Detection>()) }

        LaunchedEffect(detections) {
            if (detections.isNotEmpty()) {
                val objectNames = detections.joinToString(separator = ", ") { it.detectedObjectName }
                textToSpeechEngine?.speak("Detected: $objectNames", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        val cameraFrameAnalyzer = remember {
            CameraFrameAnalyzer(
                objectDetectionManager = ObjectDetectionManagerImpl(context),
                onObjectDetectionResults = { detectionResults ->
                    // Move detection results processing to a background thread
                    CoroutineScope(Dispatchers.Default).launch {
                        detections = detectionResults
                        boundingBoxCoordinatesState.clear()
                        detections.forEach { detection ->
                            boundingBoxCoordinatesState.add(detection.boundingBox)
                        }
                        withContext(Dispatchers.Main) {
                            // Update UI-related state on the main thread
                            boundingBoxCoordinatesState = boundingBoxCoordinatesState
                        }
                    }
                },
                confidenceScoreState = confidenceScoreState
            )
        }

        val cameraController by remember {
            mutableStateOf(viewModel.prepareCameraController(context, cameraFrameAnalyzer))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.gray_900))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            ) {
                CameraPreview(
                    controller = remember { cameraController },
                    modifier = Modifier.fillMaxSize(),
                    onPreviewSizeChanged = { newSize ->
                        previewSizeState.value = newSize
                        val scaleFactors = ImageScalingUtils.getScaleFactors(newSize.width, newSize.height)
                        scaleFactorX = scaleFactors[0]
                        scaleFactorY = scaleFactors[1]
                    }
                )
                CameraOverlay(detections = detections)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .padding(top = Dimens.Padding8dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                ImageButton(
                    drawableResourceId = R.drawable.ic_capture,
                    contentDescriptionResourceId = R.string.capture_button_description,
                    modifier = Modifier
                        .size(Dimens.CaptureButtonSize)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            // Capture photo on a background thread
                            CoroutineScope(Dispatchers.IO).launch {
                                viewModel.capturePhoto(
                                    context,
                                    cameraController,
                                    context.resources.displayMetrics.widthPixels * 1f,
                                    context.resources.displayMetrics.heightPixels * 1f,
                                    detections
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (isImageSavedStateFlow) R.string.success_image_saved_message else R.string.error_image_saved_message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                )
                val sliderValue = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }
                ThresholdLevelSlider(sliderValue) { confidenceScoreState.floatValue = it }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = Dimens.Padding32dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ImageButton(
                    drawableResourceId = R.drawable.ic_rotate_camera,
                    contentDescriptionResourceId = R.string.rotate_camera_button_description,
                    Modifier
                        .padding(
                            top = Dimens.Padding24dp,
                            start = Dimens.Padding16dp
                        )
                        .size(Dimens.RotateCameraButtonSize)
                        .clickable {
                            cameraController.cameraSelector =
                                viewModel.getSelectedCamera(cameraController)
                        }
                )
                ObjectCounter(objectCount = detections.size)
            }
        }
    }

    // Ensure TextToSpeech is shut down when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            textToSpeechEngine?.shutdown()
        }
    }
}
private fun startPersistentVoiceCommandListener(
    context: Context,
    onCommandRecognized: (String?) -> Unit
) {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("SpeechRecognizer", "Ready for speech")
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            // Restart the listening process to make it persistent
            speechRecognizer.startListening(intent)
        }

        override fun onError(error: Int) {
            Log.e("SpeechRecognizer", "Error: $error")
            // Restart listening on error to avoid shutting down
            speechRecognizer.startListening(intent)
        }

        override fun onResults(results: Bundle?) {
            val command = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
            if (command.equals("navigation", ignoreCase = true) || command.equals("object detection", ignoreCase = true)) {
                onCommandRecognized(command)
            }
            // Restart listening after processing the results
            speechRecognizer.startListening(intent)
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    // Start the listener for the first time
    speechRecognizer.startListening(intent)
}
private fun stopVoiceCommandListener() {
    // Implement logic to stop and release the SpeechRecognizer instance if needed
}