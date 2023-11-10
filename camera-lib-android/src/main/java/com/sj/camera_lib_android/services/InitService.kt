package com.sj.camera_lib_android.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sj.camera_lib_android.Database.AppDatabase
import com.sj.camera_lib_android.Database.PendingImage
import com.sj.camera_lib_android.MyApplication
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class InitService: Service() {

    private var applicationScope : CoroutineScope? = null
    private var networkStateReceiver: BroadcastReceiver? = null
    var internetDisconnected = false
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        applicationScope = (application as? MyApplication)?.applicationScope
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        doWork()
        return START_STICKY
    }

    private fun doWork() {
        val imageDao = AppDatabase.getInstance(applicationContext).imageDao()
        applicationScope?.launch {
            withContext(Dispatchers.IO) {
                val loadedPendingImages = imageDao.getPendingImages()
                val pendingImageList = loadedPendingImages.map { it.toPendingImage() }
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val intent = Intent("queue")
                    intent.putParcelableArrayListExtra(
                        "imageList",
                        ArrayList(pendingImageList)
                    )
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
                uploadImages(pendingImageList)
            }
        }
        initNetworkCallback()
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

    private fun initNetworkCallback() {
        // Register a BroadcastReceiver to monitor network state changes
        networkStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (isNetworkConnected(applicationContext)) {
                    // Internet connection is available, resume pending uploads
                    if(internetDisconnected) resumeUpload(applicationContext)
                    internetDisconnected = false
                } else internetDisconnected = true
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        applicationContext.registerReceiver(networkStateReceiver, filter)
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun resumeUpload(context: Context) {
        val imageDao = AppDatabase.getInstance(context.applicationContext).imageDao()
        applicationScope?.launch {
            withContext(Dispatchers.IO) {
                val loadedPendingImages = imageDao.getPendingImages()
                val pendingImageList = loadedPendingImages.map { it.toPendingImage() }
                uploadImages(pendingImageList)
            }
        }
    }
}