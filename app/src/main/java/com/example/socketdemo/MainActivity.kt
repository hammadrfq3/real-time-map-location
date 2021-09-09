package com.example.socketdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.socketdemo.databinding.ActivityMainBinding
import com.example.socketdemo.socket.SocketDemo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initListeners()
    }

    private fun initListeners() {

        binding.button2.setOnClickListener {
            //SocketDemo.sendConnectionRequest(binding.editTextTextPersonName.text.toString())
        }

    }

    private fun initViews() {

    }
}