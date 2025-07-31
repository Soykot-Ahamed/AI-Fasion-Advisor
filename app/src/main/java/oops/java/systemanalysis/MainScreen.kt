package oops.java.systemanalysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// Color constants
private val PinkColor = Color(0xFFFF69B4)
private val CardColor = Color(0xFFFDEDF5)

// Feature list (won't be recreated on recomposition)
private val features = listOf(
    Triple("Color Analysis", "Find colors that suit you.", Icons.Default.Palette),
    Triple("Weather Outfit", "Outfits for today's weather.", Icons.Default.Cloud),
    Triple("Virtual Try-On", "See clothes on you virtually.", Icons.Default.Camera),
    Triple("Body Shape Analyzer", "Understand your body shape.", Icons.Default.Image),
    Triple("Face Problem Detector", "Analyze facial features.", Icons.Default.Face),
    Triple("Fashion Chatbot", "Ask any fashion question.", Icons.Default.Chat),
)

@Composable
fun MainScreen(navController: NavHostController) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "StyleSense AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Welcome to StyleSense AI!",
                fontSize = 20.sp,
                color = PinkColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            FeatureGrid(navController)
        }
    }
}

@Composable
private fun FeatureGrid(navController: NavHostController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        features.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { (title, subtitle, icon) ->
                    FeatureCard(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (title) {
                                "Color Analysis" -> navController.navigate("color_analysis")
                                "Weather Outfit" -> navController.navigate("weather_outfit")
                                "Face Problem Detector" -> navController.navigate("face_detector")
                                "Body Shape Analyzer" -> navController.navigate("shape_analyzer")
                                "Fashion Chatbot" -> navController.navigate("fashion_chatbot")
                                // Add other navigation cases here

                            }
                        }
                    )
                }

                // Fill empty space if odd number of items
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PinkColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}