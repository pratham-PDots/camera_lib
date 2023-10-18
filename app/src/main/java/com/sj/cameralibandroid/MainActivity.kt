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
import com.sj.cameralibandroid.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val uploadFrom = "Shelfwatch" // 3rdParty / Shelfwatch
    private var uploadParams = ""
    private lateinit var viewModel: CameraViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        binding.scrollingTextView.text = ""

        uploadParams = """
                        {
                            "shop_id": 62475,
                            "project_id": "263cbe94-ed05-430b-a0f9-ae16ab14d0f",
                            "td_version_id": 1180,
                            "shelf_image_id": null,
                            "asset_image_id": null,
                            "shelf_type": "Retailer Owned Fridge",
                            "category_id": 1453,
                            "user_id": 36400,
                            "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6ImZlNjBjMWUyODllNGU2MTNkOGU3MGVjMzJlZGM1NjljMjllYTJlMGRhYTJlYzRkNTYzMzZjZjk4MmE0OTM5Y2YiLCJ1c2VybmFtZSI6IktQX1Rlc3RfMjAifQ.A4ich3QNdtREmfhEckbgnHZbqEy4OS9PYFCMCmhZB98",
                            "isConnected": true,
                            "sn_image_type": "skus",
                            "image_type": "single",
                            "seq_no": 1,
                            "level": null,
                            "last_image_flag": 1,
                            "uploadOnlyOnWifi": 0,
                            "app_session_id": "8e2faa6b-d6fe-413a-a693-76a0cbe0ce71",
                            "task_id": 266768
                        }
                        """
        Log.d("imageSW uploadParams ",uploadParams)
        // register BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("DataSaved"))

        binding.button2.setOnClickListener {
            launchCAMERA()
        }
    }

    private val myBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var status = intent!!.getStringExtra("status")
            var imageListSaved = intent.getParcelableArrayListExtra<ImageUploadModel>("imageListSaved")
            Log.i("imageSW BroadcastReceiver","Received: $status, " +
                    "size: ${imageListSaved?.size}, imageListSaved ==>> $imageListSaved")

            val formattedText = imageListSaved?.joinToString("\n\n") { model ->
                            """
                Position: ${model.position}
                Dimension: ${model.dimension}
                Longitude: ${model.longitude}
                Latitude: ${model.latitude}
                Total Image Captured: ${model.total_image_captured}
                App Timestamp: ${model.app_timestamp}
                Orientation: ${model.orientation}
                Zoom Level: ${model.zoom_level}
                Session ID: ${model.session_id}
                Crop Coordinates: ${model.crop_coordinates}
                Overlap Values: ${model.overlap_values}
                Upload Params: ${model.upload_params}
                URI: ${model.uri}
                Type: ${model.type}
                Name: ${model.name}
                File: ${model.file.absolutePath}
                """.trimIndent()
                        }

            binding.scrollingTextView.text = formattedText
        }
    }

    private fun launchCAMERA() {
        binding.scrollingTextView.text = ""
        val uploadParamsCustom = """
                        {
                            "shop_id": ${binding.editTextShopId.text?.trim().toString()},
                            "project_id": "${binding.editTextProjectId.text?.trim().toString()}",
                            "td_version_id": 1180,
                            "shelf_image_id": null,
                            "asset_image_id": null,
                            "shelf_type": "Retailer Owned Fridge",
                            "category_id": ${binding.editTextCategoryId.text?.trim().toString()},
                            "user_id": 36400,
                            "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6ImZlNjBjMWUyODllNGU2MTNkOGU3MGVjMzJlZGM1NjljMjllYTJlMGRhYTJlYzRkNTYzMzZjZjk4MmE0OTM5Y2YiLCJ1c2VybmFtZSI6IktQX1Rlc3RfMjAifQ.A4ich3QNdtREmfhEckbgnHZbqEy4OS9PYFCMCmhZB98",
                            "isConnected": true,
                            "sn_image_type": "skus",
                            "image_type": "single",
                            "seq_no": 1,
                            "level": null,
                            "last_image_flag": 1,
                            "uploadOnlyOnWifi": 0,
                            "app_session_id": "8e2faa6b-d6fe-413a-a693-76a0cbe0ce71",
                            "task_id": 266768
                        }
                        """


        val intent = Intent(this@MainActivity, LaunchShelfwatchCamera::class.java)
        intent.putExtra("mode", binding.editTextMode.text.toString()) //portrait / landscape
        intent.putExtra("overlapBE", binding.editTextOverlapBE.text.toString())
        intent.putExtra("uploadParam", uploadParamsCustom)
        intent.putExtra("resolution", binding.editTextResolution.text.toString())
        intent.putExtra("referenceUrl", binding.editTextReferenceURL.text.toString())
        intent.putExtra("isBlurFeature", binding.editTextBlurFeature.text.toString())
        intent.putExtra("isCropFeature", binding.editTextCropFeature.text.toString())
        intent.putExtra("uploadFrom", binding.editTextUploadFrom.text.toString()) // Shelfwatch / 3rdParty
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