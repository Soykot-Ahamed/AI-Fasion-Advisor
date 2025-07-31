package oops.java.systemanalysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.InputStream
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ColorAnalysisScreen(navController: NavHostController) {
    FaceColorApp()
}

@Composable
fun FaceColorApp() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var faceColor by remember { mutableStateOf(Color.Gray) }
    var detectedColorName by remember { mutableStateOf("") }
    var outfitSuggestions by remember { mutableStateOf(listOf<String>()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            imageUri = uri
            uri?.let {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    val image = InputImage.fromBitmap(bitmap, 0)
                    val options = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build()

                    val detector = FaceDetection.getClient(options)
                    detector.process(image)
                        .addOnSuccessListener { faces ->
                            if (faces.isNotEmpty()) {
                                val face: Rect = faces[0].boundingBox
                                val faceBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    face.left.coerceAtLeast(0),
                                    face.top.coerceAtLeast(0),
                                    face.width().coerceAtMost(bitmap.width - face.left),
                                    face.height().coerceAtMost(bitmap.height - face.top)
                                )
                                val pixels = getPixels(faceBitmap)
                                val dominant = kMeans(pixels)
                                faceColor = dominant

                                val dominantRgb = intArrayOf(
                                    (dominant.red * 255).toInt(),
                                    (dominant.green * 255).toInt(),
                                    (dominant.blue * 255).toInt()
                                )
                                detectedColorName = getClosestColorName(dominantRgb)
                                outfitSuggestions = getOutfitSuggestions(detectedColorName)
                            }
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Upload Your Photo")
        }

        imageUri?.let { uri ->

                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )

        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(faceColor)
        )

        Text(
            "Dominant Face Color: $detectedColorName",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (outfitSuggestions.isNotEmpty()) {
            Text(
                "Suggested Outfits:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column {
                outfitSuggestions.forEach { outfit ->
                    Text("- $outfit")
                }
            }
        }
    }
}

// Rest of your utility functions remain the same
fun getPixels(bitmap: Bitmap): List<IntArray> {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    return pixels.map { pixel ->
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        intArrayOf(r, g, b)
    }
}

fun kMeans(pixels: List<IntArray>, k: Int = 3, maxIterations: Int = 10): Color {
    val centroids = pixels.shuffled().take(k).toMutableList()
    var clusters = List(k) { mutableListOf<IntArray>() }

    repeat(maxIterations) {
        clusters = List(k) { mutableListOf() }

        for (pixel in pixels) {
            val nearestIndex = centroids.indices.minByOrNull { idx ->
                colorDistance(pixel, centroids[idx])
            } ?: 0
            clusters[nearestIndex].add(pixel)
        }

        for (i in centroids.indices) {
            if (clusters[i].isNotEmpty()) {
                centroids[i] = averageColor(clusters[i])
            }
        }
    }

    val dominantClusterIndex = clusters.indices.maxByOrNull { clusters[it].size } ?: 0
    val dominantColor = centroids[dominantClusterIndex]
    return Color(dominantColor[0], dominantColor[1], dominantColor[2])
}

fun colorDistance(c1: IntArray, c2: IntArray): Double {
    val rDiff = (c1[0] - c2[0]).toDouble()
    val gDiff = (c1[1] - c2[1]).toDouble()
    val bDiff = (c1[2] - c2[2]).toDouble()
    return sqrt(rDiff.pow(2) + gDiff.pow(2) + bDiff.pow(2))
}

fun averageColor(pixels: List<IntArray>): IntArray {
    val r = pixels.map { it[0] }.average().toInt()
    val g = pixels.map { it[1] }.average().toInt()
    val b = pixels.map { it[2] }.average().toInt()
    return intArrayOf(r, g, b)
}

data class NamedColor(val name: String, val rgb: IntArray)

val colorNameList = listOf(
    NamedColor("Black", intArrayOf(0, 0, 0)),
    NamedColor("White", intArrayOf(255, 255, 255)),
    NamedColor("Red", intArrayOf(255, 0, 0)),
    NamedColor("Green", intArrayOf(0, 128, 0)),
    NamedColor("Blue", intArrayOf(0, 0, 255)),
    NamedColor("Yellow", intArrayOf(255, 255, 0)),
    NamedColor("Orange", intArrayOf(255, 165, 0)),
    NamedColor("Pink", intArrayOf(255, 192, 203)),
    NamedColor("Purple", intArrayOf(128, 0, 128)),
    NamedColor("Brown", intArrayOf(165, 42, 42)),
    NamedColor("Beige", intArrayOf(245, 245, 220)),
    NamedColor("Olive", intArrayOf(128, 128, 0)),
    NamedColor("Gray", intArrayOf(128, 128, 128)),
    NamedColor("Light Blue", intArrayOf(173, 216, 230)),
    NamedColor("Light Green", intArrayOf(144, 238, 144))
)

fun getClosestColorName(rgb: IntArray): String {
    var minDistance = Double.MAX_VALUE
    var closestName = "Unknown"

    for (namedColor in colorNameList) {
        val dist = colorDistance(rgb, namedColor.rgb)
        if (dist < minDistance) {
            minDistance = dist
            closestName = namedColor.name
        }
    }
    return closestName
}

fun getOutfitSuggestions(detectedColorName: String): List<String> {
    return when (detectedColorName) {
        "Black" -> listOf("White", "Red", "Royal Blue", "Mustard", "Camel", "Silver")
        "White" -> listOf("Navy", "Maroon", "Emerald", "Coral", "Olive", "Denim Blue")
        "Red" -> listOf("Black", "Denim Blue", "White", "Beige", "Gray", "Gold")
        "Green", "Olive", "Light Green" -> listOf("Brown", "Cream", "Beige", "Tan", "Rust", "Navy")
        "Blue", "Light Blue" -> listOf("White", "Gray", "Yellow", "Orange", "Tan", "Brown")
        "Yellow", "Orange" -> listOf("Blue", "Black", "Gray", "Navy", "White", "Beige")
        "Brown", "Beige" -> listOf("Teal", "Cream", "White", "Mustard", "Olive", "Rust")
        "Pink", "Purple" -> listOf("Black", "Navy", "White", "Gray", "Gold", "Cream")
        "Gray" -> listOf("Red", "Black", "Blue", "Pink", "White", "Burgundy")
        "Teal" -> listOf("White", "Beige", "Brown", "Navy", "Gold")
        "Maroon", "Burgundy" -> listOf("Cream", "Black", "Navy", "Gray", "Blush")
        "Coral" -> listOf("Navy", "White", "Beige", "Gray")
        "Beige" -> listOf("White", "Brown", "Olive", "Teal", "Burnt Orange")
        "Navy" -> listOf("White", "Red", "Tan", "Pink", "Mustard")
        else -> listOf("Black", "White", "Gray")
    }
}