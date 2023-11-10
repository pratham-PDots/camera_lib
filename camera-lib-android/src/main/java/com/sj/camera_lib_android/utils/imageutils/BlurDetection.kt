package com.sj.camera_lib_android.utils.imageutils
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Context
import android.graphics.Bitmap
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


object BlurDetection {

    private val CLASSIC_MATRIX = floatArrayOf(
        -1.0f, -1.0f, -1.0f,
        -1.0f, 8.0f, -1.0f,
        -1.0f, -1.0f, -1.0f
    )

    fun runDetection(context: Context, sourceBitmap: Bitmap): Pair<Boolean, String> {
        val rs = RenderScript.create(context)


        val smootherBitmap = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, sourceBitmap.config)

        val blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs))
        val source = Allocation.createFromBitmap(rs,
            sourceBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SHARED
        )
        val blurTargetAllocation = Allocation.createFromBitmap(rs,
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


        val greyscaleBitmap = Bitmap.createBitmap(sourceBitmap.width,
            sourceBitmap.height,
            sourceBitmap.config
        )
        val smootherInput = Allocation.createFromBitmap(rs,
            smootherBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SHARED
        )
        val greyscaleTargetAllocation = Allocation.createFromBitmap(rs,
            greyscaleBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SHARED
        )

        val colorIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
        colorIntrinsic.setGreyscale()
        colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
        greyscaleTargetAllocation.copyTo(greyscaleBitmap)

        val edgesBitmap = Bitmap.createBitmap(sourceBitmap.width,
            sourceBitmap.height,
            sourceBitmap.config
        )
        val greyscaleInput = Allocation.createFromBitmap(rs,
            greyscaleBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SHARED
        )
        val edgesTargetAllocation = Allocation.createFromBitmap(rs,
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
        val isBlurry = mostLuminousColor < 11908536 //  threshold value 11908536
        // Note - in Android, Color.BLACK is -16777216 and Color.WHITE is -1, so range is somewhere in between. Higher is more luminous
        Log.d("imageSW ","blur value: $mostLuminousColor")

        return Pair(
            isBlurry,
            context.getString(R.string.result_from_renderscript,
                if (isBlurry) CameraActivity.BLURRED_IMAGE
                else CameraActivity.NOT_BLURRED_IMAGE, colorHex))
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
