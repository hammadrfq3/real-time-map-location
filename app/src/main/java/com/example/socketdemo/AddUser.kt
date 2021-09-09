package com.example.socketdemo

import android.location.Location
import com.google.android.gms.maps.model.LatLng

interface AddUser {

    fun addUser(username: String,socketId: String,latLng: LatLng)

    fun onLocationReceived(latLng: LatLng,socketId: String,bearing: Float)

}