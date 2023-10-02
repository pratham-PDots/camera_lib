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
    private var uploadParams = ""
    private lateinit var viewModel: CameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        uploadParams = " \"shop_id\": 2449056,\n" +
                "    \"project_id\": \"574c28bd-c643-418b-85e2-c189ed86314c\",\n" +
                "    \"td_version_id\": 1180,\n" +
                "    \"shelf_image_id\": null,\n" +
                "    \"asset_image_id\": null,\n" +
                "    \"shelf_type\": \"Retailer Owned Fridge\",\n" +
                "    \"category_id\": 7874,\n" +
                "    \"user_id\": 36400,\n" +
                "    \"token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6ImZlNjBjMWUyODllNGU2MTNkOGU3MGVjMzJlZGM1NjljMjllYTJlMGRhYTJlYzRkNTYzMzZjZjk4MmE0OTM5Y2YiLCJ1c2VybmFtZSI6IktQX1Rlc3RfMjAifQ.A4ich3QNdtREmfhEckbgnHZbqEy4OS9PYFCMCmhZB98\",\n" +
                "    \"isConnected\": true,\n" +
                "    \"sn_image_type\": \"skus\",\n" +
                "    \"image_type\": \"single\",\n" +
                "    \"seq_no\": 1,\n" +
                "    \"level\": null,\n" +
                "    \"last_image_flag\": 1,\n" +
                "    \"uploadOnlyOnWifi\": 0,\n" +
                "    \"app_session_id\": \"8e2faa6b-d6fe-413a-a693-76a0cbe0ce71\",\n" +
                "    \"task_id\": 266768"
        Log.d("imageSW uploadParams ",uploadParams)
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
        intent.putExtra("uploadParam", uploadParams)
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