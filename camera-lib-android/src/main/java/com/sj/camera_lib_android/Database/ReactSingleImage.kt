package com.sj.camera_lib_android.Database

import android.os.Parcelable
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReactSingleImage(
    val uri: String,
    var error: String,
    val status: Boolean,
    val imageData: ImageUploadModel
) : Parcelable