package com.example.sense8.presentation.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.sense8.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun NavigationScreen() {
    val context = LocalContext.current
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var directionsSteps by remember { mutableStateOf<List<Step>>(emptyList()) }
    val cameraPositionState = rememberCameraPositionState()

    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val textToSpeechEngine = remember { mutableStateOf<TextToSpeech?>(null) }
    val isTtsReady = remember { mutableStateOf(false) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val latLng = LatLng(location.latitude, location.longitude)
                currentLocation = latLng
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )

                if (directionsSteps.isNotEmpty()) {
                    val nextStep = directionsSteps.getOrNull(currentStepIndex) ?: return
                    val nextLatLng = nextStep.polylinePoints.last()

                    if (isUserCloseToPoint(latLng, nextLatLng)) {
                        textToSpeechEngine.value?.speak(
                            nextStep.instruction,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "DIRECTION_STEP"
                        )
                        currentStepIndex++
                        if (currentStepIndex >= directionsSteps.size) {
                            textToSpeechEngine.value?.speak(
                                "You have reached your destination.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ARRIVAL"
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionsState.allPermissionsGranted) {
            fusedLocationProviderClient.requestLocationUpdates(
                LocationRequest.create().apply {
                    interval = 2000
                    fastestInterval = 1000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                },
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    // Handling results from voice recognition
    val startForResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleVoiceRecognitionResult(
                result.data,
                context,
                textToSpeechEngine.value,
                currentLocation
            ) { destinationLatLng ->
                destinationLocation = destinationLatLng
                currentLocation?.let {
                    fetchDirections(context, it, destinationLatLng, textToSpeechEngine.value) { directions ->
                        polylinePoints = directions.polylinePoints
                        directionsSteps = directions.steps
                        currentStepIndex = 0
                    }
                }
            }
        } else {
            textToSpeechEngine.value?.speak("I couldn't hear you. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "RETRY_PROMPT")
        }
    }

    // Initialize TextToSpeech engine
    LaunchedEffect(Unit) {
        textToSpeechEngine.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine.value?.language = Locale.getDefault()
                isTtsReady.value = true
                startVoiceRecognition(context, startForResult)  // Trigger the prompt here after TTS is ready
            } else {
                Log.e("NavigationScreen", "Failed to initialize TextToSpeech")
            }
        }
    }

    if (locationPermissionsState.allPermissionsGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                currentLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Your Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                }

                destinationLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Destination"
                    )
                    DrawRoute(currentLocation!!, it, polylinePoints)
                }
            }
        }
    } else {
        // Display message for permission issues
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val allPermissionsRevoked =
                locationPermissionsState.permissions.size == locationPermissionsState.revokedPermissions.size

            if (allPermissionsRevoked) {
                Button(onClick = {
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts(
                                "package",
                                context.packageName,
                                null
                            )
                        }
                    context.startActivity(intent)
                }) {
                    Text("Grant permissions in settings")
                }
            } else {
                Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Request location permissions")
                }
            }
        }
    }
}

fun isUserCloseToPoint(userLocation: LatLng, targetLocation: LatLng): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        targetLocation.latitude, targetLocation.longitude, results
    )
    return results[0] < 20  // 20 meters
}

private fun fetchDirections(
    context: Context,
    currentLocation: LatLng,
    destinationLatLng: LatLng,
    ttsEngine: TextToSpeech?,
    onDirectionsFetched: (Directions) -> Unit
) {
    val url = calculateDirections(context, currentLocation, destinationLatLng)
    try {
        // Fetch directions asynchronously
        val response = kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.IO) {
                java.net.URL(url).readText()
            }
        }
        val directions = parseDirections(response)
        directions?.let {
            ttsEngine?.speak(
                "Your route is ready. The estimated time of arrival is ${directions.eta}.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ROUTE_READY"
            )
            onDirectionsFetched(directions)
        } ?: run {
            ttsEngine?.speak("Unable to fetch directions. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "DIRECTIONS_ERROR")
        }
    } catch (e: Exception) {
        ttsEngine?.speak("An error occurred while fetching directions. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "DIRECTIONS_ERROR")
    }
}



private fun startVoiceRecognition(
    context: Context,
    startForResult: ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the location")
    }
    startForResult.launch(intent)
}

private fun handleVoiceRecognitionResult(
    data: Intent?,
    context: Context,
    ttsEngine: TextToSpeech?,
    currentLocation: LatLng?,
    onDestinationDetected: (LatLng) -> Unit
) {
    val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
    result?.let {
        val destination = it[0]
        ttsEngine?.speak("Searching for $destination", TextToSpeech.QUEUE_FLUSH, null, "SEARCHING_LOCATION")
        currentLocation?.let { latLng ->
            startGeocoding(destination, context, ttsEngine, latLng, onDestinationDetected)
        }
    }
}

private fun startGeocoding(
    destination: String,
    context: Context,
    ttsEngine: TextToSpeech?,
    currentLocation: LatLng,
    onDestinationDetected: (LatLng) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocationName(destination, 1)

    if (addresses.isNullOrEmpty()) {
        ttsEngine?.speak("Location not found. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "LOCATION_NOT_FOUND")
    } else {
        val destinationLatLng = LatLng(addresses[0].latitude, addresses[0].longitude)
        ttsEngine?.speak("Found $destination. Calculating route.", TextToSpeech.QUEUE_FLUSH, null, "LOCATION_FOUND")
        onDestinationDetected(destinationLatLng)
    }
}



private fun speakDirections(ttsEngine: TextToSpeech?, steps: List<Step>) {
    for (step in steps) {
        ttsEngine?.speak(step.instruction, TextToSpeech.QUEUE_ADD, null, null)
    }
}

@Composable
fun DrawRoute(
    currentLocation: LatLng,
    destination: LatLng,
    polylinePoints: List<LatLng>
) {
    if (polylinePoints.isNotEmpty()) {
        com.google.maps.android.compose.Polyline(
            points = polylinePoints,
            color = androidx.compose.ui.graphics.Color.Blue
        )
    }
}

private fun calculateDirections(
    context: Context,
    currentLocation: LatLng,
    destinationLatLng: LatLng
): String {
    val apiKey = context.getString(R.string.google_maps_key)
    return "https://maps.googleapis.com/maps/api/directions/json?origin=${currentLocation.latitude},${currentLocation.longitude}&destination=${destinationLatLng.latitude},${destinationLatLng.longitude}&key=$apiKey"
}

private fun parseDirections(response: String): Directions? {
    val jsonResponse = org.json.JSONObject(response)
    val routes = jsonResponse.getJSONArray("routes")
    if (routes.length() == 0) {
        return null
    }

    val route = routes.getJSONObject(0)
    val legs = route.getJSONArray("legs")
    val leg = legs.getJSONObject(0)

    val eta = leg.getJSONObject("duration").getString("text")
    val stepsJson = leg.getJSONArray("steps")

    val steps = mutableListOf<Step>()
    for (i in 0 until stepsJson.length()) {
        val step = stepsJson.getJSONObject(i)
        val instruction = step.getString("html_instructions").replace("<[^>]*>".toRegex(), "")
        val polyline = step.getJSONObject("polyline").getString("points")
        steps.add(Step(instruction, decodePolyline(polyline)))
    }

    val polylinePoints = route.getJSONObject("overview_polyline").getString("points")

    return Directions(eta, steps, decodePolyline(polylinePoints))
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }

    return poly
}

data class Directions(
    val eta: String,
    val steps: List<Step>,
    val polylinePoints: List<LatLng>
)

data class Step(
    val instruction: String,
    val polylinePoints: List<LatLng>
)
