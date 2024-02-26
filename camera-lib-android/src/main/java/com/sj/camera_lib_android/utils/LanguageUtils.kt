package com.sj.camera_lib_android.utils

import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import com.sj.camera_lib_android.types.LanguageType
import org.intellij.lang.annotations.Language
import java.util.Locale

object LanguageUtils {
    fun checkAndSetLanguage(desiredLanguage: String, resources: Resources) {
        try {
            if (resources.configuration.locale.language != desiredLanguage && LanguageType.isLanguagePresent(
                    desiredLanguage
                )
            ) {
                setSDKLanguage(LanguageType.getLanguageFromServerType(desiredLanguage).androidType, resources)
            }

        } catch (e: Exception) {
            LogUtils.logGlobally(Events.FAILED_TO_CHANGE_LANGUAGE, e.message.toString())
        }
    }

    private fun setSDKLanguage(localeString: String = "en", resources: Resources) {
        try {
            val locale = Locale(localeString)
            Locale.setDefault(locale)

            val configuration = Configuration(resources.configuration)

            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        } catch (e: Exception) {
            LogUtils.logGlobally(Events.FAILED_TO_CHANGE_LANGUAGE, e.message.toString())
        }
    }
}