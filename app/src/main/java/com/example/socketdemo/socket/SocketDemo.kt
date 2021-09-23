package com.example.socketdemo.socket

import android.location.Location
import android.util.Log
import com.example.socketdemo.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.net.URISyntaxException


object SocketDemo {

    private lateinit var mSocket: Socket
    private val TAG = "Socket"
    private lateinit var addUser: AddUser

    fun initializeSocket(){

        try {
            mSocket = IO.socket(Constants.SOCKET_URL)
        } catch (e: URISyntaxException) {
            Log.e(TAG, e.message.toString())
        }

        mSocket.on(Socket.EVENT_CONNECT,onConnect)
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect)
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket.on(Constants.EVENT_ON_CONNECTION_SUCCEED, onConnectionSucceed)
        mSocket.on(Constants.EVENT_LOCATION_RECEIVER, onLocationReceiver)
        mSocket.on(Constants.EVENT_MESSAGE, onMessageReceived)
        mSocket.on(Constants.EVENT_REMOVE_MARKER, onRemoveMarker)
        connect()
    }

    fun setListener(addUser: AddUser){
        this.addUser = addUser
    }

    fun connect(){
        mSocket.connect()
    }

    fun disconnect(){
        mSocket.disconnect()
    }

    fun removeOtherUserMarker(socketId: String){
        mSocket.emit(Constants.EVENT_REMOVE_MARKER,socketId)
    }

    fun getSocketId(): String{
        return mSocket.id()
    }

    fun sendConnectionRequest(userData: JSONObject){
        mSocket.emit(Constants.EVENT_CONNECTION_REQUEST,userData)
    }

    fun sendLocation(locationData: JSONObject){
        mSocket.emit(Constants.EVENT_LOCATION_RECEIVER,locationData)
    }

    fun sendMessage(chat: Chat){
        mSocket.emit(Constants.EVENT_MESSAGE,chat)
    }

    private val onConnect = Emitter.Listener {
        Log.e(TAG,"Socket connected")
    }

    private val onDisconnect = Emitter.Listener {
        Log.e(TAG,"Socket disconnected")
    }

    private val onConnectError = Emitter.Listener {
        Log.e(TAG,"Connection error: ${it[0]}")
    }

    private val onMessageReceived = Emitter.Listener { args ->
        Log.e(TAG,"onMessageReceived")
        val data = args[0] as JSONObject
        val username: String
        val message: String
        try {
            username = data.getString("name")
            message = data.getString("msg")

            Log.d(TAG,"Username: $username Message: $message")
        } catch (e: JSONException) {
            Log.e(TAG, e.message!!)
        }
    }

    private val onConnectionSucceed = Emitter.Listener { args ->
        Log.e(TAG,"onConnectionSucceed")
        val data = args[0] as JSONObject
        Log.e(TAG,data.toString())
        val otherUserSocketID: String
        val lat: String
        val lng: String
        val userName: String
        try {
            lat = data.getString("lat")
            lng = data.getString("lng")
            userName = data.getString("userName")
            otherUserSocketID = data.getString("id")
            addUser.addUser(userName,otherUserSocketID,LatLng(lat.toDouble(),lng.toDouble()))
            Log.d(TAG,"Username: $userName SocketID: $otherUserSocketID")
        } catch (e: JSONException) {
            Log.e(TAG, e.message!!)
        }
    }

    private val onLocationReceiver = Emitter.Listener { args ->
        Log.e(TAG,"onLocationReceiver")
        val data = args[0] as JSONObject
        Log.e(TAG,data.toString())
        val lat: String
        val socketId: String
        val bearing: String
        val lng: String
        try {
            lat = data.getString("lat")
            lng = data.getString("lng")
            socketId = data.getString("id")
            bearing = data.getString("bearing")
            addUser.onLocationReceived(LatLng(lat.toDouble(),lng.toDouble()),socketId,bearing.toFloat())
            Log.d(TAG,"lat: $lat lng: $lng")
        } catch (e: JSONException) {
            Log.e(TAG, e.message!!)
        }
    }

    private val onRemoveMarker = Emitter.Listener { args ->
        Log.e(TAG,"onRemoveMarker")
        try {
            addUser.removeMarker()
        } catch (e: JSONException) {
            Log.e(TAG, e.message!!)
        }
    }


}