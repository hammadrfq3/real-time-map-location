package com.example.socketdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.socketdemo.databinding.ActivityMapsBinding
import com.example.socketdemo.socket.SocketDemo
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.json.JSONObject

import com.google.gson.Gson
import com.google.android.gms.maps.model.CameraPosition

import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.model.PolylineOptions








class MapsActivity : AppCompatActivity(), OnMapReadyCallback, AddUser {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var mLocationListener: MyLocationListener? = null
    private val TAG = "MapsActivity"
    private lateinit var marker: Marker
    private var isMarkerRotating = false
    private var toRotate: Double = 0.0
    private var destinationLatLng: LatLng? =null
    private lateinit var polylineOptions: PolylineOptions
    private var otherUserSocketId: String? = null
    private var otherUSerMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SocketDemo.setListener(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mLocationListener = MyLocationListener()
        mLocationListener!!.getLocationService(this) { lat, lng, location ->
            Log.e(TAG,"Activity Lat: $lat Lng: $lng")
            if(destinationLatLng!=null){
                runOnUiThread {
                    updateUersLocation(lat, lng, location.bearing, marker)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(lat,lng)))
                }
                if (otherUserSocketId!=null){
                    val locationData = LocationData(lat.toString(),
                        lng.toString(),
                        location.bearing,
                        otherUserSocketId!!)
                    SocketDemo.sendLocation(convertObjToJsonObj(locationData))
                }
            }
        }
    }

    fun updateUersLocation(lat: Double,lng: Double,bearing: Float,mMarker: Marker){
        val currentLocation = LatLng(lat, lng)
        //marker.position = sydney
        mMarker.rotation = bearing
        //polylineOptions.add(currentLocation)
        animateMarker(mMarker,currentLocation,false)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(31.4844536, 74.3928821)
        val userData = UserData(SocketDemo.getSocketId(),"Robot ${SocketDemo.getSocketId()}",31.4844536,74.3928821)
        SocketDemo.sendConnectionRequest(convertObjToJsonObj(userData))
        marker = mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        marker.setIcon(bitmapDescriptorFromVector(this,R.drawable.ic_car))
        marker.isFlat = true
        myLocation()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,17f))
        mMap.setOnMapClickListener {
            if(destinationLatLng ==null){
                destinationLatLng = it
                polylineOptions = PolylineOptions()
                    .color(Color.parseColor("#FFB700"))
                    .width(15f) // Point A.
            }
        }
    }

    fun myLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap.isMyLocationEnabled = true
    }

    fun convertObjToJsonObj(any: Any): JSONObject{
        val jsonInString = Gson().toJson(any)
        return JSONObject(jsonInString)
    }

    private fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorDrawableResourceId: Int
    ): BitmapDescriptor? {
        val background = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        vectorDrawable!!.setBounds(
            50,
            100,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            background.intrinsicWidth,
            background.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        //background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun animateMarker(
        marker: Marker, toPosition: LatLng,
        hideMarker: Boolean
    ) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj: Projection = mMap.projection
        val startPoint: Point = proj.toScreenLocation(marker.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration: Long = 500
        val interpolator: Interpolator = LinearInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(
                    elapsed.toFloat()
                            / duration
                )
                val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
                val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude
                marker.position = LatLng(lat, lng)
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                } else {
                    if (hideMarker) {
                        marker.isVisible = false
                    } else {
                        marker.isVisible = true
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //otherUserSocketId?.let { SocketDemo.removeOtherUserMarker(it) }
        SocketDemo.disconnect()
    }

    protected fun createMarker(
        latitude: Double,
        longitude: Double
    ): Marker? {
        return mMap.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .icon(bitmapDescriptorFromVector(this,R.drawable.ic_car))
        )
    }

    override fun addUser(username: String, socketId: String,latLng: LatLng) {

        runOnUiThread {
            otherUSerMarker = createMarker(latLng.latitude, latLng.longitude)
        }
        otherUserSocketId = socketId

    }

    override fun onLocationReceived(latLng: LatLng, socketId: String,bearing: Float) {

        runOnUiThread {
            updateUersLocation(latLng.latitude,latLng.longitude, bearing, otherUSerMarker!!)
        }

    }

    override fun removeMarker() {
        runOnUiThread {
            otherUSerMarker?.remove()
        }
    }
}