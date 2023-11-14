package com.sj.camera_lib_android.Database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReactPendingData(
    val session_id: String,
    val images: List<ReactPendingImage>
) : Parcelable