package com.sj.camera_lib_android.services
/**
 * @author Saurabh Kumar 11 September 2023
 * **/

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bugfender.sdk.Bugfender
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.sj.camera_lib_android.Database.AppDatabase
import com.sj.camera_lib_android.Database.ImageEntity
import com.sj.camera_lib_android.Database.ReactPendingData
import com.sj.camera_lib_android.Database.ReactSingleImage
import com.sj.camera_lib_android.ScopeHelper
import com.sj.camera_lib_android.models.ImageUploadModel
import com.sj.camera_lib_android.utils.CameraSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MyServices : Service() {

    private var imageUploadList: MutableList<ImageUploadModel> = arrayListOf()
    private var deviceName: String = ""
    private var storage: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    private var applicationScope : CoroutineScope? = null
    companion object{
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"

    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        applicationScope = ScopeHelper.applicationScope
        super.onCreate()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // get the Firebase storage reference
        Log.d("imageSW bucket", CameraSDK.bucketName)
        storage = FirebaseStorage.getInstance(CameraSDK.bucketName)
        storageReference = storage!!.reference


        if (intent != null) {
            imageUploadList = intent.getParcelableArrayListExtra<ImageUploadModel>("mediaList") as ArrayList<ImageUploadModel>
            deviceName = intent.getStringExtra("deviceName") ?: "deviceName"
            Log.d("imageSW uploadToFirebase", ", ListSize: ${imageUploadList.size}, deviceName: $deviceName")

        }

        if (imageUploadList.size > 0){
            try {
                uploadImage(imageUploadList, sessionId = intent?.getStringExtra("uuid") ?: "pratham", projectId = intent?.getStringExtra("project_id") ?: "pratham") // Upload images to the firebase
            }catch (exception:Exception){
                Log.e("imageSW exceptionFirebase","$exception")
            }
        }

        return START_STICKY
    }

    private fun broadCastQueue() {
            val imageDao = AppDatabase.getInstance(this@MyServices.applicationContext).imageDao()
            val loadedPendingImages = imageDao.getPendingImages()
            val intent = Intent("did-receive-queue-data")
            intent.putParcelableArrayListExtra(
                "imageList",
                ArrayList(loadedPendingImages.map { it.toPendingImage() }.groupBy { it.image.session_id }.map { imageMap ->
                    ReactPendingData(imageMap.key, imageMap.value.map { it.toReactPendingImage() })
                })
            )
            LocalBroadcastManager.getInstance(this@MyServices.applicationContext).sendBroadcast(intent)
    }

    private fun broadCastImage(image: ReactSingleImage) {
        val intent = Intent("did-receive-image-upload-status")
        intent.putExtra("image", image)
        LocalBroadcastManager.getInstance(this@MyServices.applicationContext).sendBroadcast(intent)
    }



    override fun onDestroy() {
        Log.d("imageSW", "Service destroyed")
        super.onDestroy()
    }


    private fun addImageToQueue(image: ImageUploadModel) {
        val imageDao = AppDatabase.getInstance(this.applicationContext).imageDao()
        if(imageDao.getImageByUri(image.uri) == null)
            imageDao.insertImage(ImageEntity(image = image, uri = image.uri, isUploaded = false))
        Log.d("imageSW add", image.uri)
    }

    private fun modifyImage(image: ImageUploadModel, error: String) {
        val imageDao = AppDatabase.getInstance(this.applicationContext).imageDao()
        val imageEntity = imageDao.getImageByUri(image.uri)
        imageEntity?.error = error
        if (imageEntity != null) {
            imageDao.updateImage(imageEntity)
        }
        Log.d("imageSW modify", image.uri)
    }

    private fun removeImageFromQueue(image: ImageUploadModel) {
        val imageDao = AppDatabase.getInstance(this.applicationContext).imageDao()
        val imageEntity = imageDao.getImageByUri(image.uri)
        Log.d("imageSW remove", "${imageEntity?.uri}")
        imageEntity?.let { imageDao.deleteImage(it) }
        image.uri.let {
            val file = File(it)
            Log.d("imageSW file deleted", file.delete().toString())
        }
    }

    private fun getStringifiedMetadata(metadata: StorageMetadata): String {
        val keys = metadata.customMetadataKeys
        val jsonObject = JSONObject()

        for (key in keys) {
            val value = metadata.getCustomMetadata(key)
            if (value != null) {
                jsonObject.put(key, value)
            }
        }

        return jsonObject.toString()
    }




    private fun uploadImage(list: MutableList<ImageUploadModel>, projectId : String = "", sessionId: String = "") {


        val uploadImgTime = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        var tdVersion = ""
        if(list.isNotEmpty()) {
            val json = JSONObject(list[0].upload_params)
            if(json.has("test_image_version_id"))
                tdVersion = JSONObject(list[0].upload_params)["test_image_version_id"].toString()
        }
        var fbRef: StorageReference? = if(tdVersion.isNotEmpty())
            storageReference?.child("dist/test_images/${projectId}_${tdVersion}")
        else storageReference?.child("dist/test_images/$projectId")
        Log.d("imageSW storageReference fbRef", "$fbRef")


        var count =0
        if (list.size> 0) {
            list.forEachIndexed { index, mediaModelClass ->
                applicationScope?.launch {
                    addImageToQueue(mediaModelClass)
                    broadCastQueue()
                }
                Log.e("imageSW Service uploadImageFB", "Count: $count, listSize: ${list.size} at $index")


                val upload_params = mediaModelClass.upload_params
                val position = mediaModelClass.position
                val dimension = mediaModelClass.dimension
                val longitude = mediaModelClass.longitude
                val latitude = mediaModelClass.latitude
                val total_image_captured = mediaModelClass.total_image_captured
                val app_timestamp = mediaModelClass.app_timestamp
                val orientation = mediaModelClass.orientation
                val zoom_level = mediaModelClass.zoom_level
                val session_id = mediaModelClass.session_id
                val crop_coordinates = mediaModelClass.crop_coordinates
                val overlap_values = mediaModelClass.overlap_values
                val last_image_flag = mediaModelClass.last_image_flag

                val uri = mediaModelClass.uri
                val type = mediaModelClass.type
                val name = mediaModelClass.name

                val fileUri: Uri = Uri.parse("file://$uri")


                Log.d("imageSW file", "$fileUri $uri")

                Log.d("imageSW", upload_params)

                try {
                    val metadata = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .setCustomMetadata("position", position)
                        .setCustomMetadata("dimension", dimension)
                        .setCustomMetadata("longitude", longitude)
                        .setCustomMetadata("latitude", latitude)
                        .setCustomMetadata("total_image_captured", total_image_captured)
                        .setCustomMetadata("app_timestamp", app_timestamp)
                        .setCustomMetadata("orientation", orientation)
                        .setCustomMetadata("zoom_level", zoom_level)
                        .setCustomMetadata("session_id", sessionId)
                        .setCustomMetadata("crop_coordinates", crop_coordinates)
                        .setCustomMetadata("overlap_values", overlap_values)
                        .setCustomMetadata("uri", uri)
                        .setCustomMetadata("type", type)
                        .setCustomMetadata("name", name)
                        .setCustomMetadata("last_image_flag", last_image_flag)

                    val uploadParamJson = JSONObject(upload_params)
                    uploadParamJson.put("app_session_id", sessionId)
                    uploadParamJson.put("seq_no", (index + 1).toString())
                    if(list.size > 1) uploadParamJson.put("image_type", "multiple")

                    for(key in uploadParamJson.keys()) {
                        metadata.setCustomMetadata(key, uploadParamJson[key].toString())
                    }

                    val uploadTask = fbRef?.child(name)?.putFile(fileUri, metadata.build())



                    // Upload the byte array to Firebase Storage
                    uploadTask?.addOnSuccessListener { taskSnapshot ->
                            applicationScope?.launch {
                                Log.d("imageSW", "remove queue")
                                Bugfender.d("native-image-upload-success", "reference: $fbRef name: $name")
                                removeImageFromQueue(mediaModelClass)
                                broadCastQueue()
                                broadCastImage(ReactSingleImage(
                                    uri = mediaModelClass.uri,
                                    error = "",
                                    status = true,
                                    imageData = getStringifiedMetadata(metadata.build())
                                ))
                            }

                            // Handle successful upload
                            // You can get the download URL if needed:
                            val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl
                            Log.i("imageSW Firebase Upload", "Success, downloadUrl: $downloadUrl")

                            count++
                            if (count == list.size) {
                                Log.d("imageSW Service MediaList", "Count: $count, list.size ${list.size} at $index")

                                val filter = "thisIsForMyPartner"
                                val intent = Intent(filter)
                                intent.putExtra("index", count)
                                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                            }
//                Toast.makeText(requireContext(), "Uploaded", Toast.LENGTH_SHORT).show()

                        }?.addOnFailureListener { exception ->
                            // Handle failed upload
                            applicationScope?.launch {
                                modifyImage(mediaModelClass, exception.message.toString())
                                broadCastImage(ReactSingleImage(
                                    uri = mediaModelClass.uri,
                                    error = exception.message.toString(),
                                    status = false,
                                    imageData = getStringifiedMetadata(metadata.build())
                                ))
                            }
                            Log.e("imageSW Firebase Upload", "Fail: ${exception.printStackTrace()}, message: " + exception.message)

                        }?.addOnProgressListener { progress ->
                            val progress: Double = 100.0 * progress.getBytesTransferred() / progress.getTotalByteCount()
                            Log.i("imageSW Firebase Uploading ",
                                "at $index  Uploaded " + progress.toInt() + "%")
                            val intent = Intent("Progress")
                            intent.putExtra("index",sessionId + "_" + "${index + 1}")
                            intent.putExtra("progress", progress.toInt())
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                        }

                }catch(e:Exception){
                        Log.e("imageSW firebaseUploadException","${e.printStackTrace()}")
                    }
            }

        }else{
            Log.e("Service: imageUploadList EMPTY", "at ${list.size}")

        }

    }
    fun stopService() {
        stopSelf()
    }
}