package com.sj.camera_lib_android.utils.imageutils
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.json.JSONArray
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat


object ImageProcessingUtils {

    fun calculateAveragePixelIntensity(bitmap: Bitmap): Float {
        var sum = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val intensity = (red + green + blue) / 3
            sum += intensity
        }

        return sum.toFloat() / (bitmap.width * bitmap.height)
    }
    fun isLowLightImage(bitmap: Bitmap): Boolean {
        val averageIntensity = calculateAveragePixelIntensity(bitmap)
        val threshold = 85f //100f: Adjust this threshold according to your requirements
        Log.d("imageSW LowLight", "averageIntensity==>> $averageIntensity")

        return Math.abs(averageIntensity) < threshold
    }

    fun rotateBitmapWithOpenCV(srcBitmap: Bitmap): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(srcBitmap, srcMat)

        val rotationDegrees = Core.ROTATE_90_CLOCKWISE

        val rotatedMat = Mat()
        Core.rotate(srcMat, rotatedMat, rotationDegrees)

        val dstBitmap = Bitmap.createBitmap(rotatedMat.cols(), rotatedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rotatedMat, dstBitmap)

        return dstBitmap
    }
}