package com.sj.camera_lib_android.Database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.sj.camera_lib_android.models.ImageUploadModel

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromImageUploadModel(imageUploadModel: ImageUploadModel): String {
        return Gson().toJson(imageUploadModel)
    }

    @TypeConverter
    @JvmStatic
    fun toImageUploadModel(imageUploadModelJson: String): ImageUploadModel {
        return Gson().fromJson(imageUploadModelJson, ImageUploadModel::class.java)
    }
}