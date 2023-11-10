package com.sj.camera_lib_android.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sj.camera_lib_android.services.InitService
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera

object CameraSDK {
    var bucketName = ""
    fun startCamera(
        context: Context,
        mode: String,
        overlapBE: String,
        uploadParam: String,
        resolution: String,
        referenceUrl: String,
        isBlurFeature: String,
        isCropFeature: String,
        uploadFrom: String,
        isRetake: Boolean = false,
        zoomLevel: Double = 1.0
    ) {
        Log.d("imageSW here", bucketName)
        val intent = Intent(context, LaunchShelfwatchCamera::class.java)
        intent.putExtra("mode", mode) //portrait / landscape
        intent.putExtra("overlapBE", overlapBE)
        intent.putExtra("uploadParam", uploadParam)
        intent.putExtra("resolution", resolution)
        intent.putExtra("referenceUrl",referenceUrl)
        intent.putExtra("isBlurFeature", isBlurFeature)
        intent.putExtra("isCropFeature", isCropFeature)
        intent.putExtra("uploadFrom", uploadFrom) // Shelfwatch / 3rdParty
        intent.putExtra("isRetake", isRetake)
        intent.putExtra("zoomLevel", zoomLevel)
        context.startActivity(intent)
    }

    fun init(context: Context, bucketName: String) {
        this.bucketName = bucketName
        val intent = Intent(context.applicationContext, InitService()::class.java) // image Upload from gallery
        context.startService(intent)
    }
}