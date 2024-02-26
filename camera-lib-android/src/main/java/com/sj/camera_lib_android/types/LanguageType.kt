package com.sj.camera_lib_android.types

enum class LanguageType(val serverType: String, val androidType: String) {
    ENGLISH("en", "en"),
    POLISH("pl", "pl"),
    SLOVAKIA("sk", "sk"),
    ARABIC("ar", "ar"),
    CHINESE("cn", "zh");

    companion object {
        fun getLanguageFromServerType(serverType: String): LanguageType {
            return values().find { it.serverType == serverType } ?: ENGLISH
        }

        fun isLanguagePresent(serverType: String): Boolean {
            return values().any { it.serverType == serverType }
        }
    }
}