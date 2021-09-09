package com.example.socketdemo

import com.google.android.gms.maps.model.LatLng

data class UserData(
    val socketId: String,
    val userName: String,
    val lat: Double,
    val lng: Double
)