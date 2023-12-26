package com.sj.camera_lib_android.types

enum class LanguageType(val serverType: String) {
    ENGLISH("en"),
    POLISH("pl");

    companion object {
        fun getLanguageFromServerType(serverType: String): LanguageType {
            return values().find { it.serverType == serverType } ?: ENGLISH
        }

        fun isLanguagePresent(serverType: String): Boolean {
            return values().any { it.serverType == serverType }
        }
    }
}