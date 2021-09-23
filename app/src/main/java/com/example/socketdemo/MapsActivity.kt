package com.example.socketdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.socketdemo.Constants.Companion.DEFAULT_ZOOM
import com.example.socketdemo.databinding.ActivityMapsBinding
import com.example.socketdemo.socket.SocketDemo
import com.example.socketdemo.utils.AnimationUtils
import com.example.socketdemo.utils.MapUtils
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, AddUser {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var mLocationListener: MyLocationListener? = null
    private val TAG = "MapsActivity"
    private lateinit var marker: Marker
    private var timer: Timer? = null
    private var isMarkerRotating = false
    private var toRotate: Double = 0.0
    private var destinationLatLng: LatLng? = null
    private lateinit var polylineOptions: PolylineOptions
    private var otherUserSocketId: String? = null
    private var otherUSerMarker: Marker? = null
    private var mGeoApiContext: GeoApiContext? = null
    private var previousLatLngFromServer: LatLng? = null
    private var currentLatLngFromServer: LatLng? = null
    private var otherPreviousLatLngFromServer: LatLng? = null
    private var otherCurrentLatLngFromServer: LatLng? = null
    private var tripPath = arrayListOf<com.google.maps.model.LatLng>()
    private var timerTask: TimerTask? = null
    private val mainThread = Handler(Looper.getMainLooper())

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
            Log.e(TAG, "Activity Lat: $lat Lng: $lng")
            if (destinationLatLng != null) {
                runOnUiThread {
                    //updateUersLocation(lat, lng, location.bearing, marker)
                    //mMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(lat,lng)))
                    //updateCabLocation(LatLng(lat, lng))
                }
                /*if (otherUserSocketId != null) {
                    val locationData = LocationData(
                        lat.toString(),
                        lng.toString(),
                        location.bearing,
                        otherUserSocketId!!
                    )
                    SocketDemo.sendLocation(convertObjToJsonObj(locationData))
                }*/
            }
        }
    }

    fun updateUersLocation(lat: Double, lng: Double, bearing: Float, mMarker: Marker) {
        val currentLocation = LatLng(lat, lng)
        animateMarker(mMarker, currentLocation, false, bearing)
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
        val userData = UserData(
            SocketDemo.getSocketId(),
            "Robot ${SocketDemo.getSocketId()}",
            31.4844536,
            74.3928821
        )
        SocketDemo.sendConnectionRequest(convertObjToJsonObj(userData))
        /*marker = mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        marker.setIcon(bitmapDescriptorFromVector(this, com.example.socketdemo.R.drawable.ic_car))
        marker.isFlat = true*/
        marker = addCarMarkerAndGet(sydney)
        myLocation()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, DEFAULT_ZOOM))
        mMap.setOnMapClickListener {
            if (destinationLatLng == null) {
                destinationLatLng = it
                calculateDirections()
            }
        }
    }

    fun myLocation() {
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
        //mMap.isMyLocationEnabled = true
    }

    fun convertObjToJsonObj(any: Any): JSONObject {
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
                .icon(bitmapDescriptorFromVector(this, com.example.socketdemo.R.drawable.ic_car))
        )
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        return mMap.addMarker(
            MarkerOptions().position(latLng).flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(this)))
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
                .icon(
                    bitmapDescriptorFromVector(
                        this,
                        com.example.socketdemo.R.drawable.ic_minutes
                    )
                )
                .anchor(0.5f, 0.5f)
        ).showInfoWindow()
    }

    override fun addUser(username: String, socketId: String, latLng: LatLng) {

        runOnUiThread {
            otherUSerMarker = addCarMarkerAndGet(LatLng(latLng.latitude, latLng.longitude))
        }
        otherUserSocketId = socketId

    }

    fun updateCabLocation(latLng: LatLng,myMarker: Marker) {
        //if (myMarker == null)
            //myMarker = addCarMarkerAndGet(LatLng(latLng.latitude, latLng.longitude))
        if (previousLatLngFromServer == null) {
            currentLatLngFromServer = latLng
            previousLatLngFromServer = currentLatLngFromServer
            myMarker.position = currentLatLngFromServer
            myMarker.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLngFromServer)
        } else {
            previousLatLngFromServer = currentLatLngFromServer
            currentLatLngFromServer = latLng
            val valueAnimator = AnimationUtils.cabAnimator()
            valueAnimator.addUpdateListener {
                if (currentLatLngFromServer != null && previousLatLngFromServer != null
                ) {
                    val multiplier = it.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * currentLatLngFromServer!!.latitude + (1 - multiplier) * previousLatLngFromServer!!.latitude,
                        multiplier * currentLatLngFromServer!!.longitude + (1 - multiplier) * previousLatLngFromServer!!.longitude
                    )
                    myMarker.position = nextLocation
                    val rotation =
                        MapUtils.getRotation(previousLatLngFromServer!!, currentLatLngFromServer!!)
                    if (!rotation.isNaN()) {
                        myMarker.rotation = rotation
                    }
                    myMarker.setAnchor(0.5f, 0.5f)
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
    }

    fun otherUpdateCabLocation(latLng: LatLng,myMarker: Marker) {
        //if (myMarker == null)
        //myMarker = addCarMarkerAndGet(LatLng(latLng.latitude, latLng.longitude))
        if (otherPreviousLatLngFromServer == null) {
            otherCurrentLatLngFromServer = latLng
            otherPreviousLatLngFromServer = otherCurrentLatLngFromServer
            myMarker.position = otherCurrentLatLngFromServer
            myMarker.setAnchor(0.5f, 0.5f)
            animateCamera(otherCurrentLatLngFromServer)
        } else {
            otherPreviousLatLngFromServer = otherCurrentLatLngFromServer
            otherCurrentLatLngFromServer = latLng
            val valueAnimator = AnimationUtils.cabAnimator()
            valueAnimator.addUpdateListener {
                if (otherCurrentLatLngFromServer != null && otherPreviousLatLngFromServer != null
                ) {
                    val multiplier = it.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * otherCurrentLatLngFromServer!!.latitude + (1 - multiplier) * otherPreviousLatLngFromServer!!.latitude,
                        multiplier * otherCurrentLatLngFromServer!!.longitude + (1 - multiplier) * otherPreviousLatLngFromServer!!.longitude
                    )
                    myMarker.position = nextLocation
                    val rotation =
                        MapUtils.getRotation(otherPreviousLatLngFromServer!!, otherCurrentLatLngFromServer!!)
                    if (!rotation.isNaN()) {
                        myMarker.rotation = rotation
                    }
                    myMarker.setAnchor(0.5f, 0.5f)
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
    }

    private fun animateCamera(latLng: LatLng?) {
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(
                    latLng
                ).zoom(15.5f).build()
            )
        )
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
                    for (route in result.routes) {
                        val path1 = route.overviewPolyline.decodePath()
                        tripPath.addAll(path1)
                    }
                    startTimerForTrip()
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
                polyline.color =
                    ContextCompat.getColor(this, com.example.socketdemo.R.color.black)
                polyline.isClickable = true
                polyline.width = 5f

                val midPoint = polyline.points.size / 2
                val gg = polyline.points.get(midPoint)
                createMarkerGeneral(
                    gg.latitude,
                    gg.longitude,
                    route.legs[0].duration.toString(),
                    ""
                )
            }
        }
    }

    fun midPoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): LatLng {
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
        return LatLng(lat3, lon3)
    }

    override fun onLocationReceived(latLng: LatLng, socketId: String, bearing: Float) {

        runOnUiThread {
            otherUpdateCabLocation(LatLng(latLng.latitude, latLng.longitude),otherUSerMarker!!)
        }

    }

    fun startTimerForTrip() {
        val delay = 5000L
        val period = 3000L
        val size = tripPath.size
        var index = 0
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {

                if (index == 0) {
                    val jsonObjectTripStart = JSONObject()
                    jsonObjectTripStart.put("type", "tripStart")
                    /*mainThread.post {
                        webSocketListener.onMessage(jsonObjectTripStart.toString())
                    }*/

                    val jsonObject = JSONObject()
                    jsonObject.put("type", "tripPath")
                    val jsonArray = JSONArray()
                    for (trip in tripPath) {
                        val jsonObjectLatLng = JSONObject()
                        jsonObjectLatLng.put("lat", trip.lat)
                        jsonObjectLatLng.put("lng", trip.lng)
                        jsonArray.put(jsonObjectLatLng)
                    }
                    jsonObject.put("path", jsonArray)
                    /*mainThread.post {
                        webSocketListener.onMessage(jsonObject.toString())
                    }*/
                }

                val jsonObject = JSONObject()
                jsonObject.put("type", "location")
                jsonObject.put("lat", tripPath[index].lat)
                jsonObject.put("lng", tripPath[index].lng)
                /*mainThread.post {
                    webSocketListener.onMessage(jsonObject.toString())
                }*/

                if (index == size - 1) {
                    stopTimer()
                    //startTimerForTripEndEvent(webSocketListener)
                }else{
                    runOnUiThread{
                        updateCabLocation(LatLng(tripPath[index].lat,tripPath[index].lng),marker)
                    }
                    if (otherUserSocketId != null) {
                        val locationData = LocationData(
                            tripPath[index].lat.toString(),
                            tripPath[index].lng.toString(),
                            0.0f,
                            otherUserSocketId!!
                        )
                        SocketDemo.sendLocation(convertObjToJsonObj(locationData))
                    }
                }

                index++
            }
        }
        timer?.schedule(timerTask, delay, period)
    }

    fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    override fun removeMarker() {
        runOnUiThread {
            otherUSerMarker?.remove()
        }
    }
}