package com.sj.camera_lib_android.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// ImageDao.kt

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE isUploaded = 0")
    fun getPendingImages(): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(image: ImageEntity)

    @Query("SELECT * FROM images WHERE uri = :uri")
    fun getImageByUri(uri: String): ImageEntity?

    @Update
    fun updateImage(image: ImageEntity)

    @Delete
    fun deleteImage(image: ImageEntity)
}