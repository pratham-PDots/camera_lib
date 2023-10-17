package com.sj.camera_lib_android.ui.activities
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.CameraActivity
import com.sj.camera_lib_android.R

class LaunchShelfwatchCamera : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launch_shelfwatch_camera)

        val portraitButton = findViewById<LinearLayout>(R.id.portraitRotationLL)
        val landscapeButton = findViewById<LinearLayout>(R.id.landscapeRotationLL)
        FirebaseApp.initializeApp(applicationContext)


        val extras = intent.extras
        var modeRotation = ""
        var overlayBE = 20f
        var uploadParams = ""
        var resolution = ""
        var referenceUrl = ""
        var isBlurFeature = ""
        var isCropFeature = ""
        var uploadFrom = ""
        if (extras != null) {
            modeRotation = extras.getString("mode") ?: ""
            overlayBE = extras.getString("overlapBE")?.toFloat() ?: 20f
            uploadParams = extras.getString("uploadParam") ?: ""
            resolution = extras.getString("resolution") ?: ""
            referenceUrl = extras.getString("referenceUrl") ?: ""
            isBlurFeature = extras.getString("isBlurFeature") ?: ""
            isCropFeature = extras.getString("isCropFeature") ?: ""
            uploadFrom = extras.getString("uploadFrom") ?: "Shelfwatch"

            var message =
                "Mode: $modeRotation ,uploadParams: $uploadParams , overlayBE: $overlayBE," +
                        " resolution: $resolution, referenceUrl: $referenceUrl, isBlurFeature: $isBlurFeature," +
                        " isCropFeature: $isCropFeature, uploadFrom: $uploadFrom"
            Log.d("imageSW inputFlags", " LaunchCamera : $message")


        }
        if (modeRotation != null && modeRotation.isNotEmpty()){
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom)

        }

        portraitButton.setOnClickListener {
            modeRotation = "portrait"
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom)
            Log.d("imageSW selectRotationDialog", " $modeRotation")
        }

        landscapeButton.setOnClickListener {
            modeRotation = "landscape"
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom)

            Log.d("imageSW selectRotationDialog", " $modeRotation")
        }

    }

    private fun goToCameraScreen(
        modeRotation: String,
        overlayBE: Float,
        uploadParams: String,
        resolution: String,
        referenceUrl: String,
        isBlurFeature: String,
        isCropFeature: String,
        uploadFrom: String
    ) {
        if (modeRotation.isNotEmpty()){
            val intent = Intent(this@LaunchShelfwatchCamera, CameraActivity::class.java)
            intent.putExtra("mode", modeRotation) //portrait / landscape
            intent.putExtra("overlapBE", overlayBE)
            intent.putExtra("uploadParam", uploadParams)
            intent.putExtra("resolution", resolution)
            intent.putExtra("referenceUrl", referenceUrl)
            intent.putExtra("isBlurFeature", isBlurFeature)
            intent.putExtra("isCropFeature", isCropFeature)
            intent.putExtra("uploadFrom", uploadFrom) // Shelfwatch / 3rdParty
            startActivity(intent)
            finish()
        }
    }
}