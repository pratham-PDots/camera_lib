package com.sj.camera_lib_android

/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bugfender.sdk.Bugfender
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.sj.camera_lib_android.ui.interfaces.Backpressedlistener
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.ImageDialog
import com.sj.camera_lib_android.ui.SubmitDialog
import com.sj.camera_lib_android.ui.adapters.PreviewListAdapter
import com.sj.camera_lib_android.ui.viewmodels.CameraViewModel
import com.sj.camera_lib_android.utils.Common
import com.sj.camera_lib_android.utils.Utils
import com.sj.camera_lib_android.utils.imageutils.BlurDetection
import com.sj.camera_lib_android.utils.imageutils.ImageProcessingUtils
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan
import kotlin.math.max


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

class CameraActivity : AppCompatActivity(), Backpressedlistener {
    private var imageCapture: ImageCapture? = null
    private lateinit var viewModel: CameraViewModel
    var isArrowSelected: Boolean = true
    private var mFile: File? = null
    private var mBitmap: Bitmap? = null
    private var captureTime: String = ""
    private var deviceName: String = ""

    //    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView

    private lateinit var captureImg: ImageView
    private lateinit var deleteImg: ImageView
    private lateinit var previewPageImgCS: ImageView
    private lateinit var crossImg: ImageView
    private lateinit var referenceImg: ImageView
    private lateinit var overlayGroup: Group
    private lateinit var zoomText: TextView

    private lateinit var overlapImgTop: ImageView
    private lateinit var overlapImgLeft: ImageView
    private lateinit var overlapImgRight: ImageView

    private lateinit var leftArrowIv: ImageView
    private lateinit var leftArrowTv: TextView
    private lateinit var rightArrowIv: ImageView
    private lateinit var rightArrowTv: TextView
    private lateinit var downArrowIv: ImageView
    private lateinit var downArrowTv: TextView


    private lateinit var cameraLayout: ConstraintLayout
    private lateinit var cropLayout: ConstraintLayout
    private lateinit var blurLayout: ConstraintLayout
    private lateinit var submitBtn1: LinearLayout
    private lateinit var loader: View
    private lateinit var orientationBl: View
    private lateinit var orientationTv: TextView

    private lateinit var cropImageViewCL: CropImageView
    private lateinit var resetCropBtnCL: Button
    private lateinit var retakeCropBtnCL: Button
    private lateinit var cropDoneBtnCL: Button

    private lateinit var imageBlur: ImageView
    private lateinit var retakeBlurImg: Button
    private lateinit var notBlurContinueLL: LinearLayout

    // for preview Screen
    private lateinit var previewImgLayout: ConstraintLayout
    private lateinit var cropLayoutPS: ConstraintLayout
    private lateinit var imageShowLayoutPS: ConstraintLayout
    private lateinit var uploadBtnPS: LinearLayout
    private lateinit var exitBtnPS: ImageView
    private lateinit var cropStartPS: ImageView
    private lateinit var previewImgPS: ImageView
    private lateinit var captureMorePS: ImageView
    private lateinit var previewImgRecycler: RecyclerView

    private lateinit var cropImageViewPS: CropImageView
    private lateinit var resetCropBtnPS: Button
    private lateinit var cropDoneBtnPS: Button

    private var mFilePS: File? = null
    private var mBitmapPS: Bitmap? = null

    private var mPosPS: Int = -1
    private var croppingPointsPS: Array<Int> = emptyArray()
    private var cropRectValuesPS: Rect? = null

    private var coordinatesCrop: Array<Int> = emptyArray()
    private var widthNewVF: Int? = null
    private var heightNewVF: Int? = null
    private var resizedWidthNew: Int? = null
    private var resizedHeightNew: Int? = null

    //Zoom
    private var camera: Camera? = null

    private lateinit var wideAngleButton: Button

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val scaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (viewModel.isRetake || !viewModel.zoomEnabled) return false

                val newZoomRatio = viewModel.currentZoomRatio * detector.scaleFactor

                setZoomRatio(newZoomRatio)

                return true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // hide the action bar
        supportActionBar?.hide()
        val utils = Utils()

//        InternetCheck
        if (!utils.checkInternetConnection(this)) {
            Toast.makeText(
                this,
                "Opps! No Internet\nPlease Connect to Internet",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Initialize Views ID
        initializeIDs()
        // Initialize OpenCV

        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext)
            Log.d("imageSW FirebaseApp", "initialized")
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this@CameraActivity)[CameraViewModel::class.java]
        deviceName = viewModel.getDeviceModel() // getDevice Name Model
        Log.i("imageSW deviceName", "$deviceName")

        //Show Hide Layouts
        cameraLayout.visibility = View.VISIBLE
        previewImgLayout.visibility = View.GONE
        cropLayout.visibility = View.GONE
        blurLayout.visibility = View.GONE


        val extras = intent.extras
        var modeRotation = ""
        var overlayBE = 20f
        var uploadParams = ""
        var resolution = ""
        var referenceUrl = ""
        var isBlurFeature = ""
        var isCropFeature = ""
        var uploadFrom = ""

        if (extras != null) {
            modeRotation = extras.getString("mode") ?: ""
            overlayBE = extras.getFloat("overlapBE")
            uploadParams = extras.getString("uploadParam") ?: ""
            resolution = extras.getString("resolution") ?: ""
            referenceUrl = extras.getString("referenceUrl") ?: ""
            isBlurFeature = extras.getString("isBlurFeature") ?: ""
            isCropFeature = extras.getString("isCropFeature") ?: ""
            uploadFrom = extras.getString("uploadFrom") ?: "Shelfwatch"
            viewModel.isRetake = extras.getBoolean("isRetake", false)
            viewModel.currentZoomRatio = extras.getDouble("zoomLevel", 1.0)


            var message =
                "Mode: $modeRotation ,uploadParams: $uploadParams , overlayBE: $overlayBE," +
                        " resolution: $resolution, referenceUrl: $referenceUrl, isBlurFeature: $isBlurFeature," +
                        " isCropFeature: $isCropFeature, uploadFrom: $uploadFrom, retake : ${viewModel.isRetake}"
            Log.d("imageSW inputFlags", " CA: $message")
            viewModel.dataFromOpenCameraEvent(
                applicationContext,
                modeRotation,
                overlayBE,
                uploadParams,
                resolution,
                referenceUrl,
                isBlurFeature,
                isCropFeature,
                uploadFrom
            )


        }

