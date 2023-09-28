package com.sj.cameralibandroid
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sj.camera_lib_android.Demo
import com.sj.camera_lib_android.models.ImageUploadModel
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera
import com.sj.camera_lib_android.ui.viewmodels.CameraViewModel


class MainActivity : AppCompatActivity() {
    private val uploadFrom = "Shelfwatch" // 3rdParty / Shelfwatch
    private lateinit var viewModel: CameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        // register BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("DataSaved"))

    }

    private val myBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var status = intent!!.getStringExtra("status")
            var imageListSaved = intent.getParcelableArrayListExtra<ImageUploadModel>("imageListSaved")
            Log.i("imageSW BroadcastReceiver","Received: $status, " +
                    "size: ${imageListSaved?.size}, imageListSaved ==>> $imageListSaved")


        }
    }

    fun launchCAMERA(view: View) {
        val intent = Intent(this@MainActivity, LaunchShelfwatchCamera::class.java)
        intent.putExtra("mode", "") //portrait / landscape
        intent.putExtra("overlapBE", "20f")
        intent.putExtra("uploadParam", "UploadParams")
        intent.putExtra("resolution", "2048")
        intent.putExtra("referenceUrl", "")
        intent.putExtra("isBlurFeature", "true")
        intent.putExtra("isCropFeature", "true")
        intent.putExtra("uploadFrom", uploadFrom) // Shelfwatch / 3rdParty
        startActivity(intent)

    }

    fun demoSDK(view: View) {
        Demo.showMessage(this@MainActivity,"Testing SDK OK")
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("DataSaved"))// onResume

    }

    // Unbind from the service in the onDestroy() method of the fragment
     override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver) // onDestroy
    }
}