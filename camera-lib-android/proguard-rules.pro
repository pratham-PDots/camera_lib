# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class androidx.appcompat.widget.** { *; }
# Keep the CameraSDK class and its methods
-keep class com.sj.camera_lib_android.utils.CameraSDK {
    *;
}

-keep class com.sj.camera_lib_android.models.* {
    *;
}

-keep class com.sj.camera_lib_android.Database.* {
    *;
}

-keep class com.example.dynamic.* {
    *;
}

# Keep the specified class
-keep class java.lang.*  { *; }
-dontwarn java.lang.invoke.StringConcatFactory
# Add other ProGuard rules specific to your library dependencies as needed
