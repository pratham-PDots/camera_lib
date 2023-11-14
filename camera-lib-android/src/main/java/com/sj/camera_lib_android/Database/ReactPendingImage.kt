package com.sj.camera_lib_android.Database

import android.os.Parcelable
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReactPendingImage(
    val uri: String,
    var upload_status: Boolean,
    var error: String
) : Parcelable