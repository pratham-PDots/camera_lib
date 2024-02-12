package com.sj.camera_lib_android.utils.imageutils
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicColorMatrix
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import androidx.annotation.ColorInt
import com.sj.camera_lib_android.CameraActivity
import com.sj.camera_lib_android.R
import com.sj.camera_lib_android.utils.Events
import com.sj.camera_lib_android.utils.LogUtils
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc


object BlurDetection {

    private val CLASSIC_MATRIX = floatArrayOf(
        -1.0f, -1.0f, -1.0f,
        -1.0f, 8.0f, -1.0f,
        -1.0f, -1.0f, -1.0f
    )

    fun runDetection(context: Context, sourceBitmap: Bitmap, checkBoth: Boolean = false): Boolean {
        try {
            val rs = RenderScript.create(context)


            val smootherBitmap =
                Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, sourceBitmap.config)

            val blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs))
            val source = Allocation.createFromBitmap(
                rs,
                sourceBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val blurTargetAllocation = Allocation.createFromBitmap(
                rs,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            blurIntrinsic.apply {
                setRadius(1f)
                setInput(source)
                forEach(blurTargetAllocation)
            }
            blurTargetAllocation.copyTo(smootherBitmap)


            val greyscaleBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
            )
            val smootherInput = Allocation.createFromBitmap(
                rs,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val greyscaleTargetAllocation = Allocation.createFromBitmap(
                rs,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )

            val colorIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
            colorIntrinsic.setGreyscale()
            colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
            greyscaleTargetAllocation.copyTo(greyscaleBitmap)

            val edgesBitmap = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
            )
            val greyscaleInput = Allocation.createFromBitmap(
                rs,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )
            val edgesTargetAllocation = Allocation.createFromBitmap(
                rs,
                edgesBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
            )

            val convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
            convolve.setInput(greyscaleInput)
            convolve.setCoefficients(CLASSIC_MATRIX) // Or use others
            convolve.forEach(edgesTargetAllocation)
            edgesTargetAllocation.copyTo(edgesBitmap)

            @ColorInt val mostLuminousColor = mostLuminousColorFromBitmap(edgesBitmap)
            val colorHex = "#" + Integer.toHexString(mostLuminousColor)
            val isBlurry = mostLuminousColor < 8000000 //  threshold value 11908536
            // Note - in Android, Color.BLACK is -16777216 and Color.WHITE is -1, so range is somewhere in between. Higher is more luminous
            Log.d(
                "imageSW Logic 1",
                "blur value: $mostLuminousColor 8000000 ${Color.parseColor("#CECECE")}$isBlurry"
            )


            if (!checkBoth) return isBlurry
            else return isBlurry && isBlurry(sourceBitmap)
        } catch (e: Exception){
            return false
        }
    }

    fun isBlurry(bitmap: Bitmap): Boolean {
        try {
            val sharpnessThreshold = 40.0 // Adjust threshold as needed

            val destination = Mat()
            val matGray = Mat()
            Utils.bitmapToMat(bitmap, matGray)
            Imgproc.cvtColor(matGray, matGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.Laplacian(matGray, destination, 3)
            val median = MatOfDouble()
            val std = MatOfDouble()
            Core.meanStdDev(destination, median, std)
            val sharpnessScore = Math.pow(std.get(0, 0)[0], 2.0)

            Log.d("imageSW Logic 2", "$sharpnessScore $sharpnessThreshold")
            LogUtils.logGlobally(Events.SECOND_BLUR_VALUE, "Blur value: $sharpnessScore")

            return sharpnessScore < sharpnessThreshold
        } catch (e : Exception) {
            Log.d("imageSW Logic 2", "exception")
            return false
        }
    }



    /**
     * Resolves the most luminous color pixel in a given bitmap.
     *
     * @param bitmap Source bitmap.
     * @return The most luminous color pixel in the `bitmap`
     */
    @ColorInt
    fun mostLuminousColorFromBitmap(bitmap: Bitmap): Int {
        bitmap.setHasAlpha(false)
        val pixels = IntArray(bitmap.height * bitmap.width)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        @ColorInt var mostLuminousColor = Color.BLACK

        for (pixel in pixels) {
            if (pixel > mostLuminousColor) {
                mostLuminousColor = pixel
            }
        }
        return mostLuminousColor
    }
}
