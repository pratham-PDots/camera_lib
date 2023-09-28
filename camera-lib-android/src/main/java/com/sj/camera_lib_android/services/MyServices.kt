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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.sj.camera_lib_android.models.ImageUploadModel
import java.text.SimpleDateFormat
import java.util.Locale

class MyServices : Service() {

    private var imageUploadList: MutableList<ImageUploadModel> = arrayListOf()
    private var deviceName: String = ""
    private var storage: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    companion object{
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"

    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // get the Firebase storage reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference


        if (intent != null) {
            imageUploadList = intent.getParcelableArrayListExtra<ImageUploadModel>("mediaList") as ArrayList<ImageUploadModel>
            deviceName = intent.getStringExtra("deviceName") ?: "deviceName"
            Log.d("imageSW uploadToFirebase", ", ListSize: ${imageUploadList.size}, deviceName: $deviceName")

        }

        if (imageUploadList.size > 0){
            try {
                uploadImage(imageUploadList) // Upload images to the firebase
            }catch (exception:Exception){
                Log.e("imageSW exceptionFirebase","$exception")
            }
        }

        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
    }


    private fun uploadImage(list: MutableList<ImageUploadModel>) {
        val uploadImgTime = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val fbRef = storageReference?.child("Shelfwatch")?.child("$deviceName/$uploadImgTime")
        Log.d("imageSW storageReference fbRef", "$fbRef")


        var count =0
        if (list.size> 0) {
            list.forEachIndexed { index, mediaModelClass ->
                Log.e("imageSW Service uploadImageFB", "Count: $count, listSize: ${list.size} at $index")

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

                val uri = mediaModelClass.uri
                val type = mediaModelClass.type
                val name = mediaModelClass.name

                val fileUri: Uri = Uri.fromFile(mediaModelClass.file)

                try {
                    val metadata = StorageMetadata.Builder()
                        .setContentType("image/png")
                        .setCustomMetadata("position", position)
                        .setCustomMetadata("dimension", dimension)
                        .setCustomMetadata("longitude", longitude)
                        .setCustomMetadata("latitude", latitude)
                        .setCustomMetadata("total_image_captured", total_image_captured)
                        .setCustomMetadata("app_timestamp", app_timestamp)
                        .setCustomMetadata("orientation", orientation)
                        .setCustomMetadata("zoom_level", zoom_level)
                        .setCustomMetadata("session_id", session_id)
                        .setCustomMetadata("crop_coordinates", crop_coordinates)
                        .setCustomMetadata("overlap_values", overlap_values)
                        .setCustomMetadata("uri", uri)
                        .setCustomMetadata("type", type)
                        .setCustomMetadata("name", name)
                        .build()

                    val uploadTask = fbRef?.child("images/$name")?.putFile(fileUri, metadata)

                    // Upload the byte array to Firebase Storage
                    uploadTask?.addOnSuccessListener { taskSnapshot ->
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
                            Log.e("imageSW Firebase Upload", "Fail: ${exception.printStackTrace()}, message: " + exception.message)

                        }?.addOnProgressListener { progress ->
                            val progress: Double = 100.0 * progress.getBytesTransferred() / progress.getTotalByteCount()
                            Log.i("imageSW Firebase Uploading ",
                                "at $index  Uploaded " + progress.toInt() + "%")

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