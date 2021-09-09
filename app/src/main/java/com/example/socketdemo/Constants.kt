package com.example.socketdemo

class Constants {

    companion object{
        /**
         * Called on a connection.
         */
        val EVENT_CONNECT = "connect"

        /**
         * Called on a disconnection.
         */
        val EVENT_DISCONNECT = "disconnect"

        /**
         * Called on a connection error.
         *
         *
         * Parameters:
         *
         *  * (Exception) error data.
         *
         */
        val EVENT_CONNECT_ERROR = "connect_error"

        val EVENT_MESSAGE = "message"

        val USERNAME = "username"

        val EVENT_ON_CONNECTION_SUCCEED = "onConnectionSucceed"

        val EVENT_CONNECTION_REQUEST = "connectionRequest"

        val EVENT_LOCATION_RECEIVER = "onLocationReceiver"

        val SOCKET_URL = "http://192.168.100.147:3000"
    }

}