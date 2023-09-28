package com.sj.camera_lib_android
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Context
import android.widget.Toast

object Demo {
    fun showMessage(c: Context?, message: String?) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show()
    }
}