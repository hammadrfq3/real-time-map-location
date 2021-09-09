package com.example.socketdemo

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory


class MyLocationListener : LocationListener {


    private var locationManager: LocationManager? = null
    private lateinit var myCallBack: (lat:Double,lng:Double,location: Location)-> Unit


    fun getLocationService(context: Context,callback: (lat:Double,lng:Double,location: Location)-> Unit) {
        if ( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //AppUtils.showToast(context,"please give location permission")
        }else{

            myCallBack = callback
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.5f, this)

        }
    }


    override fun onLocationChanged(loc: Location) {
        /*Log.e(
            "Location",
           "Latitude:" + loc.latitude + ", Longitude:" + loc.longitude
        )*/
        myCallBack.invoke(loc.latitude,loc.longitude,loc)
    }

    override fun onProviderDisabled(provider: String) {
        Log.e("Location", "Disable")
    }

    override fun onProviderEnabled(provider: String) {
        Log.e("Location", "Enabled")
    }

    override fun onStatusChanged(
        provider: String,
        status: Int,
        extras: Bundle
    ) {
        Log.e("Location", "Status")
    }
}