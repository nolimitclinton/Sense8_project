package com.example.sense8.presentation.mainActivity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.sense8.presentation.navgraph.NavGraph
import com.example.sense8.ui.theme.Sense8Theme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var textToSpeechEngine: TextToSpeech
    var userChoice: String? by mutableStateOf(null)

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            userChoice = results?.get(0)?.lowercase() ?: ""

            if (userChoice in listOf("navigation", "object detection")) {
                viewModel.userHasChosen = true
                Log.d("MainActivity", "Recognized text: $userChoice, userHasChosen set to true")
            } else {
                handleInvalidResponse() // Handle invalid response
            }
        } else {
            Log.e("MainActivity", "Speech recognition failed with result code: ${result.resultCode}")
            handleInvalidResponse() // Handle recognition failure
        }
    }

    private fun handleInvalidResponse() {
        textToSpeechEngine.speak(
            "I didn't catch that. Please say 'navigation' or 'object detection'.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            startSpeechRecognition()
        }, 4000) // Adjust delay if necessary
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.redirectFlagState }
        }

        textToSpeechEngine = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine.language = Locale.getDefault()
                textToSpeechEngine.speak(
                    "Welcome to Sense8. Would you like to start 'navigation' or 'object detection'?",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    startSpeechRecognition()
                }, 7000) // Delay for the prompt
            }
        }

        setContent {
            Sense8Theme {
                NavGraph(
                    startDestination = "waiting_screen", // Start with a waiting screen
                    userChoice = userChoice,
                    viewModel = viewModel,
                    context = this@MainActivity,
                    onUserChoice = { choice ->
                        userChoice = choice
                        viewModel.userHasChosen = true
                        Log.d("MainActivity", "onUserChoice triggered with choice: $choice")
                    }
                )
            }
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'navigation' or 'object detection'")
        }
        speechRecognizerLauncher.launch(intent)
    }
}
