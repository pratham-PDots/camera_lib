package com.sj.camera_lib_android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object ScopeHelper {
    val applicationScope = CoroutineScope(Dispatchers.IO)
}
