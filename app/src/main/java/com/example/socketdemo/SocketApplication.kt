package com.example.socketdemo

import android.app.Application
import com.example.socketdemo.socket.SocketDemo


class SocketApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SocketDemo.initializeSocket()

        mContext = this
    }


    companion object{

        lateinit var mContext: SocketApplication

    }

}