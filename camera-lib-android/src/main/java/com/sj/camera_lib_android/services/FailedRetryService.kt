package com.sj.camera_lib_android.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.sj.camera_lib_android.Database.AppDatabase
import com.sj.camera_lib_android.Database.PendingImage
import com.sj.camera_lib_android.ScopeHelper
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class FailedRetryService: Service() {

    private var applicationScope : CoroutineScope? = null
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        applicationScope = ScopeHelper.applicationScope
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        doWork()
        return START_STICKY
    }

    private fun doWork() {
        Log.d("imageSW FailedService", "doWork")
        val imageDao = AppDatabase.getInstance(applicationContext).imageDao()
        applicationScope?.launch {
            withContext(Dispatchers.IO) {
                val loadedPendingImages = imageDao.getFailedImages()
                Log.d("imageSW FailedService", "failedImageList: $loadedPendingImages")
                val pendingImageList = loadedPendingImages.map { it.toPendingImage() }
                uploadImages(pendingImageList)
            }
        }
    }


    private fun uploadImages(imageList: List<PendingImage>) {
        imageList.groupBy { it.image.session_id }.forEach { (s, pendingImages) ->
            val projectId = JSONObject(pendingImages[0].image.upload_params).getString("project_id")
            uploadImage(pendingImages.map { it.image }, projectId, s)
        }
    }

    private fun uploadImage(
        imageList: List<ImageUploadModel>,
        projectId: String,
        uuid: String
    ) {
        Log.d("imageSW", "project id = $projectId session id = $uuid image list = $imageList")
        val intent = Intent(applicationContext, MyServices()::class.java) // image Upload from gallery
        intent.putParcelableArrayListExtra("mediaList", ArrayList(imageList))
        intent.putExtra("project_id", projectId)
        intent.putExtra("uuid", uuid)
        applicationContext.startService(intent)
    }
}