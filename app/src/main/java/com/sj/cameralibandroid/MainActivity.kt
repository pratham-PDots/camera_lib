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
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sj.camera_lib_android.Demo
import com.sj.camera_lib_android.models.ImageUploadModel
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera
import com.sj.camera_lib_android.ui.viewmodels.CameraViewModel
import com.sj.cameralibandroid.databinding.ActivityMainBinding
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private val uploadFrom = "Shelfwatch" // 3rdParty / Shelfwatch
    private val progressMap = mutableMapOf<Int, Int>()
    private var uploadParams = JSONObject("""
                        {
                            "shop_id": 62475,
                            "project_id": "263cbe94-ed05-430b-a0f9-ae16ab14d0f",
                            "td_version_id": 178,
                            "shelf_image_id": null,
                            "asset_image_id": null,
                            "shelf_type": "Main Aisle",
                            "category_id": 1453,
                            "user_id": 36400,
                            "isConnected": true,
                            "sn_image_type": "skus",
                            "image_type": "single",
                            "seq_no": 1,
                            "level": 1,
                            "last_image_flag": 1,
                            "uploadOnlyOnWifi": 0,
                            "app_session_id": "8e2faa6b-d6fe-413a-a693-76a0cbe0ce71"
                        }
                        """)
    private lateinit var viewModel: CameraViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        binding.scrollingTextView.text = ""
        binding.progressTextView.text = ""
        progressMap.clear()

        Log.d("imageSW uploadParams ",uploadParams.toString())
        // register BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("DataSaved"))

        binding.editUserId.doOnTextChanged { text, start, before, count ->
            uploadParams.put("user_id", binding.editUserId.text?.trim().toString())
            binding.uploadParamTextView.text = formatJson(uploadParams.toString())

        }

        binding.editTextCategoryId.doOnTextChanged { text, start, before, count ->
            uploadParams.put("category_id", binding.editTextCategoryId.text?.trim().toString())
            binding.uploadParamTextView.text = formatJson(uploadParams.toString())

        }

        binding.editTextShopId.doOnTextChanged { text, start, before, count ->
            uploadParams.put("shop_id", binding.editTextShopId.text?.trim().toString())
            binding.uploadParamTextView.text = formatJson(uploadParams.toString())

        }
        binding.editTextProjectId.doOnTextChanged { text, start, before, count ->
            uploadParams.put("project_id", binding.editTextProjectId.text?.trim().toString())
            binding.uploadParamTextView.text = formatJson(uploadParams.toString())

        }

        binding.button2.setOnClickListener {
            launchCAMERA()
        }

        binding.addbutton.setOnClickListener {
            UploadParamBottomsheet().apply {
                setOnSaveClickListener(object : UploadParamBottomsheet.OnSaveClickListener {
                    override fun onSaveClicked(key: String, value: String) {
                        uploadParams.put(key.trim(), value.trim())
                        binding.uploadParamTextView.text = formatJson(uploadParams.toString())
                    }

                })
                show(supportFragmentManager, tag)
            }
        }

    }

    // Function to format the JSON string
    fun formatJson(jsonStr: String): String {
        val indentSize = 4 // You can adjust the indentation size as needed
        val builder = StringBuilder()
        var indent = 0
        val len = jsonStr.length

        for (i in 0 until len) {
            val currentChar = jsonStr[i]
            if (currentChar == '{' || currentChar == '[') {
                builder.append(currentChar)
                builder.append('\n')
                indent += indentSize
                appendIndent(builder, indent)
            } else if (currentChar == '}' || currentChar == ']') {
                builder.append('\n')
                indent -= indentSize
                appendIndent(builder, indent)
                builder.append(currentChar)
            } else if (currentChar == ',') {
                builder.append(currentChar)
                builder.append('\n')
                appendIndent(builder, indent)
            } else {
                builder.append(currentChar)
            }
        }

        return builder.toString()
    }

    fun appendIndent(builder: StringBuilder, indent: Int) {
        for (i in 0 until indent) {
            builder.append(' ')
        }
    }

    private val myBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var progress = intent!!.getIntExtra("progress", 0)
            var index = intent!!.getIntExtra("index", -1)

            Log.d("imageSW broadcast", "$index $progress")

            if(index != - 1) {
                progressMap[index] = progress
                var prettyProgress: String = "Upload Status: \n"
                progressMap.forEach { (index, progress) ->
                    prettyProgress += "Image $index : $progress% \n"
                }
                binding.progressTextView.text = prettyProgress
            }



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

            if(!formattedText.isNullOrEmpty()) binding.scrollingTextView.text = formattedText
        }
    }

    private fun launchCAMERA() {
        binding.scrollingTextView.text = ""
        binding.progressTextView.text = ""
        progressMap.clear()
        uploadParams.put("shop_id", binding.editTextShopId.text?.trim().toString())
        uploadParams.put("category_id", binding.editTextCategoryId.text?.trim().toString())
        uploadParams.put("user_id", binding.editUserId.text?.trim().toString())
        uploadParams.put("project_id", binding.editTextProjectId.text?.trim().toString())

        val intent = Intent(this@MainActivity, LaunchShelfwatchCamera::class.java)
        intent.putExtra("mode", binding.editTextMode.text.toString()) //portrait / landscape
        intent.putExtra("overlapBE", binding.editTextOverlapBE.text.toString())
        intent.putExtra("uploadParam", uploadParams.toString())
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
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("Progress"))// onResume
    }

    // Unbind from the service in the onDestroy() method of the fragment
     override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver) // onDestroy
    }
}