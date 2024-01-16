package com.sj.camera_lib_android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bugfender.sdk.Bugfender
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.Database.AppDatabase
import com.sj.camera_lib_android.ScopeHelper
import com.sj.camera_lib_android.services.FailedRetryService
import com.sj.camera_lib_android.services.InitService
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

object CameraSDK {
    var bucketName = ""
    var consumer = ""
    fun startCamera(
        context: Context,
        orientation: String = "",
        widthPercentage: Int = 20,
        uploadParams: JSONObject,
        resolution: Int = 3000,
        referenceUrl: String = "",
        allowBlurCheck: Boolean = true,
        allowCrop: Boolean = true,
        uploadFrom: String = "Shelfwatch",
        isRetake: Boolean = false,
        zoomLevel: Double = 1.0,
        showOverlapToggleButton: Boolean = false,
        showGridLines: Boolean = true,
        language_code: String = "en",
        isLambda: Boolean = false
    ) {
        consumer = uploadFrom
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
        intent.putExtra("gridlines", showGridLines)
        intent.putExtra("language", language_code)
        intent.putExtra("isLambda", isLambda)

        if (context is Activity) {
            context.startActivity(intent)
        } else {
            // If the context is not an instance of Activity, add the FLAG_ACTIVITY_NEW_TASK flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun init(context: Context, bucketName: String) {
        if(bucketName.isNotEmpty()) saveBucketName(context, bucketName)
        this.bucketName = bucketName
        FirebaseApp.initializeApp(context.applicationContext)
        Bugfender.init(context.applicationContext, "lz6sMkQQVpEZXeY9o7Bi7VwyCG7wTPU6", true)
        Bugfender.enableCrashReporting()
        LogUtils.logGlobally(
            Events.BUCKET_NAME,
            "previous bucket: ${
                retrieveStringFromSharedPreferences(
                    context,
                    "bucket_prev"
                )
            } current bucket : ${retrieveStringFromSharedPreferences(context, "bucket_cur")}"
        )
        val intent = Intent(context.applicationContext, InitService()::class.java) // image Upload from gallery
        context.startService(intent)
    }

    fun uploadFailedImage(context: Context) {
        Log.d("imageSW", "uploadFailedImage")
        val intent = Intent(context.applicationContext, FailedRetryService()::class.java)
        context.startService(intent)
    }

    fun saveBucketName(context: Context, bucketName: String) {
        saveStringToSharedPreferences(
                context,
        "bucket_prev",
        retrieveStringFromSharedPreferences(context, "bucket_cur")
        )
        saveStringToSharedPreferences(
            context,
            "bucket_cur",
            bucketName
        )
    }


    fun saveStringToSharedPreferences(context: Context, key: String, value: String) {
        // Get SharedPreferences instance
        val sharedPreferences = context.getSharedPreferences("bucket_pref", Context.MODE_PRIVATE)

        // Get SharedPreferences Editor
        val editor = sharedPreferences.edit()

        // Save a string with a key
        editor.putString(key, value)

        // Apply the changes
        editor.apply()
    }

    fun retrieveStringFromSharedPreferences(context: Context, key: String): String {
        // Get SharedPreferences instance
        val sharedPreferences = context.getSharedPreferences("bucket_pref", Context.MODE_PRIVATE)

        // Retrieve the string with the key
        return sharedPreferences.getString(key, "") ?: ""
    }

    fun logout(context: Context) {
        LogUtils.logGlobally(Events.LOGOUT)
        val imageDao = AppDatabase.getInstance(context.applicationContext).imageDao()
        ScopeHelper.applicationScope.launch {
            withContext(Dispatchers.IO) {
                imageDao.deleteAllImages()
            }
        }
    }
}