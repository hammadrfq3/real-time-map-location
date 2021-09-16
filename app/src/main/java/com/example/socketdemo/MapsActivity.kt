package com.example.socketdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
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

import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.LatLng

import android.view.animation.AccelerateDecelerateInterpolator

import com.google.android.gms.maps.model.Marker
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import android.R

import com.google.android.gms.maps.model.Polyline

import com.google.maps.internal.PolylineEncoding

import com.google.maps.model.DirectionsRoute

import android.os.Looper





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
    private var mGeoApiContext: GeoApiContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SocketDemo.setListener(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(com.example.socketdemo.R.id.map) as SupportMapFragment
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
        animateMarker(mMarker,currentLocation,false,bearing)
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

        if (mGeoApiContext == null) {
            mGeoApiContext = GeoApiContext.Builder()
                .apiKey(getString(com.example.socketdemo.R.string.google_maps_api_key))
                .build()
        }

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(31.4844536, 74.3928821)
        val userData = UserData(SocketDemo.getSocketId(),"Robot ${SocketDemo.getSocketId()}",31.4844536,74.3928821)
        SocketDemo.sendConnectionRequest(convertObjToJsonObj(userData))
        marker = mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        marker.setIcon(bitmapDescriptorFromVector(this,com.example.socketdemo.R.drawable.ic_car))
        marker.isFlat = true
        myLocation()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,17f))
        mMap.setOnMapClickListener {
            if(destinationLatLng ==null){
                destinationLatLng = it
                calculateDirections()
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
        hideMarker: Boolean,
        bearing: Float
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
                marker.rotation = bearing
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
                .icon(bitmapDescriptorFromVector(this,com.example.socketdemo.R.drawable.ic_car))
        )
    }

    protected fun createMarkerGeneral(
        latitude: Double,
        longitude: Double,
        title: String,
        snippet: String
    ) {
        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(title)
                .icon(bitmapDescriptorFromVector(this,com.example.socketdemo.R.drawable.ic_minutes))
                .anchor(0.5f, 0.5f)
        ).showInfoWindow()
    }

    override fun addUser(username: String, socketId: String,latLng: LatLng) {

        runOnUiThread {
            otherUSerMarker = createMarker(latLng.latitude, latLng.longitude)
        }
        otherUserSocketId = socketId

    }

    private fun calculateDirections() {
        Log.d(
            TAG,
            "calculateDirections: calculating directions."
        )
        val destination = com.google.maps.model.LatLng(
            destinationLatLng!!.latitude,
            destinationLatLng!!.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(true)
        directions.origin(
            com.google.maps.model.LatLng(
                marker.position.latitude,
                marker.position.longitude
            )
        )
        Log.d(
            TAG,
            "calculateDirections: destination: $destination"
        )
        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult) {
                    Log.d(
                        TAG,
                        "onResult: routes: " + result.routes[0].toString()
                    )
                    Log.d(
                        TAG,
                        "onResult: geocodedWayPoints: " + result.geocodedWaypoints[0].toString()
                    )
                    addPolylinesToMap(result);
                }

                override fun onFailure(e: Throwable) {
                    Log.e(
                        TAG,
                        "onFailure: " + e.message
                    )
                }
            })
    }

    private fun addPolylinesToMap(result: DirectionsResult) {
        Handler(Looper.getMainLooper()).post {
            Log.d(TAG, "run: result routes: " + result.routes.size)
            for (route in result.routes) {
                Log.d(TAG, "run: leg: " + route.legs[0].toString())
                Log.d(TAG, "run: distance: " + route.legs[0].distance)
                Log.d(TAG, "run: duration: " + route.legs[0].duration)

                var totalDist = route.legs[0].distance.inMeters / 1000

                val tempLatLng = midPoint(route.legs[0].startLocation.lat,
                    route.legs[0].startLocation.lng,
                    route.legs[0].endLocation.lat,
                    route.legs[0].endLocation.lng)
              /*  createMarker(tempLatLng.latitude,
                    tempLatLng.longitude)*/
                val decodedPath = PolylineEncoding.decode(route.overviewPolyline.encodedPath)
                val newDecodedPath: MutableList<LatLng> =
                    ArrayList()

                // This loops through all the LatLng coordinates of ONE polyline.
                for (latLng in decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());
                    newDecodedPath.add(
                        LatLng(
                            latLng.lat,
                            latLng.lng
                        )
                    )
                }
                val polyline: Polyline =
                    mMap.addPolyline(PolylineOptions().addAll(newDecodedPath))
                polyline.color = ContextCompat.getColor(this, com.example.socketdemo.R.color.direction)
                polyline.isClickable = true
                polyline.width = 15f

                val midPoint = polyline.points.size / 2
                val gg = polyline.points.get(midPoint)
                createMarkerGeneral(gg.latitude,
                    gg.longitude,
                    route.legs[0].duration.toString(),
                    "")
            }
        }
    }

    fun midPoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : LatLng {
        var lat1 = lat1
        var lon1 = lon1
        var lat2 = lat2
        val dLon = Math.toRadians(lon2 - lon1)

        //convert to radians
        lat1 = Math.toRadians(lat1)
        lat2 = Math.toRadians(lat2)
        lon1 = Math.toRadians(lon1)
        val Bx = Math.cos(lat2) * Math.cos(dLon)
        val By = Math.cos(lat2) * Math.sin(dLon)
        val lat3 = Math.atan2(
            Math.sin(lat1) + Math.sin(lat2),
            Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By)
        )
        val lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx)

        //print out in degrees
        println(Math.toDegrees(lat3).toString() + " " + Math.toDegrees(lon3))
        return LatLng(lat3,lon3)
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