        viewModel.discardAllImages() // cameraActivity Launch
        // register BroadcastReceiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(myBroadcastReceiver, IntentFilter("thisIsForMyPartner"))


        modeRotation = viewModel.mode

        // Rotation work
        if (modeRotation.isNotEmpty() && modeRotation == "landscape") {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Log.d(
                "imageSW",
                "modeRotation: $modeRotation ,==> landscape ==> modeSelected: $modeRotation "
            )

            setLayoutParams("4:3")

            val layoutParamPS2 = cropImageViewPS.layoutParams as ConstraintLayout.LayoutParams
            layoutParamPS2.dimensionRatio = "4:3"
            cropImageViewPS.layoutParams = layoutParamPS2


            // calculation for resizing
            resizedWidthNew = resolution.toInt()
            resizedHeightNew = (resolution.toInt() * 3) / 4
            Log.d("imageSW resizeNEW: ", "Landscape WH: $resizedWidthNew, $resizedHeightNew")

        } else {
            resizedWidthNew = (resolution.toInt() * 3) / 4
            resizedHeightNew = resolution.toInt()
            Log.d("imageSW resizeNEW: ", "Portrait WH: $resizedWidthNew, $resizedHeightNew")

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.d(
                "imageSW",
                "modeRotation: $modeRotation ,==> portrait ==> modeSelected: $modeRotation"
            )


            setLayoutParams("3:4")

            val layoutParam3 = cropImageViewCL.layoutParams as ConstraintLayout.LayoutParams
            layoutParam3.dimensionRatio = "3:4"
            cropImageViewCL.layoutParams = layoutParam3

        }


