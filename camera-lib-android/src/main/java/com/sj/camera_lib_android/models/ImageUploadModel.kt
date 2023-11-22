package com.sj.camera_lib_android.models
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File


@Parcelize
data class ImageUploadModel(
    val position: String,
    val dimension: String,
    val longitude: String,
    val latitude: String,
    val total_image_captured: String,
    val app_timestamp: String,
    val orientation: String,
    val zoom_level: String,
    val session_id: String,
    val crop_coordinates: String,
    val overlap_values: String,
    val upload_params: String,
    val uri: String,
    val type: String,
    val name: String,
    val last_image_flag: String  = "0"
) : Parcelable