package com.selimozturk.geofencesampleapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.selimozturk.geofencesampleapp.databinding.ActivityMapsBinding


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    @RequiresApi(Build.VERSION_CODES.N)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                showCurrentLocationOfUserOnMap()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                Toast.makeText(this, "Background location access granted.", Toast.LENGTH_LONG)
                    .show()
            }
            else -> {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show()
            }
        }
    }
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    //Geofence yaricapi
    private val GEOFENCE_RADIUS = 200f
    private val GEOFENCE_ID = "SOME_GEOFENCE_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geofencingClient = LocationServices.getGeofencingClient(this)
        createChannel(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.removeGeofencesButton.setOnClickListener {
            mMap.clear()
            removeGeofence()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val eiffel = LatLng(48.858164, 2.2904092)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel, 16f))
        showCurrentLocationOfUserOnMap()

        mMap.setOnMapLongClickListener { latLng ->
            if (Build.VERSION.SDK_INT >= 29) {
                if (backgroundLocationIsGranted()) {
                    addGeofenceToMap(latLng)
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
            } else {
                addGeofenceToMap(latLng)
            }
        }
    }

    private fun addGeofenceToMap(latLng: LatLng) {
        addMarker(latLng)
        addCircle(latLng)
        addGeofence(latLng)
    }

    private fun foregroundLocationIsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun backgroundLocationIsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    private fun showCurrentLocationOfUserOnMap() {
        if (foregroundLocationIsGranted()) {
            mMap.isMyLocationEnabled = true

        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)
        mMap.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng) {
        val circleOptions = CircleOptions()
            .center(latLng)
            .radius(GEOFENCE_RADIUS.toDouble())
            .fillColor(0x40ff0000)
            .strokeColor(Color.BLUE)
            .strokeWidth(4f)
        mMap.addCircle(circleOptions)
    }

    private fun getGeofence(
        latLng: LatLng,
        transitionTypes: Int
    ): Geofence {
        return Geofence.Builder()
            /*
            Olusturacagimiz geofence'i bir id ile tanimliyoruz.
            Ayni id'ye sahip iki geofence oldugunda,
            bu iki geofence'in temsil ettigi cografi
            bolgeye bakilmaksizin yenisi eskisinin yerini alacaktir.
            */
            .setRequestId(GEOFENCE_ID)
            //Geofence'in dairesel bolgesini ayarliyoruz.
            .setCircularRegion(
                latLng.latitude,
                latLng.longitude,
                GEOFENCE_RADIUS
            )
            /*
            Geofence.GEOFENCE_TRANSiTiON_ENTER ve
            Geofence.GEOFENCE_TRANSiTiON_DWELL arasindaki gecikmeyi milisaniye cinsinden ayarlar. Ornegin, bu
            deger 300000 ms (yani 5 dakika) olarak ayarlanirsa, kullanici
            bu sure boyunca geofence icinde kalirsa, Geofence.GEOFENCE_TRANSiTiON_DWELL uyarisi gonderir.
            Kullanici bu sure icinde geofence sinirindan cikarsa
            Geofence.GEOFENCE_TRANSiTiON_DWELL uyarisi gonderilmez.
            Gecis turleri bir Geofence.GEOFENCE_TRANSiTiON_DWELL icermiyorsa bu deger yok sayilir.
            */
            .setLoiteringDelay(300000)
            /*
            Geofence'in sona erme suresini ayarlar.
            Bu cografi sinir, bu sure sonunda otomatik olarak kaldirilir.
            */
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            //Geofence icin gecis turlerini ayarlar.
            .setTransitionTypes(transitionTypes)
            .build()
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng) {
        val geofence = getGeofence(
            latLng,
            Geofence.GEOFENCE_TRANSITION_ENTER
                    or Geofence.GEOFENCE_TRANSITION_EXIT
        )

        val geofencingRequest = getGeofencingRequest(geofence)

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Toast.makeText(this@MapsActivity, "Geofence(s) added", Toast.LENGTH_SHORT).show()
            }
            addOnFailureListener {
                Toast.makeText(
                    this@MapsActivity,
                    it.printStackTrace().toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i("MapsActivity", "Geofence(s) removed")
            }
            addOnFailureListener {
                Log.i("MapsActivity", it.printStackTrace().toString())
            }
        }
    }

}
