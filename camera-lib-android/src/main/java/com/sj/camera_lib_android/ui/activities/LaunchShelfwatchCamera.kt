package com.sj.camera_lib_android.ui.activities
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.CameraActivity
import com.sj.camera_lib_android.R
import com.sj.camera_lib_android.utils.Events
import com.sj.camera_lib_android.utils.LogUtils
import java.lang.NumberFormatException
import java.util.Locale

class LaunchShelfwatchCamera : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        checkAndSetLanguage()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launch_shelfwatch_camera)

        val portraitButton = findViewById<LinearLayout>(R.id.portraitRotationLL)
        val landscapeButton = findViewById<LinearLayout>(R.id.landscapeRotationLL)
        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext)
            Log.d("imageSW FirebaseApp","initialized")
        }


        val extras = intent.extras
        var modeRotation = ""
        var overlayBE = 20f
        var uploadParams = ""
        var resolution = ""
        var referenceUrl = ""
        var isBlurFeature = ""
        var isCropFeature = ""
        var uploadFrom = ""
        var isRetake = false
        var zoomLevel = 0.0
        var backendToggle = false
        var gridlines = false
        var language: String = "en"
        if (extras != null) {
            modeRotation = extras.getString("mode") ?: ""
            overlayBE = extras.getString("overlapBE")?.toFloat() ?: 20f
            uploadParams = extras.getString("uploadParam") ?: ""
            resolution = extras.getString("resolution") ?: ""
            referenceUrl = extras.getString("referenceUrl") ?: ""
            isBlurFeature = extras.getString("isBlurFeature") ?: ""
            isCropFeature = extras.getString("isCropFeature") ?: ""
            uploadFrom = extras.getString("uploadFrom") ?: "Shelfwatch"
            isRetake = extras.getBoolean("isRetake", false)
            zoomLevel = extras.getDouble("zoomLevel", 1.0)
            backendToggle = extras.getBoolean("backendToggle", false)
            gridlines = extras.getBoolean("gridlines", false)
            language = extras.getString("language", language)


            var message =
                "Mode: $modeRotation ,uploadParams: $uploadParams , overlayBE: $overlayBE," +
                        " resolution: $resolution, referenceUrl: $referenceUrl, isBlurFeature: $isBlurFeature," +
                        " isCropFeature: $isCropFeature, uploadFrom: $uploadFrom"
            Log.d("imageSW inputFlags", " LaunchCamera : $message")


        }
        if (modeRotation.isNotEmpty()){
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom, isRetake, zoomLevel, backendToggle, gridlines, language)

        }

        portraitButton.setOnClickListener {
            modeRotation = "portrait"
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom, isRetake, zoomLevel, backendToggle, gridlines, language)
            Log.d("imageSW selectRotationDialog", " $modeRotation")
        }

        landscapeButton.setOnClickListener {
            modeRotation = "landscape"
            goToCameraScreen(modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom, isRetake, zoomLevel, backendToggle, gridlines, language)

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
        uploadFrom: String,
        isRetake: Boolean = false,
        zoomLevel: Double = 1.0,
        backendToggle: Boolean = false,
        gridlines: Boolean = false,
        language: String = "en"
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
            intent.putExtra("isRetake", isRetake)
            intent.putExtra("zoomLevel", zoomLevel)
            intent.putExtra("backendToggle", backendToggle)
            intent.putExtra("gridlines", gridlines)
            intent.putExtra("language", language)
            startActivity(intent)
            finish()
        }
    }

    private fun checkAndSetLanguage() {
        try {
            intent.extras?.getString("language")?.let { desiredLanguage ->
                Log.d(
                    "imageSW",
                    "current locale: ${resources.configuration.locale.language} desired language: $desiredLanguage"
                )
                if (resources.configuration.locale.language != desiredLanguage) {
                    setSDKLanguage(desiredLanguage)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun setSDKLanguage(localeString: String = "en") {
        try {
            val locale = Locale(localeString)
            Locale.setDefault(locale)

            val resources: Resources = this.resources
            val configuration = Configuration(resources.configuration)

            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        } catch (e: Exception) {
            LogUtils.logGlobally(Events.FAILED_TO_CHANGE_LANGUAGE)
        }
    }
}