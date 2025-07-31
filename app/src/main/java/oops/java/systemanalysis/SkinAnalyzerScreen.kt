package oops.java.systemanalysis

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.ExperimentalGetImage


@Composable
fun SkinAnalyzerUI(navController: NavHostController) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    var faceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf("Ready to analyze") }
    var suggestion by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Text(
            text = "Face Problem Detector",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CameraPreview(
                onFaceCaptured = { bitmap, faceRect ->
                    isAnalyzing = true
                    faceBitmap = bitmap
                    val (res, sugg) = analyzeSkin(bitmap)
                    result = res
                    suggestion = sugg
                    isAnalyzing = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        faceBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Face analysis",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Analysis Result:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(result)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recommendation:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(suggestion)
            }
        }
    }
}

@Composable
fun CameraPreview(
    onFaceCaptured: (Bitmap, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }

    AndroidView(
        factory = { ctx ->
            androidx.camera.view.PreviewView(ctx).apply {
                controller = cameraController
                cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                cameraController.setImageAnalysisAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    FaceAnalyzer(onFaceCaptured)
                )
            }
        },
        modifier = modifier
    )
}



@ExperimentalGetImage
class FaceAnalyzer(
    private val onFaceDetected: (Bitmap, Rect) -> Unit
) : ImageAnalysis.Analyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces.first().boundingBox
                    val bitmap = Bitmap.createBitmap(
                        inputImage.width, inputImage.height, Bitmap.Config.ARGB_8888
                    ).apply {
                        copyPixelsFromBuffer(mediaImage.planes[0].buffer)
                    }
                    onFaceDetected(bitmap, face)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

private fun analyzeSkin(bitmap: Bitmap): Pair<String, String> {
    val resized = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
    var totalR = 0
    var totalG = 0
    var totalB = 0
    var count = 0

    for (x in 0 until resized.width) {
        for (y in 0 until resized.height) {
            val pixel = resized.getPixel(x, y)
            totalR += (pixel shr 16) and 0xFF
            totalG += (pixel shr 8) and 0xFF
            totalB += pixel and 0xFF
            count++
        }
    }

    val avgR = totalR / count
    val avgG = totalG / count
    val avgB = totalB / count

    return when {
        avgR > 170 && avgG < 130 -> "Redness/Acne detected" to "Use salicylic acid cleanser and apply aloe vera gel to soothe inflammation"
        avgB > 160 && avgG > 160 -> "Dry/Pale skin detected" to "Use a hydrating moisturizer and drink more water. Consider a humidifier"
        avgG > 160 && avgB < 100 -> "Oily skin detected" to "Use oil-free products and clay masks 2-3 times per week"
        else -> "Normal skin detected" to "Maintain your routine with daily SPF and proper hydration"
    }
}
