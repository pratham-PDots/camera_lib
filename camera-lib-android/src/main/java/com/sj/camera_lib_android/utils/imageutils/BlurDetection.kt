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
import com.google.errorprone.annotations.NoAllocation
import com.sj.camera_lib_android.CameraActivity
import com.sj.camera_lib_android.R
import com.sj.camera_lib_android.utils.Events
import com.sj.camera_lib_android.utils.LogUtils
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


object BlurDetection {

    private val CLASSIC_MATRIX = floatArrayOf(
        -1.0f, -1.0f, -1.0f,
        -1.0f, 8.0f, -1.0f,
        -1.0f, -1.0f, -1.0f
    )

    private var rs: RenderScript? = null

    fun runDetection(context: Context, sourceBitmap: Bitmap): Pair<Boolean, String> {

        try {
            if(rs == null) {
                rs = RenderScript.create(context)
            }

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
            Log.d("imageSW ", "blur value: $mostLuminousColor")


            if (smootherBitmap != null && !smootherBitmap.isRecycled) {
                LogUtils.logGlobally(Events.BITMAP_RECYCLED, "smootherBitmap")
                smootherBitmap.recycle()
            }

            if (greyscaleBitmap != null && !greyscaleBitmap.isRecycled) {
                LogUtils.logGlobally(Events.BITMAP_RECYCLED, "greyscaleBitmap")
                greyscaleBitmap.recycle()
            }

            if (edgesBitmap != null && !edgesBitmap.isRecycled) {
                LogUtils.logGlobally(Events.BITMAP_RECYCLED, "edgesBitmap")
                edgesBitmap.recycle()
            }

            destroyAllocation(source)
            destroyAllocation(greyscaleInput)
            destroyAllocation(smootherInput)
            destroyAllocation(edgesTargetAllocation)
            destroyAllocation(greyscaleTargetAllocation)
            destroyAllocation(blurTargetAllocation)

            return Pair(
                isBlurry,
                context.getString(
                    R.string.result_from_renderscript,
                    if (isBlurry) CameraActivity.BLURRED_IMAGE
                    else CameraActivity.NOT_BLURRED_IMAGE, colorHex
                )
            )
        } catch (e : Exception) {
            LogUtils.logGlobally(Events.BLUR_FAILED, e.message.toString())
            return Pair(false, "")
        }
    }

    fun checkBlurryImage(bitmap: Bitmap): Pair<Boolean, String> {
        try {
            val options = BitmapFactory.Options()
            options.inDither = true
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val l = CvType.CV_8UC1 //8-bit grey scale image
            val matImage = Mat()
            Utils.bitmapToMat(bitmap, matImage)
            val matImageGrey = Mat()
            Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY)
            val destImage: Bitmap = Bitmap.createBitmap(bitmap)
            val dst2 = Mat()
            Utils.bitmapToMat(destImage, dst2)
            val laplacianImage = Mat()
            dst2.convertTo(laplacianImage, l)
            Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U)
            val laplacianImage8bit = Mat()
            laplacianImage.convertTo(laplacianImage8bit, l)
            val bmp = Bitmap.createBitmap(
                laplacianImage8bit.cols(),
                laplacianImage8bit.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(laplacianImage8bit, bmp)
            val pixels = IntArray(bmp.height * bmp.width)
            bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            var maxLap = -16777216 // 16m
            for (pixel in pixels) {
                if (pixel > maxLap) maxLap = pixel
            }

            val soglia = -8118750
            if (maxLap <= soglia) {
                println("is blur image")
                Log.d("imageSW BlurValue","maxLap $maxLap ")
                return Pair(true, "")
            }
        } catch (e: java.lang.Exception) {
            return Pair(false, "")
        }
        return Pair(false, "")
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

    private fun destroyAllocation(allocation: Allocation) {
        try {
            allocation.destroy()
        } catch (_: Exception) {
            Log.d("imageSW destroy", allocation.name)
        }
    }
}
