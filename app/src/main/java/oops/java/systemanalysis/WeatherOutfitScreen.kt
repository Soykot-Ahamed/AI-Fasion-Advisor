package oops.java.systemanalysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import androidx.compose.ui.Alignment

@Composable
fun WeatherOutfitScreen() {
    var city by remember { mutableStateOf("Dhaka") } // Default city
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(city) {
        isLoading = true
        errorMessage = null
        try {
            val api = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)

            val response = api.getWeatherForecast(city)
            val forecastList = response.list.take(5) // Take 5 upcoming entries

            suggestions = forecastList.map {
                val temp = it.main.temp
                val desc = it.weather.firstOrNull()?.description ?: "unknown"
                val date = it.dt_txt
                "📅 $date\n🌡 Temp: ${temp}°C\n🌤 Weather: $desc\n👕 Suggestion: ${suggestDress(temp)}"
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unknown error occurred"
            suggestions = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "Weather Outfit Suggestions",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter City") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            errorMessage != null -> {
                Text(
                    "❌ Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            suggestions.isEmpty() -> {
                Text(
                    "No suggestions available",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            else -> {
                suggestions.forEach { suggestion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            suggestion,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// Data models
data class WeatherResponse(val list: List<WeatherItem>)

data class WeatherItem(
    val dt_txt: String,
    val main: MainInfo,
    val weather: List<WeatherDesc>
)

data class MainInfo(val temp: Double)

data class WeatherDesc(val main: String, val description: String)

// Retrofit API interface
interface WeatherApiService {
    @GET("data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String =apiKey2,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

// Helper function for dress suggestions
private fun suggestDress(temp: Double): String {
    return when {
        temp > 30 -> "Light cotton clothes, sunglasses, cap"
        temp in 20.0..30.0 -> "T-shirt and jeans or casual dress"
        temp in 10.0..19.9 -> "Sweater or hoodie"
        else -> "Warm jacket, gloves, and scarf"
    }
}