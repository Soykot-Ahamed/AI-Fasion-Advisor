package oops.java.systemanalysis

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.Main.route
    ) {
        composable(Screens.Main.route) { MainScreen(navController) }
        composable(Screens.ColorAnalysis.route) { ColorAnalysisScreen(navController) }
        composable(Screens.WeatherOutfit.route) { WeatherOutfitScreen() }
        composable(Screens.SkinAnalyzer.route) {  SkinAnalyzerUI(navController) }
        composable(Screens.BodyShape.route) { BodyShapeAnalyzerScreen(navController) }
        composable(Screens.FashionChatbot.route) { FashionChatbotScreen(navController) }

        // Add other screens here
    }
}

// Define sealed class for better navigation management
sealed class Screens(val route: String) {
    object Main : Screens("main")
    object ColorAnalysis : Screens("color_analysis")
    object WeatherOutfit : Screens("weather_outfit")

    // object VirtualTryOn : Screens("virtual_tryon")
    object BodyShape : Screens("shape_analyzer")
    object SkinAnalyzer : Screens("face_detector")
     object FashionChatbot : Screens("fashion_chatbot")
}