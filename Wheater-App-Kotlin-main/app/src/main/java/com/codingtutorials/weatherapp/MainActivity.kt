package com.codingtutorials.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var cityNameText: TextView? = null
    private var temperatureText: TextView? = null
    private var humidityText: TextView? = null
    private var descriptionText: TextView? = null
    private var windText: TextView? = null
    private var weatherIcon: ImageView? = null
    private var refreshButton: Button? = null
    private var cityNameInput: EditText? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cityNameText = findViewById(R.id.cityNameText)
        temperatureText = findViewById(R.id.temperatureText)
        humidityText = findViewById(R.id.humidityText)
        windText = findViewById(R.id.windText)
        descriptionText = findViewById(R.id.descriptionText)
        weatherIcon = findViewById(R.id.weatherIcon)
        refreshButton = findViewById(R.id.fetchWeatherButton)
        cityNameInput = findViewById(R.id.cityNameInput)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fetch weather for the user's current location on app start
        fetchWeatherForCurrentLocation()

        refreshButton?.setOnClickListener {
            val cityName = cityNameInput?.text.toString()
            if (cityName.isNotEmpty()) {
                fetchWeatherData(cityName)
            } else {
                cityNameInput?.error = "Please enter a city name"
            }
        }
    }

    private fun fetchWeatherForCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                fetchWeatherDataByCoordinates(location.latitude, location.longitude)
            } else {
                cityNameText?.text = "Location not available"
            }
        }
    }

    private fun fetchWeatherData(cityName: String) {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=$cityName&appid=$API_KEY&units=metric"
        fetchWeatherDataFromUrl(url)
    }

    private fun fetchWeatherDataByCoordinates(latitude: Double, longitude: Double) {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric"
        fetchWeatherDataFromUrl(url)
    }

    private fun fetchWeatherDataFromUrl(url: String) {
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()
            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string()
                runOnUiThread { updateUI(result) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(result: String?) {
        if (result != null) {
            try {
                val jsonObject = JSONObject(result)
                val main = jsonObject.getJSONObject("main")
                val temperature = main.getDouble("temp")
                val humidity = main.getDouble("humidity")
                val windSpeed = jsonObject.getJSONObject("wind").getDouble("speed")

                val description =
                    jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")
                val iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon")
                val resourceName = "ic_$iconCode"
                val resId = resources.getIdentifier(resourceName, "drawable", packageName)
                weatherIcon?.setImageResource(resId)

                cityNameText?.text = jsonObject.getString("name")
                temperatureText?.text = String.format("%.0f°", temperature)
                humidityText?.text = String.format("%.0f%%", humidity)
                windText?.text = String.format("%.0f km/h", windSpeed)
                descriptionText?.text = description
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val API_KEY = "7be4a25466b8361c2ae28097a6aa5617"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
