package com.quran.labs.androidquran

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.quran.labs.androidquran.util.Compass
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class KiblatActivity : AppCompatActivity() {

    private lateinit var compass: Compass
    private lateinit var compassView: View
    private lateinit var arrow: ImageView
    private lateinit var qiblaDirectionText: TextView
    private lateinit var sotwText: TextView
    private lateinit var sensorStatusText: TextView

    private var currentAzimuth = 0f
    private var bearingToKaaba = 295.0 // Default bearing from Jakarta

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiblat)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        compassView = findViewById(R.id.compass_view)
        arrow = findViewById(R.id.arrow)
        qiblaDirectionText = findViewById(R.id.qiblaDirection)
        sotwText = findViewById(R.id.sotwText)
        sensorStatusText = findViewById(R.id.sensor_status)

        findViewById<View>(R.id.qiblafinder)?.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qiblafinder.withgoogle.com/"))
            startActivity(browserIntent)
        }

        setupLocationAndBearing()
        setupCompass()
    }

    private fun setupLocationAndBearing() {
        val lat: Double
        val lng: Double
        val cityName: String

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var lastLocation: Location? = null

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (lastLocation == null || loc.accuracy < lastLocation.accuracy) {
                    lastLocation = loc
                }
            }
        }

        if (lastLocation != null) {
            lat = lastLocation.latitude
            lng = lastLocation.longitude
            cityName = "Lokasi Saya"
        } else {
            // Default to Jakarta
            lat = -6.2088
            lng = 106.8456
            cityName = "Jakarta (Default)"
        }

        val kaabaLng = 39.826206
        val kaabaLat = Math.toRadians(21.422487)
        val myLatRad = Math.toRadians(lat)
        val longDiff = Math.toRadians(kaabaLng - lng)
        val y = sin(longDiff) * cos(kaabaLat)
        val x = cos(myLatRad) * sin(kaabaLat) - sin(myLatRad) * cos(kaabaLat) * cos(longDiff)
        
        bearingToKaaba = (Math.toDegrees(atan2(y, x)) + 360) % 360
        arrow.rotation = bearingToKaaba.toFloat()
        qiblaDirectionText.text = "$cityName: ${bearingToKaaba.toInt()}°"
    }

    private fun setupCompass() {
        compass = Compass(this)
        compass.setListener(object : Compass.CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                runOnUiThread {
                    adjustCompass(azimuth)
                }
            }

            override fun onAccuracyChanged(accuracy: Int) {
                runOnUiThread {
                    when (accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                            sensorStatusText.text = "Akurasi Rendah - Kalibrasi HP Anda"
                        }
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                            sensorStatusText.text = "Akurasi Sedang"
                        }
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                            sensorStatusText.text = "Akurasi Tinggi"
                        }
                    }
                }
            }
        })
    }

    private fun adjustCompass(azimuth: Float) {
        sotwText.text = "Arah: ${azimuth.toInt()}°"

        val rotateAnimation = RotateAnimation(
            -currentAzimuth, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 200
            fillAfter = true
        }

        currentAzimuth = azimuth
        compassView.startAnimation(rotateAnimation)
    }

    override fun onStart() {
        super.onStart()
        compass.start()
    }

    override fun onResume() {
        super.onResume()
        compass.start()
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
    }

    override fun onStop() {
        super.onStop()
        compass.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        compass.stop()
    }
}
