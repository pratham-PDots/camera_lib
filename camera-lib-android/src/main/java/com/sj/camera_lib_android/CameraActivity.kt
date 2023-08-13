package com.sj.camera_lib_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class CameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        Toast.makeText(this, "Welcome to SDK", Toast.LENGTH_SHORT).show()
    }
}