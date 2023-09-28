package com.sj.camera_lib_android.utils.imageutils
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.json.JSONArray


object ImageProcessingUtils {
    /*
        fun isImageBlurry(uri: Uri): Boolean {
            // Load and convert the image to Bitmap
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Convert Bitmap to Mat (OpenCV's image format)
            val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
            Utils.bitmapToMat(bitmap, mat)

            // Convert the image to grayscale
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

            // Apply Laplacian filter for edge detection
            val laplacian = Mat()
            Imgproc.Laplacian(mat, laplacian, CvType.CV_64F)

            // Calculate sharpness score
            val sharpness = Core.sumElems(laplacian).`val`[0]

            // Define a threshold for blur
            val blurThreshold = 1000.0 // Adjust this threshold as needed

            // Check if the sharpness score is below the threshold
            return sharpness < blurThreshold
        }
    */
/*    fun isImageBlurry(bitmap: Bitmap): Boolean {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        val laplacian = Mat()
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F)
        val variance = Core.mean(laplacian).`val`[0]
        Log.d("imageSW BlurValue","$variance")
        return variance < 100 // Adjust the threshold as needed
    }*/

/*
    fun checkBluryImg(capturedBtmpImg: Bitmap):Boolean {
        try {
            val options = BitmapFactory.Options()
            options.inDither = true
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
//            val decodedString: ByteArray = Base64.decode(imageAsBase64, Base64.DEFAULT)
//            val image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)


//      Bitmap image = decodeSampledBitmapFromFile(imageurl, 2000, 2000);
            val l = CvType.CV_8UC1 //8-bit grey scale image
            val matImage = Mat()
            Utils.bitmapToMat(capturedBtmpImg, matImage)
            val matImageGrey = Mat()
            Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY)
            val destImage: Bitmap = Bitmap.createBitmap(capturedBtmpImg)
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

//            int soglia = -6118750;
            val soglia = -8118750
            if (maxLap <= soglia) {
                println("is blur image")
                Log.d("imageSW BlurValue","maxLap $maxLap ")
                return true
            }
        } catch (e: java.lang.Exception) {
            Log.d("imageSW blurException"," "+e.printStackTrace())
//            Bugfender.e("android_imageSW blurException"," "+e.printStackTrace())

        }

        return false
    }
*/

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


    fun enhanceImageBrightness(image: Bitmap, brightnessFactor: Float): Bitmap {
        val width = image.width
        val height = image.height
        val enhancedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = image.getPixel(x, y)

                // Extract the color channels
                val alpha = Color.alpha(pixel)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                // Increase the brightness of each channel
                val enhancedRed = enhanceBrightnessChannel(red, brightnessFactor)
                val enhancedGreen = enhanceBrightnessChannel(green, brightnessFactor)
                val enhancedBlue = enhanceBrightnessChannel(blue, brightnessFactor)

                // Create the enhanced pixel
                val enhancedPixel = Color.argb(alpha, enhancedRed, enhancedGreen, enhancedBlue)

                // Set the enhanced pixel in the enhanced image
                enhancedImage.setPixel(x, y, enhancedPixel)
            }
        }

        return enhancedImage
    }

    private fun enhanceBrightnessChannel(channel: Int, brightnessFactor: Float): Int {
        val enhancedChannel = channel * brightnessFactor
        return enhancedChannel.coerceIn(0f, 255f).toInt()
    }


    fun jsonArrayToArrayList(jsonArray: JSONArray): ArrayList<String> {
        val arrayList = ArrayList<String>()

        for (i in 0 until jsonArray.length()) {
            val element = jsonArray.getString(i)
            arrayList.add(element)
        }

        Log.d("imageSW","arrayList : $arrayList")
        return arrayList
    }



}