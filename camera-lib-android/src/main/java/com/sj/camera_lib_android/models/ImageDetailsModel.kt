package com.sj.camera_lib_android.models
/**
 * @author Saurabh Kumar 11 September 2023
 * **/

import android.graphics.Bitmap
import java.io.File
data class ImageDetailsModel (
    val position: String,
    val dimension: String,
    val appTimestamp: String,
    val zoomLevel: String,
    val orientation: String,
    val direction: String,
    val isAutomatic: Boolean,
    val row: Double,
    val stepsTaken: ArrayList<String>,
    val nextStep: String,
    val overlapPercent: Float,

    val imageItem: ImageModel,

    val file: File,
    val image: Bitmap,
    var croppedCoordinates: Array<Int>,


    )