        // Code for orientation check
        if (modeRotation.isNotEmpty()) {

            val orientationEventListener: OrientationEventListener =
                object : OrientationEventListener(this) {
                    override fun onOrientationChanged(orientation: Int) {
                        var angle: Int = orientation
                        var orientationName = getOrientation(angle)

                        when (orientationName) {
                            Orientation_PORTRAIT -> {
                                if (modeRotation.isNotEmpty() && modeRotation.equals(
                                        "portrait",
                                        true
                                    )
                                ) {
                                    orientationBl.visibility = View.INVISIBLE

                                } else {
                                    orientationTv.text = "Change orientation to landscape"
                                    orientationBl.visibility = View.VISIBLE
                                }
                            }

                            ORIENTATION_REVERSE_PORTRAIT -> {
                                orientationTv.text = "Change orientation to $modeRotation"
                                orientationBl.visibility = View.VISIBLE
                            }

                            ORIENTATION_LANDSCAPE -> {
                                if (modeRotation.equals("landscape", true)) {
                                    orientationBl.visibility = View.INVISIBLE

                                } else {
                                    orientationTv.text = "Change orientation to portrait"
                                    orientationBl.visibility = View.VISIBLE
                                }
                            }

                            ORIENTATION_REVERSE_LANDSCAPE -> {
                                orientationTv.text = "Change orientation to $modeRotation"
                                orientationBl.visibility = View.VISIBLE

                            }

                            ORIENTATION_PARALLEL -> {
                                if (modeRotation.equals(
                                        "portrait",
                                        true
                                    ) && Common.isPortraitParallel
                                ) {

                                    orientationBl.visibility = View.INVISIBLE
                                } else if (modeRotation.equals(
                                        "landscape",
                                        true
                                    ) && Common.isLandscapeParallel
                                ) {
                                    orientationBl.visibility = View.INVISIBLE
                                }
                            }

                        }

                    }
                }
            orientationEventListener.enable()

        } else finish()


        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCameraW()

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { it ->
                withContext(Dispatchers.Main) {
                    Log.d(
                        "imageSW state_livedata:",
                        "leftArrow: ${it.showLeftArrow}, rightArrow: ${it.showRightArrow}, downArrow: ${it.showDownArrow}, All Directions: ${it.showArrowAllDirection}"
                    )
                    if (it.showLoader) {
                        loader.visibility = View.VISIBLE
                    } else {
                        captureImg.setBackgroundResource(R.drawable.white_solid_circle)
                        loader.visibility = View.GONE
                    }

                    if (viewModel.isRetake) overlayGroup.isVisible = false

                    Log.d("imageSW it.enableCaptureBtn", " ${it.enableCaptureBtn}")
                    captureImg.isEnabled = it.enableCaptureBtn


                    //arrows

                    if (it.showArrowAllDirection) {
                        isArrowSelected = false
                        //make them visible
                        leftArrowIv.visibility = View.VISIBLE
                        downArrowIv.visibility = View.VISIBLE
                        rightArrowIv.visibility = View.VISIBLE
                        leftArrowIv.setImageResource(R.drawable.img_13)
                        downArrowIv.setImageResource(R.drawable.img_15)
                        rightArrowIv.setImageResource(R.drawable.img_14)

                        leftArrowTv.visibility = View.GONE
                        rightArrowTv.visibility = View.GONE
                        downArrowTv.visibility = View.GONE

                        leftArrowIv.isEnabled = true
                        rightArrowIv.isEnabled = true
                        downArrowIv.isEnabled = true
                    } else {

                        isArrowSelected = true
                        if (it.showLeftArrow) {
                            leftArrowIv.visibility = View.VISIBLE
                            leftArrowTv.visibility = View.VISIBLE
                            leftArrowIv.isEnabled = false
                            leftArrowIv.setImageResource(R.drawable.img_9)
                            leftArrowIv.setBackgroundColor(Color.TRANSPARENT)

                        } else {
                            leftArrowIv.visibility = View.GONE
                            leftArrowTv.visibility = View.GONE
                        }

                        if (it.showRightArrow) {
                            rightArrowIv.visibility = View.VISIBLE
                            rightArrowTv.visibility = View.VISIBLE
                            rightArrowIv.isEnabled = false
                            rightArrowIv.setImageResource(R.drawable.img_10)
                            rightArrowIv.setBackgroundColor(Color.TRANSPARENT)

                        } else {
                            rightArrowIv.visibility = View.GONE
                            rightArrowTv.visibility = View.GONE
                        }

                        if (it.showDownArrow) {
                            downArrowIv.visibility = View.VISIBLE
                            downArrowTv.visibility = View.VISIBLE
                            downArrowIv.isEnabled = false
                            downArrowIv.setImageResource(R.drawable.img_11)
                            downArrowIv.setBackgroundColor(Color.TRANSPARENT)

                        } else {
                            downArrowIv.visibility = View.GONE
                            downArrowTv.visibility = View.GONE
                        }
                    }

                    Log.d("imageSW it.nextStep", " ${it.nextStep}")
                    when (it.nextStep) {
                        "left" -> {
                            isArrowSelected = true
                            setArrowColors(
                                leftArrowColor = Color.GREEN,
                                rightArrowColor = Color.TRANSPARENT,
                                downArrowColor = Color.TRANSPARENT
                            )
                        }

                        "right" -> {
                            isArrowSelected = true
                            setArrowColors(
                                leftArrowColor = Color.TRANSPARENT,
                                rightArrowColor = Color.GREEN,
                                downArrowColor = Color.TRANSPARENT
                            )
                        }

                        "down" -> {
                            isArrowSelected = true
                            setArrowColors(
                                leftArrowColor = Color.TRANSPARENT,
                                rightArrowColor = Color.TRANSPARENT,
                                downArrowColor = Color.GREEN
                            )
                        }

                        else -> {
                            setArrowColors(
                                leftArrowColor = Color.TRANSPARENT,
                                rightArrowColor = Color.TRANSPARENT,
                                downArrowColor = Color.TRANSPARENT
                            )
                            rightArrowIv.isEnabled = true
                            leftArrowIv.isEnabled = true
                            downArrowIv.isEnabled = true
                        }
                    }

                    Log.d("imageSW leftOverlayImage", ": ${it.leftOverlayImage}")
                    Log.d("imageSW rightOverlayImage", ": ${it.rightOverlayImage}")
                    Log.d("imageSW topOverlayImage", ": ${it.topOverlayImage}")

                    //Overlapping images
                    if (it.leftOverlayImage != null) {
                        overlapImgLeft.visibility = View.VISIBLE
                        setImgWidth(overlapImgLeft, overlayBE.toInt())

                        val croppedBitmap =
                            cropBitmapByWidthFromRight(it.leftOverlayImage, overlayBE.toInt())
                        overlapImgLeft.setImageBitmap(croppedBitmap)
                        Log.d(
                            "imageSW croppedBitmap1",
                            " " + it.leftOverlayImage.width + " , " + it.leftOverlayImage.height
                        )
                        Log.d(
                            "imageSW cropRightOVerlap",
                            "cropBitmapByWidthFromRight: with ${overlayBE.toInt()}% ==>> " + croppedBitmap.width + " , " + croppedBitmap.height
                        )

                    } else overlapImgLeft.visibility = View.GONE

                    if (it.rightOverlayImage != null) {
                        overlapImgRight.visibility = View.VISIBLE
                        setImgWidth(overlapImgRight, overlayBE.toInt())

                        overlapImgRight.setImageBitmap(
                            cropBitmapByWidthFromLeft(it.rightOverlayImage, overlayBE.toInt())
                        )
                    } else overlapImgRight.visibility = View.GONE

                    if (it.topOverlayImage != null) {
                        overlapImgTop.visibility = View.VISIBLE
                        setImgHeight(overlapImgTop, overlayBE.toInt())

                        overlapImgTop.setImageBitmap(
                            cropBitmapFromBottom(
                                it.topOverlayImage,
                                overlayBE.toInt()
                            )
                        )
                    } else overlapImgTop.visibility = View.GONE

                    Log.d("imageSW it.isImageListEmpty", " : ${it.isImageListEmpty}")

                    deleteImg.isVisible = !it.isImageListEmpty
                    previewPageImgCS.isVisible = !it.isImageListEmpty
                    submitBtn1.isVisible = !it.isImageListEmpty

                    viewModel.imageCapturedListLive.observe(
                        this@CameraActivity,
                        androidx.lifecycle.Observer { imageModel ->

                            wideAngleButton.isVisible =
                                (viewModel.currentImageList.size == 0 && !isWideAngleCameraSameAsDefault())
                            if (viewModel.isRetake) captureImg.isEnabled =
                                (viewModel.currentImageList.size == 0)
                            viewModel.zoomEnabled = (imageModel.size == 0)

                            if (viewModel.currentImageList.size > 0) {
                                previewPageImgCS.setImageBitmap(imageModel.last().image)
                            }

                        }
                    )

                    // PreviewScreen work
                    if (viewModel.currentImageList.size > 0) {
                        previewImgRecycler.adapter =
                            PreviewListAdapter(this@CameraActivity, viewModel.currentImageList,
                                onClick = { clickedImg: Bitmap, croppingPoints1: Array<Int>, file1: File, position1: Int ->
                                    setImageInPreview(clickedImg, croppingPoints1, file1, position1)
                                })

                        viewModel.currentImageList.last().let {
                            setImageInPreview(
                                it.image,
                                it.croppedCoordinates,
                                it.file,
                                viewModel.currentImageList.size - 1
                            )
                        }

                    }
                }
            }


        }


        //calculate viewFinder Dimensions and set overlapping
        calculateViewDimensions(viewFinder) { width, height ->
            widthNewVF = width
            heightNewVF = height
            Log.d("imageSW viewFinder WH", " Width: $widthNewVF , height: $heightNewVF")

            //Overlapping images
            setImgWidth(overlapImgLeft, overlayBE.toInt())
            setImgWidth(overlapImgRight, overlayBE.toInt())
            setImgHeight(overlapImgTop, overlayBE.toInt())
        }


        // Image referenceUrl WORK
        referenceImg.isVisible = referenceUrl.isNotEmpty()

        referenceImg.setOnClickListener {
            val imageDialog = ImageDialog(this, referenceUrl)
            imageDialog.show()
        }

        wideAngleButton.setOnClickListener {
            val wideAngleCameraId =
                findWideAngleCamera((getSystemService(Context.CAMERA_SERVICE) as CameraManager))

            viewModel.wideAngleSet = if (viewModel.wideAngleSet) {
                startCamera()
                false
            } else {
                startCamera(wideAngleCameraId)
                true
            }
        }


        crossImg.setOnClickListener {
            onBackPressed()
        }

        // capture photo button click
        captureImg.setOnClickListener {
            if (viewModel.currentImageList.size == 0) {
                isArrowSelected = true
            }

            if (isArrowSelected) {
                captureImg.setBackgroundResource(R.drawable.black_solid_circle)
                Log.d("imageSW takePhoto", " START")
                takePhoto(isBlurFeature, isCropFeature)
            } else {
                openDirectionDialog()
            }
        }

        leftArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
                viewModel.leftArrowClicked(it1)
            }
        }

        rightArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            Log.d("imageSW rightArrow", " clicked")
            this.let { it1 ->
                isArrowSelected = true
                viewModel.rightArrowClicked(it1)
            }
        }

        downArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
                viewModel.bottomArrowClicked(it1)
            }
        }

        deleteImg.setOnClickListener {
            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }
            if (viewModel.currentImageList.size > 0) {
                if (viewModel.currentImageList.size == 1) resetZoom()
                viewModel.deleteLastCapturedImage()
            } else {
                isArrowSelected = true // size = 0
            }

            viewModel.renderUi() // deleteImg
        }

        submitBtn1.setOnClickListener {

            SubmitDialog( // submitBtn1
                getString(R.string.dialog_submit),
                getString(R.string.yes_btn),
                getString(R.string.no_btn),
                onClick = {
                    viewModel.submitClicked = true
                    Log.d(
                        "imageSW",
                        "Saved Image Count : ${viewModel.imageSavedCount} Submit : ${viewModel.submitClicked}"
                    )
                    if (viewModel.currentImageList.isNotEmpty() && viewModel.imageSavedCount == 0) {
                        viewModel.showLoader() // submitBtn1
                        uploadSaveImages(this@CameraActivity) // submitBtn1
                    }

                }
            ).show(supportFragmentManager, "DialogFragment")

        }

        uploadBtnPS.setOnClickListener {

            SubmitDialog( // uploadBtnPS
                getString(R.string.dialog_submit),
                getString(R.string.yes_btn),
                getString(R.string.no_btn),
                onClick = {
                    viewModel.submitClicked = true
                    Log.d(
                        "imageSW",
                        "Saved Image Count : ${viewModel.imageSavedCount} Submit : ${viewModel.submitClicked}"
                    )
                    if (viewModel.currentImageList.isNotEmpty() && viewModel.imageSavedCount == 0) {
                        viewModel.showLoader() // submitBtn1
                        uploadSaveImages(this@CameraActivity) // submitBtn1
                    }

                }
            ).show(supportFragmentManager, "DialogFragment")
        }

        previewPageImgCS.setOnClickListener {
            cropStartPS.visibility = View.GONE
            cameraLayout.visibility = View.GONE
            previewImgLayout.visibility = View.VISIBLE
        }

        exitBtnPS.setOnClickListener {
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            startCameraW() // exitBtnPS
        }

        captureMorePS.setOnClickListener {
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            startCameraW() // captureMorePS
        }


        //Cropping Work
        resetCropBtnCL.setOnClickListener {
            resetCroppingImg(cropImageViewCL, mBitmap!!, Uri.fromFile(mFile), "CS")
        }

        // Retake from Crop screen
        retakeCropBtnCL.setOnClickListener {
            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }

            mBitmap?.recycle()
            mBitmap = null



            mFile = null
        }
        // Retake from Blur screen
        retakeBlurImg.setOnClickListener {

            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }

            mBitmap?.recycle()
            mBitmap = null


            mFile = null
        }


        //Done Button
        cropDoneBtnCL.setOnClickListener {

            // this is for CROP DONE Button
            cropImageViewCL.getCroppedImageAsync()
        }

        // Continue Button
        notBlurContinueLL.setOnClickListener {
            // this is for continue_ with BLUR
            mBitmap?.let { it1 ->
                mFile?.let { it2 ->
                    cropLowLightCheck(it1, it2, isCropFeature) // Continue
                }
            }
        }

        //Cropping Work: getCropped Image Result
        cropImageViewCL.setOnCropImageCompleteListener { _, result ->
            // Get the cropped image and display it in the ImageView

            // Get the coordinates of the four corners
            val cropRect = result.cropRect
            val left = cropRect.left
            val top = cropRect.top
            val right = cropRect.right
            val bottom = cropRect.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            coordinatesCrop = arrayOf(left, top, right, bottom)
            Log.d(
                "imageSW Cropping",
                "DONE coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}"
            )


//            checkLowLightSave after cropping
            if (mFile != null && mBitmap != null && mBitmap.toString().isNotEmpty()) {
                mFile?.let {
                    checkLowLightSave(mBitmap!!) // after cropping
                }

            } else {
                Bugfender.e("android_cropping_bitmap", "mFile or mBitmap is empty/ null")
                Log.d("imageSW", " checkLowLightSave: mFile or mBitmap is empty/ null")
            }
        }


        // Preview Screen work
        //Cropping Work => set selected cropped area
        cropStartPS.setOnClickListener {
            //Show Hide Layouts
            cameraLayout.visibility = View.GONE
            previewImgLayout.visibility = View.VISIBLE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            imageShowLayoutPS.visibility = View.GONE
            cropLayoutPS.visibility = View.VISIBLE

            cropRectValuesPS = croppingRect(croppingPointsPS, cropImageViewPS)

            cropImageViewPS.setImageBitmap(mBitmapPS)
            if (cropRectValuesPS != null) {
                cropImageViewPS.cropRect = cropRectValuesPS
            }
            Log.d(
                "imageSW resizedBitmapPS",
                " Size: WH " + mBitmap?.width + " , " + mBitmap?.height
            )

        }

        // Reset Button Click
        resetCropBtnPS.setOnClickListener {

            if (cropRectValuesPS != null) {
                cropImageViewPS.cropRect = cropRectValuesPS
            } else {
                mBitmapPS?.let { it1 ->
                    resetCroppingImg(
                        cropImageViewPS,
                        it1,
                        Uri.fromFile(mFilePS),
                        "PS"
                    )
                }
            }

        }



        cropDoneBtnPS.setOnClickListener {
            cropImageViewPS.getCroppedImageAsync()
        }

        //getCropped Image Result
        cropImageViewPS.setOnCropImageCompleteListener { _, result ->
            // Get the cropped image and display it in the ImageView

            // Get the coordinates of the four corners
            val cropRect = result.cropRect

            val left = cropRect.left
            val top = cropRect.top
            val right = cropRect.right
            val bottom = cropRect.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            val coordinatesCrop = arrayOf(left, top, right, bottom)
            Log.d(
                "imageSW Cropping22",
                "DONE coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}"
            )

            if (coordinatesCrop != null && mFile != null) {


                // this is to update and get new coordinates
                if (mPosPS != -1) {
                    viewModel.currentImageList[mPosPS].croppedCoordinates = coordinatesCrop
//                    = ImageDetailsModel(coordinatesCrop)
                    previewImgRecycler?.adapter?.notifyDataSetChanged()
                    Log.d("imageSW new CropPS", "updated ok")
                    Bugfender.i("android_new_crop_pvf", "updated ok")

                }

            }

            //Show Hide Layouts
            cameraLayout.visibility = View.GONE
            previewImgLayout.visibility = View.VISIBLE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            imageShowLayoutPS.visibility = View.VISIBLE
            cropLayoutPS.visibility = View.GONE
        }

        wideAngleButton.isVisible =
            (viewModel.currentImageList.size == 0 && !isWideAngleCameraSameAsDefault())

        cameraExecutor = Executors.newSingleThreadExecutor()
    } // END of onCreate

    @SuppressLint("ClickableViewAccessibility")
    private fun setZoomListener() {
        scaleGestureDetector = ScaleGestureDetector(this, scaleGestureListener)

        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

        setZoomRatio(viewModel.currentZoomRatio)
    }

    fun setZoomRatio(zoomRatio: Double) {
        val cameraInstance = camera ?: return

        val newZoomRatio = zoomRatio.coerceIn(1.0, 2.0)

        cameraInstance.cameraControl.setZoomRatio(newZoomRatio.toFloat())

        viewModel.currentZoomRatio = newZoomRatio
        zoomText.text = String.format("%.1fx", viewModel.currentZoomRatio)
    }

    private fun resetZoom() {
        if (viewModel.isRetake) return
        viewModel.zoomEnabled = true
        viewModel.currentZoomRatio = 1.0
        setZoomRatio(viewModel.currentZoomRatio)
    }

    private fun setArrowColors(leftArrowColor: Int, rightArrowColor: Int, downArrowColor: Int) {
        leftArrowIv.setBackgroundColor(leftArrowColor)
        rightArrowIv.setBackgroundColor(rightArrowColor)
        downArrowIv.setBackgroundColor(downArrowColor)
    }

    private fun setLayoutParams(ratio: String) {
        val layoutParam = viewFinder.layoutParams as ConstraintLayout.LayoutParams
        layoutParam.dimensionRatio = ratio
        viewFinder.layoutParams = layoutParam

        val layoutParam1 = previewImgPS.layoutParams as ConstraintLayout.LayoutParams
        layoutParam1.dimensionRatio = ratio
        previewImgPS.layoutParams = layoutParam1


        val layoutParam4 = imageBlur.layoutParams as ConstraintLayout.LayoutParams
        layoutParam4.dimensionRatio = ratio
        imageBlur.layoutParams = layoutParam4
    }

    private fun setImageInPreview(
        clickedImg: Bitmap,
        croppingPoints1: Array<Int>,
        file1: File,
        position1: Int
    ) {
        previewImgPS.setImageBitmap(clickedImg)

        croppingPointsPS = croppingPoints1
        mBitmapPS = clickedImg
        mFilePS = file1
        mPosPS = position1

        Log.d(
            "imageSW PrevFrag",
            " imageSize: WH " + clickedImg.width + " , " + clickedImg.height + "\nBitmap: $clickedImg"
        )

        cropStartPS.isVisible = croppingPointsPS.isNotEmpty()
    }

    private fun findWideAngleCamera(manager: CameraManager): String? {
        var wideAngleCameraId: String? = null
        var widestFieldOfView = 0f

        try {
            val cameraIds = manager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val sensorInfo =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val focalLengths =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val minimumFocusDistance =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK &&
                    sensorInfo != null &&
                    focalLengths != null &&
                    minimumFocusDistance != null
                ) {
                    Log.d("imageSW wide find", cameraId)
                    val fieldOfView = calculateFieldOfView(sensorInfo, focalLengths)
                    if (fieldOfView > widestFieldOfView) {
                        widestFieldOfView = fieldOfView
                        wideAngleCameraId = cameraId
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        return wideAngleCameraId
    }

    private fun calculateFieldOfView(sensorInfo: SizeF, focalLengths: FloatArray): Float {
        // Calculate the field of view using sensor size, focal length, and minimum focus distance
        val horizontalFieldOfView = 2 * atan(sensorInfo.width / (2 * focalLengths[0]))
        val verticalFieldOfView = 2 * atan(sensorInfo.height / (2 * focalLengths[0]))

        // Choose the wider field of view (horizontal or vertical)
        return max(horizontalFieldOfView, verticalFieldOfView)
    }

    private fun startCameraW() {
        wideAngleButton.isVisible =
            (viewModel.currentImageList.size == 0 && !isWideAngleCameraSameAsDefault())
        val wideAngleCameraId =
            findWideAngleCamera((getSystemService(Context.CAMERA_SERVICE) as CameraManager))
        if (viewModel.wideAngleSet) startCamera(wideAngleCameraId) else startCamera()
    }


    private fun uploadSaveImages(context: Context) {
        lifecycleScope.launch {
            viewModel.uploadImages(context) // uploadSaveImages

        }
        if (viewModel.currentImageList.size == viewModel.imageUploadList.size) {
            viewModel.hideLoader()
            viewModel.discardAllImages()
            finish()
        }
    }

    private fun cropLowLightCheck(mBitmap1: Bitmap, mFile1: File, isCropFeature1: String) {
        if (isCropFeature1.isNotEmpty() && isCropFeature1.equals("true", true)) {
            croppingStart(mBitmap1)
        } else {
            coordinatesCrop = emptyArray()
            // Low Light and save work
            val enhancedRotedBitmap = checkLowLightSave(mBitmap1) //No Cropping
        }
    }

    private fun croppingStart(resizedEnhancedRotatedBitmap: Bitmap) {
        // Cropping Work
        Log.d(
            "imageSW cropping: ", "start with bitmap => $resizedEnhancedRotatedBitmap" +
                    "\nsize WH: ${resizedEnhancedRotatedBitmap.width} , ${resizedEnhancedRotatedBitmap.height}"
        )

        //Show Hide Layouts
        cameraLayout.visibility = View.GONE
        previewImgLayout.visibility = View.GONE
        cropLayout.visibility = View.VISIBLE
        blurLayout.visibility = View.GONE


        if (resizedEnhancedRotatedBitmap != null && resizedEnhancedRotatedBitmap.toString()
                .isNotEmpty()
        ) {
            cropImageViewCL.setImageBitmap(resizedEnhancedRotatedBitmap) // cropping
        } else {
            Log.d(
                "imageSW cropping: ",
                "NOT start with bitmap NULL => $resizedEnhancedRotatedBitmap"
            )
            Bugfender.e(
                "android_cropping_start ",
                "not start with bitmap is null or empty => $resizedEnhancedRotatedBitmap"
            )
        }

        viewModel.hideLoader()
    }

    private fun checkLowLightSave(bitmap2: Bitmap): Bitmap {
        val targetBmp: Bitmap = bitmap2.copy(Bitmap.Config.ARGB_8888, false)

        val isLowLight = ImageProcessingUtils.isLowLightImage(targetBmp)
        Log.d("imageSW isLowLight 1: ", "" + isLowLight)

        var enhancedRotatedBitmap: Bitmap? = null

        if (isLowLight) {
            Log.d("imageSW isLowLight 2: ", " $isLowLight  Enhancement DONE")

            enhancedRotatedBitmap = bitmap2

        } else {

            enhancedRotatedBitmap = bitmap2
            Log.d("imageSW ", " No Enhancement")

        }


        // File Saving work
        if (mFile != null && enhancedRotatedBitmap.toString()
                .isNotEmpty() && captureTime.isNotEmpty()
        ) {

            viewModel.handleClickedImage(
                enhancedRotatedBitmap,
                coordinatesCrop,
                mFile!!,
                captureTime,
                this@CameraActivity
            )

            Bugfender.d("android_image_update_react", "done")

            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE
        } else {
            Bugfender.e("android_image_save", "mFile or mBitmap is empty/ null")
            Log.d("imageSW save", "fileFinal or bitmapFinal is empty/ null")
        }

        return enhancedRotatedBitmap
    }

    private fun croppingRect(croppingPoints: Array<Int>, cropImageView: CropImageView): Rect? {
        Log.d(
            "imageSW CroppingPS 1",
            " coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${croppingPoints.contentToString()}"
        )

        // Convert the guideline coordinates to a Rect object
        val left = croppingPoints[0]
        val top = croppingPoints[1]
        val right = croppingPoints[2]
        val bottom = croppingPoints[3]
        val guidelinesRect = Rect(left, top, right, bottom)
        Log.d(
            "imageSW CroppingPS 2",
            " All coordinates left: $left , top: $top , right: $right , bottom: $bottom"
        )

        return guidelinesRect

    }

    override fun onBackPressed() {
        if (backpressedlistener != null) {
            if (viewModel.currentImageList.size > 0) {
                SubmitDialog( // onBackPressed
                    getString(R.string.discard_submit),
                    getString(R.string.yes_btn),
                    getString(R.string.no_btn),
                    onClick = {
                        viewModel.discardAllImages() // back button discard
                        resetZoom()
                    }
                ).show(supportFragmentManager, "DialogFragment")
            } else {
                finish()
            }

        }
    }

    private fun openDirectionDialog() {
        SubmitDialog( // select a direction
            getString(R.string.dialogTitle),
            getString(R.string.ok_btn),
            "",
            onClick = { }
        ).show(supportFragmentManager, "DialogFragment")
    }

    private fun takePhoto(isBlurFeature: String, isCropFeature: String) {
        viewModel.showLoader()
        val imageCapture = imageCapture ?: return
        val nameTimeStamp = viewModel.uuid.toString() + "_" + (viewModel.currentImageList.size + 1)
        val outputDirectory = this.filesDir
        if (outputDirectory != null) Log.d(
            "imageSW outputDirectory",
            outputDirectory.absolutePath.toString()
        )
        val photoFile = File(outputDirectory, "$nameTimeStamp.jpg")

        // Create time-stamped output file to hold the image
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("imageSW", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedImageUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d("imageSW takePhoto", " END , img: $savedImageUri at time $nameTimeStamp")


                    // set the saved uri to the image view

                    Glide.with(this@CameraActivity)
                        .asBitmap()
                        .load(savedImageUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(resizedWidthNew!!, resizedHeightNew!!)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {

                                val bitmap = resource

                                viewModel.imageSavedCount++

                                Log.d("imageSW", "Saved Image Count : ${viewModel.imageSavedCount}")

                                saveImageToFile(photoFile, bitmap, this@CameraActivity)

                                mFile = photoFile
                                mBitmap = bitmap
                                captureTime = nameTimeStamp
                                viewModel.imageName = nameTimeStamp

                                val msg = "Photo capture succeeded: $savedImageUri"
                                Log.d(TAG, msg)
                                viewModel.hideLoader()


                                // Condition Check Work Flow:
                                // Blur ==> lowLight ==> rotation ==> cropping ==> Saving Final Image
                                if (isBlurFeature != null && isBlurFeature.isNotEmpty() && isBlurFeature == "true") {

                                    val bitmap1 = mBitmap
                                    val targetBmp: Bitmap =
                                        bitmap1!!.copy(Bitmap.Config.ARGB_8888, false)


                                    val isImgBlur = BlurDetection.runDetection(
                                        this@CameraActivity,
                                        targetBmp
                                    ) // Blur check
                                    Log.d(
                                        "imageSW Blur: ",
                                        "isBlurFeature: $isBlurFeature  ,isImgBlur: $isImgBlur"
                                    )
                                    Bugfender.d(
                                        "android_image_blur",
                                        "isBlurFeature: $isBlurFeature  ,isImgBlur: $isImgBlur"
                                    )
                                    if (isImgBlur.first) {
                                        // Image is blurred
                                        imageBlur.setImageBitmap(mBitmap)

                                        //Show Hide Layouts
                                        cameraLayout.visibility = View.GONE
                                        previewImgLayout.visibility = View.GONE
                                        cropLayout.visibility = View.GONE
                                        blurLayout.visibility = View.VISIBLE


                                    } else {
                                        // Image is not blurred
                                        cropLowLightCheck(
                                            mBitmap!!,
                                            mFile!!,
                                            isCropFeature
                                        ) //not blurred

                                    }
                                } else {
                                    // check Low Light Image
                                    cropLowLightCheck(
                                        mBitmap!!,
                                        mFile!!,
                                        isCropFeature
                                    ) // No blur features
                                }
                            }

                        })
                }
            })
    }

    fun saveImageToFile(file1: File, bitmap1: Bitmap, context: Context? = null) {
        Log.d("imageSW ", "saveImageToFile START")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.IO) {
                    val appContext = context?.applicationContext

                    FileOutputStream(file1).use { outputStream ->
                        bitmap1.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        outputStream.flush()
                        outputStream.close()
                    }

                    viewModel.imageSavedCount--

                    Log.d(
                        "imageSW",
                        "Saved Image Count : ${viewModel.imageSavedCount} Submit : ${viewModel.submitClicked}"
                    )

                    if (viewModel.submitClicked && viewModel.imageSavedCount == 0)
                        appContext?.let { uploadSaveImages(it) }


                    Log.d("imageSW ", "saveBitmapToDisk DONE: $bitmap1")

                }
                // Image file saved successfully
            } catch (e: IOException) {
                e.printStackTrace()
                Bugfender.e(
                    "android_image_save_file",
                    "IOException: " + e.printStackTrace().toString()
                )

                // Error occurred while saving the image file
            }
        }
        Log.d("imageSW ", "saveImageToFile DONE")

    }

    private fun isWideAngleCameraSameAsDefault(): Boolean {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val wideAngleCameraId = findWideAngleCamera(manager)
        val defaultCameraId = manager.cameraIdList.find { cameraId ->
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }
        return wideAngleCameraId == defaultCameraId
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun startCamera(wideAngleCameraId: String? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            // ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    })
                }

            // Select back camera as a default
            val cameraSelector = wideAngleCameraId?.let { wC ->
                CameraSelector.Builder()
                    .addCameraFilter { fil -> fil.filter { Camera2CameraInfo.from(it).cameraId == wC } }
                    .build()
            }
                ?: CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )

                setZoomListener()


            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraW()
            } else {
                // If permissions are not granted,
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
//                finish()
            }
        }
    }


    private fun setImgWidth(imageView: ImageView, overlapBE: Int) {
        val currentWidth = widthNewVF
        val newWidth =
            (currentWidth?.times(overlapBE))?.div(100) // Calculate 20% of the current width

        val layoutParams = imageView.layoutParams
        if (newWidth != null) {
            layoutParams.width = newWidth
        }
        imageView.layoutParams = layoutParams
        Log.d("imageSW", "OW:$currentWidth, newWidth set at $overlapBE% newWidth: $newWidth")
    }

    private fun setImgHeight(imageView: ImageView, overlapBE: Int) {
        val currentHeight = heightNewVF
        val newHeight =
            (currentHeight?.times(overlapBE))?.div(100) // Calculate 20% of the current width

        val layoutParams = imageView.layoutParams
        if (newHeight != null) {
            layoutParams.height = newHeight

        }
        imageView.layoutParams = layoutParams
        Log.d("imageSW", "OH:$currentHeight, newHeight set at $overlapBE% newHeight: $newHeight")
    }

    // Function to calculate the height and width of a view
    fun calculateViewDimensions(
        view: View,
        onDimensionsCalculated: (width: Int, height: Int) -> Unit
    ) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Calculate view's dimensions
                val width = view.width
                val height = view.height

                // Invoke the callback with the calculated dimensions
                onDimensionsCalculated(width, height)

                // Remove the listener to avoid multiple invocations
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun cropBitmapFromBottom(bitmap: Bitmap, percent: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val cropHeight = (height * percent / 100f).toInt()
        val cropY = height - cropHeight
        return Bitmap.createBitmap(bitmap, 0, cropY, width, cropHeight)
    }

    private fun cropBitmapByWidthFromRight(bitmap: Bitmap, percent: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = (width * percent / 100f).toInt()
        val cropX = width - cropWidth
        return Bitmap.createBitmap(bitmap, cropX, 0, cropWidth, height)
    }

    private fun cropBitmapByWidthFromLeft(bitmap: Bitmap, percent: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = (width * percent / 100f).toInt()
        return Bitmap.createBitmap(bitmap, 0, 0, cropWidth, height)
    }


    private fun resetCroppingImg(
        cropImageView: CropImageView,
        mBitmap1: Bitmap,
        imgUri1: Uri,
        screen: String
    ) {
        Log.d("imageSW reset", " imgUri==>>  $imgUri1")
        cropImageView.resetCropRect()
        if (imgUri1 != null && imgUri1.toString().isNotEmpty()) {
            cropImageView.setImageBitmap(mBitmap1)

        } else
            Common.showToast(this, "Please capture image before reset")
    }


    private fun initializeIDs() {
        // for Camera Layout
        viewFinder = findViewById(R.id.viewFinder)
        captureImg = findViewById(R.id.image_capture_button)
        deleteImg = findViewById(R.id.delete_btn)
        previewPageImgCS = findViewById(R.id.preview_btn)
        wideAngleButton = findViewById(R.id.wideAngleButton)
        crossImg = findViewById(R.id.cross_iv)
        referenceImg = findViewById(R.id.imgReference_iv)
        overlayGroup = findViewById(R.id.overlayGroup)
        submitBtn1 = findViewById(R.id.submitImgLL)
        loader = findViewById(R.id.loader)
        orientationBl = findViewById(R.id.orientation_bl)
        orientationTv = findViewById(R.id.orientation_tv)
        leftArrowIv = findViewById(R.id.left_arrow_iv)
        leftArrowTv = findViewById(R.id.left_arrow_tv)
        rightArrowIv = findViewById(R.id.right_arrow_iv)
        rightArrowTv = findViewById(R.id.right_arrow_tv)
        downArrowIv = findViewById(R.id.down_arrow_iv)
        downArrowTv = findViewById(R.id.down_arrow_tv)
        overlapImgTop = findViewById(R.id.captured_img_top)
        overlapImgLeft = findViewById(R.id.captured_img_left)
        overlapImgRight = findViewById(R.id.captured_img_right)

        cameraLayout = findViewById(R.id.cameraLayout)
        blurLayout = findViewById(R.id.blurLayout)
        cropLayout = findViewById(R.id.croppingLayout)


        /*Cropping Camera Layout*/
        cropImageViewCL = findViewById(R.id.cropImageView)
        retakeCropBtnCL = findViewById(R.id.croppingRetakeBtn)
        resetCropBtnCL = findViewById(R.id.croppingResetBtn)
        cropDoneBtnCL = findViewById(R.id.croppingDoneBtn)

        imageBlur = findViewById(R.id.demoImg)
        retakeBlurImg = findViewById(R.id.croppingRetakeBtn2)
        notBlurContinueLL = findViewById(R.id.LL22) // notBlur Text


        // for Preview Layout
        previewImgLayout = findViewById(R.id.previewImgLayout)
        imageShowLayoutPS = findViewById(R.id.imageShowLayout_ps)
        cropLayoutPS = findViewById(R.id.croppingLayoutPreview)

        exitBtnPS = findViewById(R.id.exit_btn)
        cropStartPS = findViewById(R.id.cropIV)
        previewImgPS = findViewById(R.id.preview_iv)
        previewImgRecycler = findViewById(R.id.preview_img_rv)
        captureMorePS = findViewById(R.id.captureMore_iv)
        uploadBtnPS = findViewById(R.id.uploadImgLL_ps)

        /*Cropping Preview Screen Layout*/
        cropImageViewPS = findViewById(R.id.cropImageView_ps)
        resetCropBtnPS = findViewById(R.id.croppingResetBtn_ps)
        cropDoneBtnPS = findViewById(R.id.croppingDoneBtn_ps)

        zoomText = findViewById(R.id.zoomText)


    }

    private fun getOrientation(angle: Int): Int {
        return when {
            angle in 0..45 || angle in 315..360 -> Orientation_PORTRAIT
            angle in 45..135 -> ORIENTATION_REVERSE_LANDSCAPE
            angle in 135..225 -> ORIENTATION_REVERSE_PORTRAIT
            angle in 225..315 -> ORIENTATION_LANDSCAPE
            angle in -1 downTo -2 -> ORIENTATION_PARALLEL
            else -> Orientation_PORTRAIT
        }
    }


    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */
    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }


        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }

    private val myBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var index = intent!!.getIntExtra("index", 0)
            Log.e("imageSW myBroadcastReceiver", "All images Uploaded Successfully of size $index")

            viewModel.discardAllImages() // Delete images after Successful Update

            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

        }
    }


    companion object {
        private const val TAG = "CameraSDK_Android"
        const val FILENAME_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val REQUEST_CODE_PERMISSIONS = 20
        var backpressedlistener: Backpressedlistener? = null
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val Orientation_PORTRAIT = 0
        private const val ORIENTATION_REVERSE_PORTRAIT = 1
        private const val ORIENTATION_LANDSCAPE = 2
        private const val ORIENTATION_REVERSE_LANDSCAPE = 3
        private const val ORIENTATION_PARALLEL = -1

        const val BLURRED_IMAGE = "BLURRED IMAGE"
        const val NOT_BLURRED_IMAGE = "NOT BLURRED IMAGE"
    }

    override fun onResume() {
        super.onResume()
        backpressedlistener = this
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(myBroadcastReceiver, IntentFilter("thisIsForMyPartner"))// onResume

    }

    override fun onPause() {
        backpressedlistener = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopService(Intent(this, MyServices()::class.java)) // onDestroy
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(myBroadcastReceiver) // Unbind broadcastR in onDestroy
    }

}