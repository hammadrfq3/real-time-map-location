package com.example.socketdemo

import com.google.android.gms.maps.model.LatLng

interface AddUser {

    fun addUser(username: String,socketId: String,latLng: LatLng)

    fun onLocationReceived(latLng: LatLng,socketId: String,bearing: Float)

    fun removeMarker()

}