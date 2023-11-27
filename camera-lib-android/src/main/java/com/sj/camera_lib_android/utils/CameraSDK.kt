package com.sj.camera_lib_android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bugfender.sdk.Bugfender
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.services.FailedRetryService
import com.sj.camera_lib_android.services.InitService
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera
import org.json.JSONObject

object CameraSDK {
    var bucketName = ""
    fun startCamera(
        context: Context,
        orientation: String,
        widthPercentage: Int,
        uploadParams: JSONObject,
        resolution: Int,
        referenceUrl: String,
        allowBlurCheck: Boolean,
        allowCrop: Boolean,
        uploadFrom: String,
        isRetake: Boolean = false,
        zoomLevel: Double = 1.0,
        showOverlapToggleButton: Boolean = false
    ) {
        Log.d("imageSW here", bucketName)
        val intent = Intent(context, LaunchShelfwatchCamera::class.java)
        intent.putExtra("mode", orientation) //portrait / landscape
        intent.putExtra("overlapBE", widthPercentage.toString())
        intent.putExtra("uploadParam", uploadParams.toString())
        intent.putExtra("resolution", resolution.toString())
        intent.putExtra("referenceUrl",referenceUrl)
        intent.putExtra("isBlurFeature", allowBlurCheck.toString())
        intent.putExtra("isCropFeature", allowCrop.toString())
        intent.putExtra("uploadFrom", uploadFrom) // Shelfwatch / 3rdParty
        intent.putExtra("isRetake", isRetake)
        intent.putExtra("zoomLevel", zoomLevel)
        intent.putExtra("backendToggle", showOverlapToggleButton)

        if (context is Activity) {
            context.startActivity(intent)
        } else {
            // If the context is not an instance of Activity, add the FLAG_ACTIVITY_NEW_TASK flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun init(context: Context, bucketName: String) {
        this.bucketName = bucketName
        FirebaseApp.initializeApp(context.applicationContext)
        Bugfender.init(context.applicationContext, "lz6sMkQQVpEZXeY9o7Bi7VwyCG7wTPU6", true)
        Bugfender.enableCrashReporting()
        val intent = Intent(context.applicationContext, InitService()::class.java) // image Upload from gallery
        context.startService(intent)
    }

    fun uploadFailedImage(context: Context) {
        Log.d("imageSW", "uploadFailedImage")
        val intent = Intent(context.applicationContext, FailedRetryService()::class.java)
        context.startService(intent)
    }
}