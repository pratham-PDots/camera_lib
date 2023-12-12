package com.sj.camera_lib_android.models
/**
 * @author Saurabh Kumar 11 September 2023
 * **/

import android.graphics.Bitmap
import java.io.File
data class ImageDetailsModel(
    val position: IntArray,
    val dimension: IntArray,
    val appTimestamp: String,
    val zoomLevel: String,
    val orientation: String,
    val direction: String,
    val isAutomatic: Boolean,
    val row: Double,
    val stepsTaken: ArrayList<String>,
    val nextStep: String,
    val overlapPercent: String,
    val uploadParams: String,

    val imageItem: ImageModel,

    val file: File,
    val image: Bitmap,
    var croppedCoordinates: Array<Int>,
    var gyroHorizontal: Float = 0f,
    var gyroVertical: Float = 0f
    )
