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
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.widget.ImageButton
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
import com.canhub.cropper.CropImageView
import com.google.firebase.FirebaseApp
import com.sj.camera_lib_android.databinding.ActivityCameraBinding
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.ui.FlashType
import com.sj.camera_lib_android.ui.ImageDialog
import com.sj.camera_lib_android.ui.SubmitDialog
import com.sj.camera_lib_android.ui.adapters.PreviewListAdapter
import com.sj.camera_lib_android.ui.interfaces.Backpressedlistener
import com.sj.camera_lib_android.ui.viewmodels.CameraViewModel
import com.sj.camera_lib_android.utils.Common
import com.sj.camera_lib_android.utils.Events
import com.sj.camera_lib_android.utils.LanguageUtils
import com.sj.camera_lib_android.utils.LogUtils
import com.sj.camera_lib_android.utils.Utils
import com.sj.camera_lib_android.utils.imageutils.BlurDetection
import com.sj.camera_lib_android.utils.imageutils.ImageProcessingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt


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

    private var newImageClick = true

    //    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView

    private lateinit var captureImg: ImageView
    private lateinit var deleteImg: ImageView
    private lateinit var previewPageImgCS: ImageView
    private lateinit var crossImg: ImageView
    private lateinit var referenceImg: ImageView
    private lateinit var flashButton: ImageButton
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

    /* View binding */
    private lateinit var binding: ActivityCameraBinding

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

    /* Tilt Detection */
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var timestamp: Long = 0
    private var pitch = 0.0f
    private var roll = 0.0f
    private var gyroscopeX = 0.0f
    private var gyroscopeY = 0.0f
    private val alpha = 0.98f // Complementary filter constant
    private var filteredTiltX = 0f
    private var filteredTiltY = 0f
    private val alphaTilt = 0.05f

    private lateinit var wideAngleButton: ImageButton
    private var wideAngleClicked = false

    //Flash
    private var currentFlashType: FlashType = FlashType.AUTO

    private var maxGridSize = -1

    private var language: String = "en"

    private var isLambda = false

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
        intent.extras?.getString("language")?.let { LanguageUtils.checkAndSetLanguage(it, this.resources) }
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        OpenCVLoader.initDebug()

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
        var gridlines = false

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
            viewModel.backendToggle = extras.getBoolean("backendToggle", false)
            gridlines = extras.getBoolean("gridlines", false)
            language = extras.getString("language", "en")
            isLambda = extras.getBoolean("isLambda", false)


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


        logCameraLaunchEvent(uploadParams)
        LogUtils.logGlobally(
            Events.NATIVE_PARAMS,
            "orientation: $modeRotation, widthPercentage: $overlayBE, resolution: $resolution, referenceUrl: $referenceUrl, allowBlurCheck: $isBlurFeature, allowCrop: $isCropFeature, isRetake: ${viewModel.isRetake}, zoomLevel: ${viewModel.currentZoomRatio}, showOverlapToggleButton: ${viewModel.backendToggle}, showGrideLines: $gridlines, langauge: $language"
        )

        viewModel.discardAllImages() // cameraActivity Launch


        modeRotation = viewModel.mode

        // Rotation work
        if (modeRotation.isNotEmpty() && modeRotation == "landscape") {
            if(!isLambda) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
            viewModel.imageWidth = resolution.toInt()
            viewModel.imageHeight = (resolution.toInt() * 3) / 4
            Log.d("imageSW resizeNEW: ", "Landscape WH: $resizedWidthNew, $resizedHeightNew")

        } else {
            resizedWidthNew = (resolution.toInt() * 3) / 4
            resizedHeightNew = resolution.toInt()
            viewModel.imageWidth = (resolution.toInt() * 3) / 4
            viewModel.imageHeight = resolution.toInt()
            Log.d("imageSW resizeNEW: ", "Portrait WH: $resizedWidthNew, $resizedHeightNew")

            if(!isLambda) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                                    orientationTv.text = getString(R.string.change_orientation_landscape)
                                    orientationBl.visibility = View.VISIBLE
                                }
                            }

                            ORIENTATION_REVERSE_PORTRAIT -> {
                                orientationTv.text = getString(R.string.change_orientation, if(modeRotation.equals(
                                        "portrait",
                                        true
                                    )) getString(R.string.portrait_small) else getString(R.string.landscape_small))
                                orientationBl.visibility = View.VISIBLE
                            }

                            ORIENTATION_LANDSCAPE -> {
                                if (modeRotation.equals("landscape", true)) {
                                    orientationBl.visibility = View.INVISIBLE

                                } else {
                                    orientationTv.text = getString(R.string.change_orientation_portrait)
                                    orientationBl.visibility = View.VISIBLE
                                }
                            }

                            ORIENTATION_REVERSE_LANDSCAPE -> {
                                orientationTv.text = getString(R.string.change_orientation, if(modeRotation.equals(
                                        "portrait",
                                        true
                                    )) getString(R.string.portrait_small) else getString(R.string.landscape_small))
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
            if(!isLambda) orientationEventListener.enable()

        } else finish()


        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCameraW()

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        lifecycleScope.launch { viewModel.previewFlow.collect { updatePreview() } }

        lifecycleScope.launch {
            viewModel.uiState.collect { it ->
                withContext(Dispatchers.Main) {
                    Log.d("imageSW preview entered ui state", viewModel.currentImageList.size.toString())
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

                        leftArrowIv.isClickable = true
                        rightArrowIv.isClickable = true
                        downArrowIv.isClickable = true
                    } else {

                        isArrowSelected = true
                        if (it.showLeftArrow) {
                            leftArrowIv.visibility = View.VISIBLE
                            leftArrowTv.visibility = View.VISIBLE
                            leftArrowIv.isClickable = false
                            leftArrowIv.setImageResource(R.drawable.img_9)
                            leftArrowIv.setBackgroundColor(Color.TRANSPARENT)

                        } else {
                            leftArrowIv.visibility = View.GONE
                            leftArrowTv.visibility = View.GONE
                        }

                        if (it.showRightArrow) {
                            rightArrowIv.visibility = View.VISIBLE
                            rightArrowTv.visibility = View.VISIBLE
                            rightArrowIv.isClickable = false
                            rightArrowIv.setImageResource(R.drawable.img_10)
                            rightArrowIv.setBackgroundColor(Color.TRANSPARENT)

                        } else {
                            rightArrowIv.visibility = View.GONE
                            rightArrowTv.visibility = View.GONE
                        }

                        if (it.showDownArrow) {
                            downArrowIv.visibility = View.VISIBLE
                            downArrowTv.visibility = View.VISIBLE
                            downArrowIv.isClickable = false
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

                            binding.overlapToggle?.isEnabled = (viewModel.currentImageList.size == 0)

                            if (viewModel.currentImageList.size > 0) {
                                previewPageImgCS.setImageBitmap(imageModel.last().image)
                            }

                            /* overlap */
                            if (viewModel.currentImageList.isNotEmpty()) {
                                Log.d("imageSW toggle", "${viewModel.backendToggle} ${binding.overlapToggle?.isChecked} ${viewModel.currentImageList.size}")
                                if(viewModel.backendToggle) {
                                    if (binding.overlapToggle?.isChecked == false) binding.overlapGroup?.isVisible =
                                        false
                                    if (viewModel.currentImageList.size == 1 && binding.overlapToggle?.isChecked == true)
                                        binding.overlayGroup.isVisible = true
                                }
                            }
                        }
                    )

                    Log.d("imageSW preview", viewModel.currentImageList.size.toString())
                    // PreviewScreen work
                    updatePreview()
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

        flashButton.setOnClickListener {
            toggleFlash()
        }

        wideAngleButton.setOnClickListener {
            val wideAngleCameraId =
                findWideAngleCamera((getSystemService(Context.CAMERA_SERVICE) as CameraManager))

            wideAngleButton.setColorFilter(if(wideAngleClicked) Color.argb(255, 181, 71, 71) else Color.TRANSPARENT)
            wideAngleClicked = !wideAngleClicked

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
            //Gyro work
            viewModel.gyroValueX = String.format(Locale.US, "%.2f", filteredTiltY).toFloat()
            viewModel.gyroValueY = String.format(Locale.US, "%.2f", filteredTiltX).toFloat()

            if (viewModel.currentImageList.size == 0) {
                isArrowSelected = true
            }

            if(checkMaxImageLimitReached()) { openMaxLimitDialog() }
            else if (isArrowSelected || (viewModel.backendToggle && binding.overlapToggle?.isChecked == false)) {
                captureImg.setBackgroundResource(R.drawable.black_solid_circle)
                logCapturePressEvent()
                takePhoto(isBlurFeature, isCropFeature)
            } else {
                openDirectionDialog()
            }
        }

        leftArrowIv.setOnClickListener {
            LogUtils.logGlobally(Events.LEFT_ARROW_CLICKED)
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
                viewModel.leftArrowClicked(it1)
            }
        }

        rightArrowIv.setOnClickListener {
            LogUtils.logGlobally(Events.RIGHT_ARROW_CLICKED)
            it.setBackgroundColor(Color.GREEN)
            Log.d("imageSW rightArrow", " clicked")
            this.let { it1 ->
                isArrowSelected = true
                viewModel.rightArrowClicked(it1)
            }
        }

        downArrowIv.setOnClickListener {
            LogUtils.logGlobally(Events.DOWN_ARROW_CLICKED)
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
                viewModel.bottomArrowClicked(it1)
            }
        }

        deleteImg.setOnClickListener {
            LogUtils.logGlobally(Events.DELETE_CLICKED)
            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }
            if (viewModel.currentImageList.size > 0) {
                if (viewModel.currentImageList.size == 1) resetZoom()
                viewModel.deleteLastCapturedImage()
            } else {
                isArrowSelected = true // size = 0
            }

            Log.d("imageSW preview render", "${viewModel.currentImageList.size}")
            viewModel.renderUi() // deleteImg
        }

        submitBtn1.setOnClickListener {
            SubmitDialog( // submitBtn1
                prompt = getString(R.string.dialog_submit),
                yesText = getString(R.string.yes_btn),
                noText = getString(R.string.no_btn),
                onClick = {
                    viewModel.submitClicked = true
                    broadcastSubmitPress(this@CameraActivity)
                    LogUtils.logGlobally(Events.UPLOAD_BUTTON_PRESSED, "Total Images: ${viewModel.currentImageList.size}")
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
                prompt = getString(R.string.dialog_submit),
                yesText = getString(R.string.yes_btn),
                noText = getString(R.string.no_btn),
                onClick = {
                    viewModel.submitClicked = true
                    broadcastSubmitPress(this@CameraActivity)
                    LogUtils.logGlobally(Events.UPLOAD_BUTTON_PRESSED_PREVIEW, "Total Images: ${viewModel.currentImageList.size}")
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
            LogUtils.logGlobally(Events.CROP_RETAKE)
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
            LogUtils.logGlobally(Events.BLUR_RETAKE)
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
        cropDoneBtnCL.cropClickWithDebounce {
            LogUtils.logGlobally(Events.CROP_DONE)
            // this is for CROP DONE Button
            cropImageViewCL.croppedImageAsync()
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

            deleteCroppedImage(result.uriContent)
            // Get the coordinates of the four corners
            val cropRect = result.cropRect
            val left = cropRect?.left
            val top = cropRect?.top
            val right = cropRect?.right
            val bottom = cropRect?.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            coordinatesCrop = arrayOf(left!!, top!!, right!!, bottom!!)
            LogUtils.logGlobally(
                Events.IMAGE_CROPPED,
                "coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}"
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
            LogUtils.logGlobally(Events.CROP_DONE_PREVIEW)
            cropImageViewPS.croppedImageAsync()
        }

        //getCropped Image Result
        cropImageViewPS.setOnCropImageCompleteListener { _, result ->
            // Get the cropped image and display it in the ImageView

            deleteCroppedImage(result.uriContent)
            // Get the coordinates of the four corners
            val cropRect = result.cropRect

            val left = cropRect?.left
            val top = cropRect?.top
            val right = cropRect?.right
            val bottom = cropRect?.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            val coordinatesCrop = arrayOf(left!!, top!!, right!!, bottom!!)
            LogUtils.logGlobally(
                Events.IMAGE_CROPPED_PREVIEW,
                "coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}"
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


        wideAngleButton.setColorFilter(Color.argb(255, 181, 71, 71))
        wideAngleButton.isVisible =
            (viewModel.currentImageList.size == 0 && !isWideAngleCameraSameAsDefault())
        viewModel.hasWideAngle = !isWideAngleCameraSameAsDefault()
        logCameras()

        cameraExecutor = Executors.newSingleThreadExecutor()

        initSensors()

        resetToggle()

        binding.verticalTiltView.setHorizontalMode(false)

        binding.gridOverlayView.isVisible = gridlines
    } // END of onCreate

    private fun resetToggle() {
        binding.overlapToggle?.apply {
            isVisible = viewModel.backendToggle && !viewModel.isRetake
        }
    }

    private fun logCameraLaunchEvent(uploadParam: String) {
        try {
            val uploadJson = JSONObject(uploadParam)
            val latitude = uploadJson.optString("latitude")
            val longitude = uploadJson.optString("longitude")
            val shopName = uploadJson.optString("shop_name")
            val shopId = uploadJson.optString("shop_id")
            val categoryName = uploadJson.optString("category_name")
            val categoryId = uploadJson.optString("category_id")

            val shop_data = JSONObject()
            shop_data.put("shop_id", shopId)
            shop_data.put("shop_name", shopName)
            shop_data.put("category_id", categoryId)
            shop_data.put("category_name", categoryName)

            val logJson = JSONObject()
            logJson.put("latitude", latitude)
            logJson.put("longitude", longitude)
            logJson.put("shop_data", shop_data)
            LogUtils.logGlobally(Events.CAMERA_LAUNCHED_EVENT, logJson.toString())
        } catch(e : Exception) {
            LogUtils.logGlobally("upload-param-parse-failure", uploadParam)
        }
    }

    private fun logCapturePressEvent() {
        try {
            var attributes = "hasWideAngle: ${wideAngleButton.isVisible}, flash: ${currentFlashType.name}, Gyro values(Horizontal, Vertical): (${viewModel.gyroValueX}, ${viewModel.gyroValueY})"
            if(wideAngleButton.isVisible) attributes += ", wideAngleSelected: ${viewModel.wideAngleSet}"
            if (viewModel.backendToggle) attributes += ", overlapToggleState: ${binding.overlapToggle.isChecked}"
            LogUtils.logGlobally(Events.CAPTURE_BUTTON_PRESSED, attributes)
        } catch(_: Exception) {

        }
    }

    private fun deleteCroppedImage(content: Uri?) {
        content?.let {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        contentResolver.delete(it, null, null).let { rowsDeleted ->
                            Log.d("imageSW", "cropped image $it $rowsDeleted")
                        }
                    } catch (e: Exception) {
                        Log.e("imageSW", "cropped image exception $e")
                    }
                }
            }
        }
    }
    private fun View.cropClickWithDebounce(action: () -> Unit) {
        this.setOnClickListener {
            if (newImageClick) action()
            newImageClick = false
        }
    }

    private fun updatePreview() {
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

    private fun broadcastSubmitPress(context: Context) {
        val x = viewModel.currentImageList.map { "${it.file}" }
        val intent = Intent("did-submit-press")
        intent.putExtra("upload_params", viewModel.upload_param)
        intent.putExtra("images", ArrayList(viewModel.currentImageList.map { "${it.file}" }))
        intent.putExtra("is_retake", viewModel.isRetake)
        intent.putExtra("session_id", viewModel.uuid.toString())
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun broadcastCameraClose(context: Context) {
        val intent = Intent("did-camera-close")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun initSensors() {
        // Initialize SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get accelerometer and gyroscope sensors
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        binding.tiltGroup.isVisible = !(accelerometerSensor == null && gyroscopeSensor == null)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val accelX = event.values[0]
                    val accelY = event.values[1]
                    val accelZ = event.values[2]

                    // Detect landscape orientation based on accelerometer data
                    val landscapeOrientation = (abs(accelX) > abs(accelY))

                    if (landscapeOrientation) {
                        // Landscape orientation: adjust pitch and roll calculation
                        pitch = atan2(-accelY, sqrt(accelX * accelX + accelZ * accelZ))
                        roll = atan2(accelX, accelZ)
                    } else {
                        // Portrait orientation
                        pitch = atan2(-accelX, sqrt(accelY * accelY + accelZ * accelZ))
                        roll = atan2(accelY, accelZ)
                    }
                }

                Sensor.TYPE_GYROSCOPE -> {
                    val gyroX = event.values[0]
                    val gyroY = event.values[1]

                    if (timestamp != 0L) {
                        val dT =
                            (event.timestamp - timestamp) * 1e-9f // Convert nanoseconds to seconds

                        gyroscopeX += gyroX * dT
                        gyroscopeY += gyroY * dT

                        // Apply complementary filter for improved stability
                        pitch = alpha * (pitch + gyroscopeX * dT) + (1 - alpha) * pitch
                        roll = alpha * (roll + gyroscopeY * dT) + (1 - alpha) * roll
                    }
                }
            }

            timestamp = event.timestamp

            // Update tilt values
            val tiltXDegrees = Math.toDegrees(roll.toDouble()).toFloat()
            val tiltYDegrees = Math.toDegrees(pitch.toDouble()).toFloat()

            // Apply the low-pass filter
            filteredTiltX += alphaTilt * (mapTilt(tiltXDegrees, true) - filteredTiltX)
            filteredTiltY += alphaTilt * (mapTilt(tiltYDegrees, false) - filteredTiltY)

            val tiltXVal = String.format(Locale.US, "%.2f", filteredTiltX).toFloat()
            val tiltYVal = String.format(Locale.US, "%.2f", filteredTiltY).toFloat()

            // Update the tilt views with the filtered values
            binding.horizontalTiltView.setValue(tiltYVal)
            binding.verticalTiltView.setValue(tiltXVal)
            binding.tiltWarningMessage.isVisible = ((abs(tiltXVal) > 6f) || (abs(tiltYVal) > 6f))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    private fun mapTilt(tiltDegree: Float, xAxis: Boolean): Float {
        val degreeDiff = tiltDegree - (if (xAxis) 90 else 0)
        return degreeDiff / 5f
    }

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
        zoomText.text = String.format(Locale.US, "%.1fx", viewModel.currentZoomRatio)
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

    private fun logCameras(){
        val cameraList: MutableList<String> = mutableListOf()
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
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
                    val fieldOfView = calculateFieldOfView(sensorInfo, focalLengths)
                    cameraList.add("cameraId: $cameraId fieldOfView: $fieldOfView")
                }
            }

            LogUtils.logGlobally(Events.NATIVE_AVAILABLE_WIDE_ANGLE, cameraList.toString())
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
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
            if(viewModel.backendToggle) {
                viewModel.overlapToggleChecked = binding.overlapToggle?.isChecked == true
            }
            viewModel.uploadImages(context) // uploadSaveImages
            if (viewModel.currentImageList.size == viewModel.imageUploadList.size) {
                viewModel.hideLoader()
                viewModel.discardAllImages()
                finish()
            }
        }
    }

    private fun cropLowLightCheck(mBitmap1: Bitmap, mFile1: File, isCropFeature1: String) {
        LogUtils.logGlobally(Events.CROP_METHOD)
        if (isCropFeature1.isNotEmpty() && isCropFeature1.equals("true", true)) {
            croppingStart(mBitmap1)
        } else {
            coordinatesCrop = emptyArray()
            // Low Light and save work
            val enhancedRotedBitmap = checkLowLightSave(mBitmap1) //No Cropping
        }
    }

    private fun croppingStart(resizedEnhancedRotatedBitmap: Bitmap) {
        LogUtils.logGlobally(Events.CROP_START, "Bitmap: $resizedEnhancedRotatedBitmap")
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
            if(!previewImgLayout.isVisible) {
                if (viewModel.currentImageList.size > 0) {
                    SubmitDialog( // onBackPressed
                        prompt = getString(R.string.discard_submit),
                        yesText = getString(R.string.yes_btn),
                        noText = getString(R.string.no_btn),
                        onClick = {
                            LogUtils.logGlobally(Events.CROSS_CLICK, "Discard Images")
                            viewModel.deleteAllImages()
                            viewModel.discardAllImages() // back button discard
                            resetZoom()
                        }
                    ).show(supportFragmentManager, "DialogFragment")
                } else {
                    LogUtils.logGlobally(Events.CROSS_CLICK, "Close Camera Screen")
                    broadcastCameraClose(this)
                    finish()
                }
            } else {
                cropLayoutPS.visibility = View.GONE
                imageShowLayoutPS.visibility = View.VISIBLE
                cameraLayout.visibility = View.VISIBLE
                previewImgLayout.visibility = View.GONE
                startCameraW()
            }
        }
    }

    private fun openDirectionDialog() {
        SubmitDialog( // select a direction
            prompt = getString(R.string.dialogTitle),
            yesText = getString(R.string.ok_btn),
            noText = "",
            onClick = { }
        ).show(supportFragmentManager, "DialogFragment")
    }

    private fun checkMaxImageLimitReached(): Boolean {
        try {
            Log.d("imageSW max limit", "current Image size: ${viewModel.currentImageList.size}")

            if (viewModel.currentImageList.size == MAX_IMAGE_LIMIT) return true

            if (viewModel.currentImageList.indexOfFirst { it.direction != "" && !it.isAutomatic } == -1) {
                if ((viewModel.directionSelected == "left" || viewModel.directionSelected == "right") && viewModel.currentImageList.size > 1) {
                    maxGridSize =
                        (viewModel.currentImageList.size) * (MAX_IMAGE_LIMIT / viewModel.currentImageList.size)
                    Log.d(
                        "imageSW max limit",
                        "Calculated: Row:${viewModel.currentImageList.size} Column:${MAX_IMAGE_LIMIT / viewModel.currentImageList.size} Grid size:$maxGridSize"
                    )
                } else maxGridSize = -1
            }

            Log.d(
                "imageSW max limit",
                "Comparing: current Image size:${viewModel.currentImageList.size} maxGridSize: $maxGridSize"
            )

            return maxGridSize != -1 && viewModel.currentImageList.size == maxGridSize
        } catch (e: Exception) {
            LogUtils.logGlobally("max-limit-calculation-failure $e")
            return false
        }
    }

    private fun openMaxLimitDialog() {
        SubmitDialog( // select a direction
            title = getString(R.string.max_limit_dialog_title),
            prompt = getString(R.string.max_limit_dialog_message),
            yesText = getString(R.string.ok_btn),
            noText = "",
            onClick = { }
        ).show(supportFragmentManager, "DialogFragment")
    }

    private fun toggleFlash() {
        val newFlashType = when(currentFlashType) {
            FlashType.AUTO -> FlashType.OFF
            FlashType.OFF -> FlashType.ON
            FlashType.ON -> FlashType.AUTO
        }
        currentFlashType = newFlashType
        setFlashDrawable()
    }

    private fun setFlashDrawable() {
        val flashDrawable = when(currentFlashType) {
            FlashType.AUTO -> R.drawable.flash_auto
            FlashType.ON -> R.drawable.flash_on
            FlashType.OFF -> R.drawable.flash_off
        }
        flashButton.setImageResource(flashDrawable)
    }
    private fun getFlashMode() =
        when (currentFlashType) {
            FlashType.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashType.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashType.ON -> ImageCapture.FLASH_MODE_ON
        }

    private fun takePhoto(isBlurFeature: String, isCropFeature: String) {
        viewModel.showLoader()
        imageCapture?.flashMode = getFlashMode()
        val imageCapture = imageCapture ?: return
        val nameTimeStamp = viewModel.uuid.toString() + "_" + (viewModel.currentImageList.size + 1)
        val outputDirectory = this.filesDir
        if (outputDirectory != null) Log.d(
            "imageSW outputDirectory",
            outputDirectory.absolutePath.toString()
        )
        val photoFile = File(outputDirectory, "$nameTimeStamp.jpg")

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    LogUtils.logGlobally(Events.CAPTURE_FAILED)
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    LogUtils.logGlobally(Events.CAPTURE_SUCCESS)
                    var bitmap = imageProxy.toBitmap()
                    LogUtils.logGlobally(Events.IMAGE_PROXY_CONVERSION)
                    var requiredHeight = resizedHeightNew!!
                    var requiredWidth = resizedWidthNew!!
                    val needsRotation = imageProxy.imageInfo.rotationDegrees == 90 && viewModel.mode == "portrait"
                    if(needsRotation) {
                        val temp = requiredHeight
                        requiredHeight = requiredWidth
                        requiredWidth = temp
                    }
                    val originalWidthHeight = "Original Width: ${bitmap.width}, Original Height: ${bitmap.height}"
                    bitmap = resizeImgBitmap(bitmap, requiredWidth, requiredHeight)
                    LogUtils.logGlobally(Events.RESIZE_IMAGE, "$originalWidthHeight, resizedWidth: ${bitmap.width}, resizedHeight: ${bitmap.height}")

                    if (needsRotation) bitmap = ImageProcessingUtils.rotateBitmapWithOpenCV(bitmap)
                    LogUtils.logGlobally(Events.ROTATE_IMAGE, "Rotation Needed: $needsRotation Rotation Degrees: ${imageProxy.imageInfo.rotationDegrees}")

                    viewModel.imageSavedCount++

                    saveImageToFile(photoFile, bitmap, this@CameraActivity)

                    mFile = photoFile
                    mBitmap = bitmap
                    captureTime = nameTimeStamp
                    newImageClick = true
                    viewModel.imageName = nameTimeStamp

                    viewModel.hideLoader()


                    // Condition Check Work Flow:
                    // Blur ==> lowLight ==> rotation ==> cropping ==> Saving Final Image
                    if (isBlurFeature.isNotEmpty() && isBlurFeature == "true") {

                        val bitmap1 = mBitmap
                        val targetBmp: Bitmap =
                            bitmap1!!.copy(Bitmap.Config.ARGB_8888, false)


                        val isImgBlur = BlurDetection.runDetection(
                            this@CameraActivity,
                            targetBmp
                        ) // Blur check
                        LogUtils.logGlobally(Events.IMAGE_BLUR, "Is Image Blur: ${isImgBlur.first}")
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
                    imageProxy.close()
                }
            })
    }

    private fun resizeImgBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    }

    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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

                    LogUtils.logGlobally(Events.IMAGE_SAVED, file1.name)

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
        val wideAngleCameraId = findWideAngleCamera(manager) ?: return true
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
        flashButton = findViewById(R.id.flashButton)
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

        const val MAX_IMAGE_LIMIT = 60

        const val BLURRED_IMAGE = "BLURRED IMAGE"
        const val NOT_BLURRED_IMAGE = "NOT BLURRED IMAGE"
    }

    override fun onResume() {
        super.onResume()
        registerSensors()
        setZoomRatio(viewModel.currentZoomRatio)
        backpressedlistener = this

    }

    override fun onPause() {
        backpressedlistener = null
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    private fun registerSensors() {
        accelerometerSensor?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopService(Intent(this, MyServices()::class.java)) // onDestroy
    }

}