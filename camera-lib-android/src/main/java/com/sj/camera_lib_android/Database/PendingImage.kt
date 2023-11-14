package com.sj.camera_lib_android.Database

import android.os.Parcelable
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class PendingImage(
    val uri: String,
    var isUploaded: Boolean,
    var error: String,
    var image: ImageUploadModel
) : Parcelable {
    fun toReactPendingImage() : ReactPendingImage {
        return ReactPendingImage(
            uri = this.uri,
            upload_status = this.isUploaded,
            error = this.error
        )
    }
}