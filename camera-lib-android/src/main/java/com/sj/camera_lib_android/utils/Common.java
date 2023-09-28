package com.sj.camera_lib_android.utils;
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Context;
import android.widget.Toast;

public class Common {

    public static boolean isReverseLandscape= false;
    public static boolean isPortrait= false;
    public static boolean isPortraitParallel= false;
    public static boolean isLandscapeParallel= false;
    public static String zoomSelected = "0";

    public static void showToast(Context context, String message) {
        Toast.makeText(context, ""+message, Toast.LENGTH_SHORT).show();
    }
}
