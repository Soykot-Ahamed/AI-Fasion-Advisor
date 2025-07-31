package oops.java.systemanalysis

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.InputStream
import kotlin.math.abs

@Composable
fun BodyShapeAnalyzerScreen(navController: NavHostController) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf("") }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val stream: InputStream? = context.contentResolver.openInputStream(it)
            bitmap = android.graphics.BitmapFactory.decodeStream(stream)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Upload a Full Body Photo", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.height(300.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                analyzeBodyShape(it) { result ->
                    analysisResult = result
                }
            }) {
                Text("Analyze Body Shape")
            }
        }

        if (analysisResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Result:", style = MaterialTheme.typography.titleMedium)
            Text(analysisResult)
        }
    }
}

fun analyzeBodyShape(bitmap: Bitmap, onResult: (String) -> Unit) {
    val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build()
    val detector = PoseDetection.getClient(options)

    val image = InputImage.fromBitmap(bitmap, 0)
    detector.process(image)
        .addOnSuccessListener { pose ->
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val leftWaist = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) // Approx waist
            val rightWaist = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW) // Approx waist

            if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
                val shoulderWidth = distance(leftShoulder.position.x, rightShoulder.position.x)
                val hipWidth = distance(leftHip.position.x, rightHip.position.x)
                val waistWidth = if (leftWaist != null && rightWaist != null)
                    distance(leftWaist.position.x, rightWaist.position.x)
                else (shoulderWidth + hipWidth) / 2

                val shape = when {
                    shoulderWidth > hipWidth + 30 -> "Inverted Triangle: Choose V-neck and flared skirts."
                    hipWidth > shoulderWidth + 30 -> "Pear Shape: Use A-line skirts and shoulder-enhancing tops."
                    abs(shoulderWidth - hipWidth) < 20 && abs(waistWidth - shoulderWidth) > 20 ->
                        "Hourglass: Use fitted clothes that define the waist."
                    else -> "Rectangle: Try belts, ruffles, and layers to create curves."
                }

                onResult(
                    "Shoulder: ${shoulderWidth.toInt()} px\n" +
                            "Waist: ${waistWidth.toInt()} px\n" +
                            "Hip: ${hipWidth.toInt()} px\n\n" +
                            "Suggested Shape: $shape"
                )
            } else {
                onResult("Couldn't detect body properly. Try another photo.")
            }
        }
        .addOnFailureListener {
            onResult("Pose detection failed. Try again.")
        }
}

fun distance(x1: Float, x2: Float): Float = abs(x2 - x1)
