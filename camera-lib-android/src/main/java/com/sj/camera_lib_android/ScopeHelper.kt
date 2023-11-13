package com.sj.camera_lib_android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object MyApplication {
    val applicationScope = CoroutineScope(Dispatchers.IO)
}
