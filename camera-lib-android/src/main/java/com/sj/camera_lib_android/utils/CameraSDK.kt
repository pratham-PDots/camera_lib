package com.sj.camera_lib_android.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sj.camera_lib_android.Database.AppDatabase
import com.sj.camera_lib_android.Database.ImageEntity
import com.sj.camera_lib_android.Database.PendingImage
import com.sj.camera_lib_android.models.ImageUploadModel
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.activities.LaunchShelfwatchCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

object CameraSDK {

    var bucketName = ""
    private var networkStateReceiver: BroadcastReceiver? = null
    var internetDisconnected = false
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
        isRetake: Boolean = false
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
        context.startActivity(intent)
    }

    fun init(activity: AppCompatActivity, bucketName: String) {
        this.bucketName = bucketName
        val imageDao = AppDatabase.getInstance(activity.applicationContext).imageDao()

        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val loadedPendingImages = imageDao.getPendingImages()
                val pendingImageList = loadedPendingImages.map { it.toPendingImage() }
                activity.runOnUiThread {
                    val intent = Intent("queue")
                    intent.putParcelableArrayListExtra(
                        "imageList",
                        ArrayList(pendingImageList)
                    )
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
                }
                uploadImages(activity, pendingImageList)
            }
        }
        initNetworkCallback(activity)
    }

    private fun uploadImages(activity: AppCompatActivity, imageList : List<PendingImage>) {
        imageList.groupBy { it.image.session_id }.forEach { (s, pendingImages) ->
            val projectId = JSONObject(pendingImages[0].image.upload_params).getString("project_id")
            uploadImage(activity, pendingImages.map { it.image }, projectId, s)
        }
    }

    private fun uploadImage(
        activity: AppCompatActivity,
        imageList: List<ImageUploadModel>,
        projectId: String,
        uuid: String
    ) {
        Log.d("imageSW", "project id = $projectId session id = $uuid image list = $imageList")
        val intent = Intent(activity, MyServices()::class.java) // image Upload from gallery
        intent.putParcelableArrayListExtra("mediaList", ArrayList(imageList))
        intent.putExtra("project_id", projectId)
        intent.putExtra("uuid", uuid)
        activity.startService(intent)
    }

    private fun initNetworkCallback(activity: AppCompatActivity) {
        // Register a BroadcastReceiver to monitor network state changes
        networkStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isNetworkConnected(activity)) {
                    // Internet connection is available, resume pending uploads
                    if(internetDisconnected) resumeUpload(activity)
                    internetDisconnected = false
                } else internetDisconnected = true
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity.registerReceiver(networkStateReceiver, filter)
    }

    private fun isNetworkConnected(activity: AppCompatActivity): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun resumeUpload(activity: AppCompatActivity) {
        val imageDao = AppDatabase.getInstance(activity.applicationContext).imageDao()
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val loadedPendingImages = imageDao.getPendingImages()
                val pendingImageList = loadedPendingImages.map { it.toPendingImage() }
                uploadImages(activity, pendingImageList)
            }
        }
    }
}