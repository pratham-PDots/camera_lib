package com.sj.camera_lib_android.utils

import android.util.Log
import com.bugfender.sdk.Bugfender

object LogUtils {
    fun logLocally(event: String, description: String = "") {
        Log.d(event, description)
    }

    fun logGlobally(event: String, description: String = "") {
        logLocally(event, description)
        Bugfender.d(event, description)
    }
}