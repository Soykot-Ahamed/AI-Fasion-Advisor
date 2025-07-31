package oops.java.systemanalysis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FashionChatbotScreen(navController: NavHostController) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var geminiResponse by remember { mutableStateOf("") }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    // Initialize TTS
    DisposableEffect(Unit) {
        val ttsObj = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = ttsObj

        onDispose {
            ttsObj.stop()
            ttsObj.shutdown()
        }
    }

    // Initialize SpeechRecognizer
    DisposableEffect(Unit) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                    recognizedText = text
                    if (isFashionRelated(text)) {
                        askGemini(text) { response ->
                            geminiResponse = response
                            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    } else {
                        geminiResponse = "Sorry, please ask fashion-related questions."
                        tts?.speak("Sorry, please ask fashion-related questions.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                    isListening = false
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    geminiResponse = "Sorry, I didn't catch that. Please try again."
                    tts?.speak("Sorry, I didn't catch that. Try again.", TextToSpeech.QUEUE_FLUSH, null, null)
                    isListening = false
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        speechRecognizer = recognizer

        onDispose {
            recognizer.destroy()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            geminiResponse = "Microphone permission is required for voice input"
        }
    }

    // Check for permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fashion Chatbot") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF69B4),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Fashion Voice Assistant",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFF69B4),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Ask any fashion-related question",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask a fashion question")
                    }
                    isListening = true
                    speechRecognizer?.startListening(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF69B4),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(if (isListening) "Listening..." else "Tap to Speak", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEDF5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Question:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF69B4),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(recognizedText)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (geminiResponse.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEDF5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Response:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF69B4),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(geminiResponse)
                    }
                }
            }
        }
    }
}

private fun isFashionRelated(text: String): Boolean {
    val keywords = listOf(
        "fashion", "style", "dress", "outfit", "wear", "clothes", "skirt",
        "shirt", "suit", "color", "jeans", "shoes", "accessories", "trend", "outfits",
        "pants", "top", "jacket", "coat", "blouse", "sweater", "dresses", "shorts",
        "seasonal", "occasion", "formal", "casual", "business", "party", "wedding"
    )
    return keywords.any { text.contains(it, ignoreCase = true) }
}

private fun askGemini(query: String, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Replace with your actual Gemini API key
            val apiKey = apiKey
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {"text": "You are a fashion expert assistant. Provide helpful, concise fashion advice."},
                        {"text": "$query"}
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.7,
                    "topP": 0.9,
                    "maxOutputTokens": 1024
                  }
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val candidates = json.optJSONArray("candidates")
            val content = candidates?.getJSONObject(0)?.getJSONObject("content")
            val parts = content?.getJSONArray("parts")
            val textPart = parts?.getJSONObject(0)
            val reply = textPart?.getString("text") ?: "Sorry, I couldn't understand that."

            onResult(reply)
        } catch (e: Exception) {
            onResult("Sorry, I couldn't process your request. Please try again later. Error: ${e.message}")
        }
    }
}
