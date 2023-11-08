package com.sj.camera_lib_android.Database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sj.camera_lib_android.models.ImageUploadModel
import kotlinx.parcelize.Parcelize

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val image: ImageUploadModel,
    val uri: String,
    var isUploaded: Boolean = false,
    var error: String = ""
) {
    fun toPendingImage() : PendingImage {
        return PendingImage(
            uri = this.uri,
            isUploaded = this.isUploaded,
            error = this.error,
            image = this.image
        )
    }
}