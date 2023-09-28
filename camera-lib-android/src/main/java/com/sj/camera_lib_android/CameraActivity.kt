package com.sj.camera_lib_android
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bugfender.sdk.Bugfender
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

class CameraActivity : AppCompatActivity(), Backpressedlistener {
    private var imageCapture: ImageCapture? = null
    private lateinit var viewModel: CameraViewModel
    var isArrowSelected: Boolean = true
    private var mFile: File? = null
    private var mBitmap: Bitmap? = null
    private var mImgUri: Uri? = null
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
    private var mImgUriPS: Uri? = null

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
    private lateinit var zoomLayout: LinearLayout
    private lateinit var radioGroupZoom: RadioGroup
    private lateinit var zoomPercentFinal: String

    // Firebase Database
//    private val mDatabase = FirebaseDatabase.getInstance()
//    private var mDatabaseReference = mDatabase.reference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // hide the action bar
        supportActionBar?.hide()
        val utils = Utils()

//        InternetCheck
        if (!utils.checkInternetConnection(this)) {
            Toast.makeText(this, "Opps! No Internet\nPlease Connect to Internet", Toast.LENGTH_SHORT).show()
        }



        // Initialize Views ID
        initializeIDs()
        // Initialize OpenCV
//        OpenCVLoader.initDebug()

        // Initialize Firebase
//        FirebaseApp.initializeApp(this@CameraActivity)

         if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext)
             Log.d("imageSW FirebaseApp","initialized")
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
            overlayBE = extras.getString("overlapBE")?.toFloat() ?: 20f
            uploadParams = extras.getString("uploadParam") ?: ""
            resolution = extras.getString("resolution") ?: ""
            referenceUrl = extras.getString("referenceUrl") ?: ""
            isBlurFeature = extras.getString("isBlurFeature") ?: ""
            isCropFeature = extras.getString("isCropFeature") ?: ""
            uploadFrom = extras.getString("uploadFrom") ?: "Shelfwatch"

            var message = "Mode: $modeRotation ,uploadParams: $uploadParams , overlayBE: $overlayBE," +
                    " resolution: $resolution, referenceUrl: $referenceUrl, isBlurFeature: $isBlurFeature," +
                    " isCropFeature: $isCropFeature, uploadFrom: $uploadFrom"
            Log.d("imageSW inputFlags", " CA: $message")
            viewModel.dataFromOpenCameraEvent(applicationContext,modeRotation, overlayBE, uploadParams, resolution, referenceUrl, isBlurFeature, isCropFeature,uploadFrom)


        }

        viewModel.discardAllImages() // cameraActivity Launch
        // register BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("thisIsForMyPartner"))


        modeRotation = viewModel.mode

        // Rotation work
        if (modeRotation.isNotEmpty() && modeRotation == "landscape"){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Log.d("imageSW","modeRotation: $modeRotation ,==> landscape ==> modeSelected: $modeRotation ")

            val layoutParam = viewFinder.layoutParams as ConstraintLayout.LayoutParams
                layoutParam.dimensionRatio = "4:3"
                viewFinder.layoutParams = layoutParam

            val layoutParam1 = previewImgPS.layoutParams as ConstraintLayout.LayoutParams
                layoutParam1.dimensionRatio = "4:3"
                previewImgPS.layoutParams = layoutParam1


            /*       *//*val layoutParam = cropImageView.layoutParams as ConstraintLayout.LayoutParams
                layoutParam.dimensionRatio = "4:3"
                cropImageView.layoutParams = layoutParam
*//*

                val layoutParam = demoImg.layoutParams as ConstraintLayout.LayoutParams
                layoutParam.dimensionRatio = "4:3"
                demoImg.layoutParams = layoutParam
*/
//            updateGuidelineConstraintsForLandscape()

            /** for Preview Screen **/
                val layoutParamPS = previewImgPS.layoutParams as ConstraintLayout.LayoutParams
                layoutParamPS.dimensionRatio = "4:3"
                previewImgPS.layoutParams = layoutParamPS

                val layoutParamPS2 = cropImageViewPS.layoutParams as ConstraintLayout.LayoutParams
                layoutParamPS2.dimensionRatio = "4:3"
                cropImageViewPS.layoutParams = layoutParamPS2

                val layoutParamPS3 = imageBlur.layoutParams as ConstraintLayout.LayoutParams
                layoutParamPS3.dimensionRatio = "4:3"
                imageBlur.layoutParams = layoutParamPS3


            // calculation for resizing
            resizedWidthNew = resolution.toInt()
            resizedHeightNew = (resolution.toInt() * 3)/4
            Log.d("imageSW resizeNEW: ", "Landscape WH: $resizedWidthNew, $resizedHeightNew")

        }
        else{
            resizedWidthNew = (resolution.toInt() * 3)/4
            resizedHeightNew = resolution.toInt()
            Log.d("imageSW resizeNEW: ", "Portrait WH: $resizedWidthNew, $resizedHeightNew")

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.d("imageSW","modeRotation: $modeRotation ,==> portrait ==> modeSelected: $modeRotation")



            val layoutParam = viewFinder.layoutParams as ConstraintLayout.LayoutParams
                layoutParam.dimensionRatio = "3:4"
                viewFinder.layoutParams = layoutParam

            val layoutParam1 = previewImgPS.layoutParams as ConstraintLayout.LayoutParams
                layoutParam1.dimensionRatio = "3:4"
                previewImgPS.layoutParams = layoutParam1

                 val layoutParam3 = cropImageViewCL.layoutParams as ConstraintLayout.LayoutParams
                 layoutParam3.dimensionRatio = "3:4"
                 cropImageViewCL.layoutParams = layoutParam3


                 val layoutParam4 = imageBlur.layoutParams as ConstraintLayout.LayoutParams
                 layoutParam4.dimensionRatio = "3:4"
                 imageBlur.layoutParams = layoutParam4


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

//                                Log.d("imageRotation", " Orientation_PORTRAIT =>> $orientation , modeRotation: $modeRotation")
//                                utils.isPortrait = true
//                                utils.isReverseLandscape = false
                                } else {
                                    orientationTv.text = "Change orientation to landscape"
                                    orientationBl.visibility = View.VISIBLE
                                }
//                            Log.d("imageSW ", "Portrait Orientation =>> $orientation")
//                            utils.showToast(this@CameraActivity, "Portrait Orientation")
                            }

                            ORIENTATION_REVERSE_PORTRAIT -> {
                                orientationTv.text = "Change orientation to $modeRotation"
                                orientationBl.visibility = View.VISIBLE
//                            Log.d("imageSWRotation", " ORIENTATION_REVERSE_PORTRAIT =>> $orientation , modeRotation: $modeRotation")

//                            utils.isReverseLandscape = false
//                            utils.isPortrait = false

//                            Log.d("imageSW", "Reverse Portrait Orientation = $orientation")
//                            utils.showToast(this@CameraActivity, "ReversePortrait Orientation")
                            }

                            ORIENTATION_LANDSCAPE -> {
                                if (modeRotation.equals("landscape", true)) {
                                    orientationBl.visibility = View.INVISIBLE
//                                Log.d("imageRotation", " ORIENTATION_LANDSCAPE =>> $orientation , modeRotation: $modeRotation")

//                                utils.isReverseLandscape = false
//                                utils.isPortrait = false

                                } else {
                                    orientationTv.text = "Change orientation to portrait"
                                    orientationBl.visibility = View.VISIBLE
                                }
//                            Log.d("imageSW Landscape ", "Orientation = $orientation")
//                            utils.showToast(this@CameraActivity, "Landscape Orientation")
                            }

                            ORIENTATION_REVERSE_LANDSCAPE -> {
//                            Log.d("imageRotation", " ORIENTATION_REVERSE_LANDSCAPE =>> $orientation , modeRotation: $modeRotation")

                                /*if (mode == "landscape") {
                                viewBinding.orientationBl.root.visibility = View.INVISIBLE
                                utils.isReverseLandscape = true
                                utils.isPortrait = false

                            } else {
                                viewBinding.orientationBl.orientationTv.text = "Change orientation to portrait"
                                viewBinding.orientationBl.root.visibility = View.VISIBLE
                            }*/
                                orientationTv.text = "Change orientation to $modeRotation"
                                orientationBl.visibility = View.VISIBLE

//                            Log.d("Reverse Landscape ", "Orientation = $orientation")
//                            utils.showToast(this@CameraActivity, "Reverse Landscape Orientation")
                            }

                            ORIENTATION_PARALLEL -> {
                                if (modeRotation.equals("portrait", true) && Common.isPortraitParallel) {

//                                Log.d("imageRotation ", " modeRotation: $modeRotation,  ORIENTATION_PARALLEL, =>> $orientation \n==> modeSelected: $modeSelected")

//                                utils.isPortrait = true
//                                utils.isReverseLandscape = false
                                    orientationBl.visibility = View.INVISIBLE
                                } else if (modeRotation.equals(
                                        "landscape",
                                        true
                                    ) && Common.isLandscapeParallel
                                ) {
//                                Log.d("imageRotation ", " modeRotation: $modeRotation,  ORIENTATION_PARALLEL =>> $orientation \n==> modeSelected: $modeSelected")

//                                utils.isReverseLandscape = false
//                                utils.isPortrait = false
                                    orientationBl.visibility = View.INVISIBLE
                                }
                            }

                        }

                    }
                }
            orientationEventListener.enable()

        }else finish()




        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
             startCamera()

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { it ->
                withContext(Dispatchers.Main) {
          Log.d("imageSW state_livedata:", "leftArrow: ${it.showLeftArrow}, rightArrow: ${it.showRightArrow}, downArrow: ${it.showDownArrow}, All Directions: ${it.showArrowAllDirection}")
                    if (it.showLoader) {
                        loader.visibility = View.VISIBLE
                    } else {
//            captureImg.isEnabled = true
                        captureImg.setBackgroundResource(R.drawable.white_solid_circle)
                        loader.visibility = View.GONE
                    }

                    Log.d("imageSW it.enableCaptureBtn"," ${it.enableCaptureBtn}")
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

                    Log.d("imageSW it.nextStep"," ${it.nextStep}")
                    if (it.nextStep == "left") {
                        isArrowSelected = true
                        leftArrowIv.setBackgroundColor(Color.GREEN)
                        rightArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        downArrowIv.setBackgroundColor(Color.TRANSPARENT)
                    } else if (it.nextStep == "right") {
                        isArrowSelected = true
                        rightArrowIv.setBackgroundColor(Color.GREEN)
                        leftArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        downArrowIv.setBackgroundColor(Color.TRANSPARENT)
                    } else if (it.nextStep == "down") {
                        isArrowSelected = true
                        downArrowIv.setBackgroundColor(Color.GREEN)
                        leftArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        rightArrowIv.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        leftArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        rightArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        downArrowIv.setBackgroundColor(Color.TRANSPARENT)
                        rightArrowIv.isEnabled = true
                        leftArrowIv.isEnabled = true
                        downArrowIv.isEnabled = true
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
//            Log.d("top overlay image","exist")
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
                    if (it.isImageListEmpty) {
                        deleteImg.visibility = View.GONE
                        previewPageImgCS.visibility = View.GONE
                        submitBtn1.visibility = View.GONE
                    } else {
                        deleteImg.visibility = View.VISIBLE
                        previewPageImgCS.visibility = View.VISIBLE
                        submitBtn1.visibility = View.VISIBLE
                    }

                    viewModel.imageCapturedListLive.observe(this@CameraActivity, androidx.lifecycle.Observer { imageModel ->

                        if (viewModel.currentImageList.size > 0) {
                                previewPageImgCS.setImageBitmap(imageModel.last().image)
                                zoomLayout.visibility = View.GONE
//                            Log.d("imageSW","currentImageList.size > 0")
                            }
                            else{
                                Common.zoomSelected = "0"
                                zoomLayout.visibility = View.VISIBLE

                                // Check the first radio button (index 0)
                                val firstRadioButton: RadioButton = radioGroupZoom.findViewById(R.id.zoom_0_rb) as RadioButton
                                firstRadioButton.isChecked = true

                                // Zoom Work
                                if (Common.zoomSelected != null) {
                                    zoomCamera("0f", camera!!) // currentImageList.size < 0
                                }
//                                Log.d("imageSW","currentImageList.size < 0")

                            }

                        }
                    )

                    // PreviewScreen work
                    if (viewModel.currentImageList.size>0){
                        previewImgRecycler.adapter = PreviewListAdapter(this@CameraActivity, viewModel.currentImageList,
                            onClick = { clickedImg: Bitmap, croppingPoints1: Array<Int>, file1: File, position1: Int ->

                                previewImgPS.setImageBitmap(clickedImg)
//                    binding?.previewIv?.setImageURI(Uri.fromFile(file1))

                                croppingPointsPS = croppingPoints1
                                mBitmapPS = clickedImg
                                mFilePS = file1
                                mPosPS = position1

                                Log.d("imageSW PrevFrag",
                                    " imageSize: WH " + clickedImg.width + " , " + clickedImg.height + "\nBitmap: $clickedImg")

                                if (clickedImg != null && croppingPointsPS.isNotEmpty()) {
                                    cropStartPS.visibility = View.VISIBLE // showing cropping icon
                                } else {
                                    cropStartPS.visibility = View.GONE

                                }

                            })
                    }
                }
            }


        }
//
        // Zoom in OUT
                radioGroupZoom.setOnCheckedChangeListener { group, checkedId ->
                    val selectedRadioButton:RadioButton = group.findViewById(checkedId)
        //            Toast.makeText(requireContext(), " On checked change :" + " ${selectedRadioButton.text}", Toast.LENGTH_SHORT).show()
                    Common.zoomSelected = selectedRadioButton.text.toString()

                    if (Common.zoomSelected != null) {
                        zoomCamera("${Common.zoomSelected}f", camera!!) // zoom click .25
                    }
                        // Reset background color for all radio buttons
                        for (i in 0 until group.childCount) {
                            val radioButton = group.getChildAt(i) as RadioButton
                            radioButton.setBackgroundColor(Color.parseColor("#7F000000"))
                            radioButton.background = this.getDrawable(R.drawable.bg_round_lightblack)
                        }

                        // Set the background color of the selected radio button
                        selectedRadioButton.setBackgroundColor(Color.parseColor("#000000"))
                    selectedRadioButton.background = this.getDrawable(R.drawable.black_solid_circle)


                }


        //calculate viewFinder Dimensions and set overlapping
        calculateViewDimensions(viewFinder) { width, height ->
            widthNewVF = width
            heightNewVF = height
            Log.d("imageSW viewFinder WH"," Width: $widthNewVF , height: $heightNewVF")

            //Overlapping images
            setImgWidth(overlapImgLeft, overlayBE.toInt())
            setImgWidth(overlapImgRight, overlayBE.toInt())
            setImgHeight(overlapImgTop, overlayBE.toInt())
        }


        // Image referenceUrl WORK
        if (referenceUrl.isNotEmpty()) {
            referenceImg.visibility = View.VISIBLE
        } else {
            referenceImg.visibility = View.GONE

        }

        referenceImg.setOnClickListener {
            val imageDialog = ImageDialog(this,referenceUrl)
            imageDialog.show()

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
                Log.d("imageSW takePhoto"," START")
                takePhoto(isBlurFeature, isCropFeature)
            }else{
                openDirectionDialog()
            }
        }
        
        leftArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
        //        Log.i("isArrowSelected==>> ",""+isArrowSelected)
                viewModel.leftArrowClicked(it1)
            }
        }

        rightArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            Log.d("imageSW rightArrow", " clicked")
            this.let { it1 ->
                isArrowSelected = true
                viewModel.rightArrowClicked(it1)
//        Log.i("isArrowSelected==>> ",""+isArrowSelected)
            }
        }

        downArrowIv.setOnClickListener {
            it.setBackgroundColor(Color.GREEN)
            this.let { it1 ->
                isArrowSelected = true
//        Log.i("isArrowSelected==>> ",""+isArrowSelected)

                viewModel.bottomArrowClicked(it1)
            }
        }

        deleteImg.setOnClickListener {
            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }
            if (viewModel.currentImageList.size > 0 ){
                viewModel.deleteLastCapturedImage()
            }else {
                isArrowSelected = true // size = 0
            }

            viewModel.renderUi() // deleteImg
        }

        submitBtn1.setOnClickListener {
            if (utils.checkInternetConnection(this)) {

            SubmitDialog( // submitBtn1
                getString(R.string.dialog_submit),
                getString(R.string.yes_btn),
                getString(R.string.no_btn),
                onClick = {
                    if (viewModel.currentImageList.isNotEmpty()) {
                        viewModel.showLoader() // submitBtn1
                        uploadSaveImages(this@CameraActivity) // submitBtn1
                    }
//                    Bugfender.d("android_data_uploaded_chf","ok")
//                    finish()
                }
            ).show(supportFragmentManager, "DialogFragment")
        }else {
            viewModel.hideLoader()
            Toast.makeText(this, "Opps! No Internet\nPlease Connect to Internet", Toast.LENGTH_SHORT).show()
        }

        }

        uploadBtnPS.setOnClickListener {
            if (utils.checkInternetConnection(this)) {

                SubmitDialog( // uploadBtnPS
                    getString(R.string.dialog_submit),
                    getString(R.string.yes_btn),
                    getString(R.string.no_btn),
                    onClick = {
                        if (viewModel.currentImageList.isNotEmpty()) {
                            viewModel.showLoader() // uploadBtnPS
                            uploadSaveImages(this@CameraActivity) // uploadBtnPS
                        }
//                    Bugfender.d("android_data_uploaded_chf","ok")
//                    finish()
                    }
                ).show(supportFragmentManager, "DialogFragment")
            }else {
                viewModel.hideLoader()
                Toast.makeText(this, "Opps! No Internet\nPlease Connect to Internet", Toast.LENGTH_SHORT).show()
            }
        }
        
        previewPageImgCS.setOnClickListener{
            cropStartPS.visibility = View.GONE
            cameraLayout.visibility = View.GONE
            previewImgLayout.visibility = View.VISIBLE
        }

        exitBtnPS.setOnClickListener{
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            startCamera() // exitBtnPS
        }

        captureMorePS.setOnClickListener{
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            startCamera() // captureMorePS
        }



        //Cropping Work
        resetCropBtnCL.setOnClickListener {
            resetCroppingImg(cropImageViewCL, mBitmap!!,Uri.fromFile(mFile),"CS")
//                viewBinding?.cropImageView?.let { it1 -> resetCroppingImg(it1, Uri.fromFile(mFile)) }
        }

        // Retake from Crop screen
        retakeCropBtnCL.setOnClickListener{
            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }

            mBitmap?.recycle()
            mBitmap = null


//            bitmapFinal?.recycle()
//            bitmapFinal = null

            mFile = null
//                fileFinal = null
        }
        // Retake from Blur screen
        retakeBlurImg.setOnClickListener{

            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE

            mFile?.let { it1 -> viewModel.deleteFile(it1.path) }

            mBitmap?.recycle()
            mBitmap = null

//            bitmapFinal?.recycle()
//            bitmapFinal = null

            mFile = null
//                fileFinal = null
        }



        //Done Button
        cropDoneBtnCL.setOnClickListener {

            // this is for CROP DONE Button
            cropImageViewCL.getCroppedImageAsync()
        }

        // Continue Button
        notBlurContinueLL.setOnClickListener{
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
//           viewBinding?.imageView?.setImageBitmap(result.bitmap)

            // Get the coordinates of the four corners
            val cropRect = result.cropRect
            val left = cropRect.left
            val top = cropRect.top
            val right = cropRect.right
            val bottom = cropRect.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            coordinatesCrop = arrayOf(left,top, right, bottom)
            Log.d("imageSW Cropping", "DONE coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}")
//            Log.d("imageSW bitmap crop"," size: ${bitmapFinal?.width} , ${bitmapFinal?.height}")


//            checkLowLightSave after cropping
            if (mFile != null && mBitmap != null && mBitmap.toString().isNotEmpty()) {
                mFile?.let {
                    checkLowLightSave(it, mBitmap!!) // after cropping
                }

//            utils.showToast(this, "File Saved")
            } else {
                Bugfender.e("android_cropping_bitmap","mFile or mBitmap is empty/ null")
                Log.d("imageSW", " checkLowLightSave: mFile or mBitmap is empty/ null")
//            utils.showToast(this, "fileFinal or bitmapFinal is empty/ null ")
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

//            binding?.cropImageView?.setImageUriAsync(Uri.fromFile(mFile))
            cropImageViewPS.setImageBitmap(mBitmapPS)
            if (cropRectValuesPS != null) {
                cropImageViewPS.cropRect = cropRectValuesPS
            }
            Log.d("imageSW resizedBitmapPS", " Size: WH " + mBitmap?.width + " , " + mBitmap?.height)

        }

        // Reset Button Click
        resetCropBtnPS.setOnClickListener {
//            binding?.imagePreviewLayout?.visibility = View.GONE
//            binding?.croppingLayout?.visibility = View.VISIBLE
//
//            val cropRectValues = croppingRect(croppingPoints!!, cropImageView)
//
//            binding?.cropImageView?.setImageUriAsync(Uri.fromFile(mFile))
            if (cropRectValuesPS != null) {
                cropImageViewPS.cropRect = cropRectValuesPS
            }else{
                mBitmapPS?.let { it1 -> resetCroppingImg(cropImageViewPS, it1,Uri.fromFile(mFilePS), "PS") }
            }

        }

        /* // Retake Button Click
         binding?.croppingRetakeBtn?.setOnClickListener {
 //            resetCroppingImg(cropImageView, mBitmap)
         }*/


        cropDoneBtnPS.setOnClickListener {
            cropImageViewPS.getCroppedImageAsync()
        }
//        val cropImageView: CropImageView = view.findViewById(R.id.cropImageView)

        //getCropped Image Result
        cropImageViewPS.setOnCropImageCompleteListener { _, result ->
            // Get the cropped image and display it in the ImageView
//           viewBinding?.imageView?.setImageBitmap(result.bitmap)

            // Get the coordinates of the four corners
            val cropRect = result.cropRect

            val left = cropRect.left
            val top = cropRect.top
            val right = cropRect.right
            val bottom = cropRect.bottom

            // format sd be like this [Xmin, Ymin, Xmax, Ymax]
            val coordinatesCrop = arrayOf(left,top, right, bottom)
            Log.d("imageSW Cropping22", "DONE coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${coordinatesCrop.contentToString()}")

            if (coordinatesCrop != null && mFile != null) {

//                viewModel.updateCropCoordinates(mFile!!.toUri().toString(), mPos, coordinatesCrop)
//                simulateItemClick(mPos)

                // this is to update and get new coordinates
                if (mPosPS != -1) {
                    viewModel.currentImageList[mPosPS].croppedCoordinates = coordinatesCrop
//                    = ImageDetailsModel(coordinatesCrop)
                    previewImgRecycler?.adapter?.notifyDataSetChanged()
                    Log.d("imageSW new CropPS","updated ok")
                    Bugfender.i("android_new_crop_pvf","updated ok")

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



//        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    } // END of onCreate

    private fun uploadSaveImages(context: Context) {
        lifecycleScope.launch{
            viewModel.uploadImages(context) // uploadSaveImages

        }
        if (viewModel.currentImageList.size == viewModel.imageUploadList.size) {
            viewModel.hideLoader()
            viewModel.discardAllImages()
            finish()
            /*//Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE*/
        }
    }

    private fun cropLowLightCheck(mBitmap1: Bitmap, mFile1: File, isCropFeature1: String) {
        if (isCropFeature1 != null && isCropFeature1.isNotEmpty() && isCropFeature1.equals("true",true)) {
            croppingStart(mBitmap1)
        }else{
            coordinatesCrop = emptyArray()
            // Low Light and save work
            val enhancedRotedBitmap = checkLowLightSave(mFile1, mBitmap1) //No Cropping
        }
    }

    private fun croppingStart(resizedEnhancedRotatedBitmap: Bitmap) {
        // Cropping Work
        Log.d("imageSW cropping: ", "start with bitmap => $resizedEnhancedRotatedBitmap" +
                "\nsize WH: ${resizedEnhancedRotatedBitmap.width} , ${resizedEnhancedRotatedBitmap.height}")

        //Show Hide Layouts
        cameraLayout.visibility = View.GONE
        previewImgLayout.visibility = View.GONE
        cropLayout.visibility = View.VISIBLE
        blurLayout.visibility = View.GONE


        /* viewBinding?.cropImageView?.apply {
             viewBinding?.cropImageView?.setGuidelines(CropImageView.Guidelines.ON)
         }*/

        if (resizedEnhancedRotatedBitmap != null && resizedEnhancedRotatedBitmap.toString().isNotEmpty()) {
            cropImageViewCL.setImageBitmap(resizedEnhancedRotatedBitmap) // cropping
//          cropImageViewCL.setImageUriAsync(mImgUri)
        } else {
            Log.d("imageSW cropping: ", "NOT start with bitmap NULL => $resizedEnhancedRotatedBitmap")
            Bugfender.e("android_cropping_start ", "not start with bitmap is null or empty => $resizedEnhancedRotatedBitmap")
        }

        viewModel.hideLoader()
    }

    private fun checkLowLightSave(file1: File, bitmap2: Bitmap): Bitmap {
        val targetBmp: Bitmap = bitmap2.copy(Bitmap.Config.ARGB_8888, false)

        val isLowLight = ImageProcessingUtils.isLowLightImage(targetBmp)
        Log.d("imageSW isLowLight 1: ", "" + isLowLight)

        var enhancedRotatedBitmap: Bitmap? = null

          if (isLowLight) {
              // image has low light
              Common.showToast(this, "This image has low light!")
              // Image Enhancement
//              val enhancedBitmap2 = ImageProcessingUtils.enhanceImageBrightness(targetBmp, 1.5f)
              Log.d("imageSW isLowLight 2: ", " $isLowLight  Enhancement DONE")

              // check And Do Rotation for reverse mode
//              val rotatedEnhancedBitmap = checkAndDoRotation(file1, enhancedBitmap2,resolution)
              enhancedRotatedBitmap = bitmap2

          } else {
              // No Low light Continue with your normal flow
  //        utils.showToast(requireContext(), "The image has not low light")

              // check And Do Rotation for reverse mode
//              val rotatedBitmap = checkAndDoRotation(file1, bitmap2, resolution)
              enhancedRotatedBitmap = bitmap2
              Log.d("imageSW ", " No Enhancement")

          }


//        bitmapFinal = bitmap2
//        fileFinal = file1

        // File Saving work
        if (mFile != null && enhancedRotatedBitmap.toString().isNotEmpty() && captureTime.isNotEmpty()) {

            viewModel.handleClickedImage(enhancedRotatedBitmap, coordinatesCrop, mFile!!, captureTime, this@CameraActivity)

//            saveImg4React(fileFinal!!, bitmapFinal!!)
            Bugfender.d("android_image_update_react","done")

            //Show Hide Layouts
            cameraLayout.visibility = View.VISIBLE
            previewImgLayout.visibility = View.GONE
            cropLayout.visibility = View.GONE
            blurLayout.visibility = View.GONE
//            utils.showToast(requireContext(), "File Saved")
        } else {
            Bugfender.e("android_image_save","mFile or mBitmap is empty/ null")
            Log.d("imageSW save", "fileFinal or bitmapFinal is empty/ null")
//            utils.showToast(requireContext(), "fileFinal or bitmapFinal is empty/ null ")
        }

        return enhancedRotatedBitmap
    }

    private fun croppingRect(croppingPoints: Array<Int>, cropImageView: CropImageView): Rect? {
        Log.d("imageSW CroppingPS 1", " coordinatesCrop: Xmin, Ymin, Xmax, Ymax: ${croppingPoints.contentToString()}")

        // Convert the guideline coordinates to a Rect object
        val left = croppingPoints[0]
        val top = croppingPoints[1]
        val right = croppingPoints[2]
        val bottom = croppingPoints[3]
        val guidelinesRect = Rect(left, top, right, bottom)
        Log.d("imageSW CroppingPS 2", " All coordinates left: $left , top: $top , right: $right , bottom: $bottom")

        return guidelinesRect

    }

    override fun onBackPressed() {
            if (backpressedlistener != null){
//            utils.showToast(this,"Camera Back Pressed")
            if (viewModel.currentImageList.size > 0) {
                SubmitDialog( // onBackPressed
                    getString(R.string.discard_submit),
                    getString(R.string.yes_btn),
                    getString(R.string.no_btn),
                    onClick = {
                        viewModel.discardAllImages() // back button discard
                        //Layout Visibility
//                        viewBinding?.cameraLayout?.visibility = View.VISIBLE
//                        viewBinding?.croppingLayout?.visibility = View.GONE
//                        viewBinding?.blurLayout?.visibility = View.GONE
                    }
                ).show(supportFragmentManager, "DialogFragment")
            } else {
//                context?.let { it1 -> viewModel.dismissCamera(it1) }
                finish()
//                activity?.onBackPressed()
            }

        }
    }
    private fun openDirectionDialog() {
        SubmitDialog( // select a direction
            getString(R.string.dialogTitle),
            getString(R.string.ok_btn),
            "",
            onClick = {
                this?.let { it1 ->

                    Toast.makeText(this, getString(R.string.dialogTitle), Toast.LENGTH_SHORT).show()

                    /*//        view.isEnabled = false
                              view?.setBackgroundResource(R.drawable.red_solid_circle)
                              viewModel.showLoader()
                              takePhoto()
                              viewModel.resetArrowClicked()*/
                }

            }
        ).show(supportFragmentManager, "DialogFragment")
    }

    private fun takePhoto(isBlurFeature: String, isCropFeature: String) {
        viewModel.showLoader()
        val imageCapture = imageCapture ?: return
        val nameTimeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val outputDirectory = this.filesDir
        if (outputDirectory != null) Log.d("imageSW outputDirectory", outputDirectory.absolutePath.toString())
        val photoFile = File(outputDirectory, "$nameTimeStamp.png")

        // Create time-stamped output file to hold the image
//        val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".png")
        // Create output options object which contains file + metadata
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
                    Log.d("imageSW takePhoto"," END , img: $savedImageUri at time $nameTimeStamp")

                   /* val dimensionsAndResolution = getImageDimensionsAndResolution(this@CameraActivity, savedImageUri)
                    if (dimensionsAndResolution != null) {
                        val (width, height, resolution) = dimensionsAndResolution
                        Log.d("imageSW Info", "Width: $width, Height: $height, Resolution: $resolution")
                    } else {
                        Log.e("imageSW Info", "Failed to get image info")
                    }*/



                    // set the saved uri to the image view
//                    previewPageImgCS.visibility = View.VISIBLE
//                    previewPageImgCS.setImageURI(savedImageUri)
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && savedImageUri!=null) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, savedImageUri))
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, savedImageUri)
                    }

                    mFile = photoFile
                    mImgUri = savedImageUri
                    mBitmap = bitmap
                    captureTime = nameTimeStamp

                    val msg = "Photo capture succeeded: $savedImageUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                    viewModel.hideLoader()


                    // Condition Check Work Flow:
                    // Blur ==> lowLight ==> rotation ==> cropping ==> Saving Final Image
                    if (isBlurFeature != null && isBlurFeature.isNotEmpty() && isBlurFeature == "true") {

                        val bitmap1 = mBitmap
                        val targetBmp: Bitmap = bitmap1!!.copy(Bitmap.Config.ARGB_8888, false)


                        val isImgBlur = BlurDetection.runDetection(this@CameraActivity, targetBmp) // Blur check
//                        val isImgBlur2 = ImageProcessingUtils.isImageBlurry(mBitmap!!) // Blur check
//                        val isImgBlur = ImageProcessingUtils.checkBluryImg(mBitmap!!) // Blur check
//                        Log.d("imageSW Blur: ", "isImgBlur3: ${isImgBlur.first}")
                        Log.d("imageSW Blur: ", "isBlurFeature: $isBlurFeature  ,isImgBlur: $isImgBlur")
                        Bugfender.d("android_image_blur","isBlurFeature: $isBlurFeature  ,isImgBlur: $isImgBlur")
                        if (isImgBlur.first) {
                            // Image is blurred
//                            viewBinding?.demoImg?.setImageBitmap(bitmapFinal!!)
                            imageBlur.setImageURI(savedImageUri)
//                            Glide.with(this@CameraActivity).load(savedImageUri).into(imageBlur)

                            //Show Hide Layouts
                            cameraLayout.visibility = View.GONE
                            previewImgLayout.visibility = View.GONE
                            cropLayout.visibility = View.GONE
                            blurLayout.visibility = View.VISIBLE

//                                utils.showToast(requireContext(), "Image is blur!")

                        } else {
                            // Image is not blurred
//                        utils.showToast(requireContext(), "Image is not blur.")
                            cropLowLightCheck(mBitmap!!, mFile!!, isCropFeature) //not blurred

                        }
                    } else {
                        // check Low Light Image
//                        val enhancedRotedBitmap2 = checkLowLightSave(file, mBitmap!!, isCropFeature)
                        cropLowLightCheck(mBitmap!!, mFile!!, isCropFeature) // No blur features
                    }
                }
            })
    }

    private fun startCamera() {
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
                        // Values returned from our analyzer are passed to the attached listener
                        // We log image analysis results here - you should do something useful
                        // instead!
//                        Log.d("imageSW imageAnalyzer", "Average luminosity: $luma")
                    })
                }


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector,preview, imageCapture, imageAnalyzer)

                // Zoom Work
                 if (viewModel.currentImageList.size > 0) {
                    if (Common.zoomSelected != null) {
                        zoomCamera("${Common.zoomSelected}f", camera!!) // startCamera
                    }
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "ShelfwatchCamSDK").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
//                finish()
            }
        }
    }


    private fun setImgWidth(imageView: ImageView, overlapBE: Int) {
        val currentWidth = widthNewVF
        val newWidth = (currentWidth?.times(overlapBE))?.div(100) // Calculate 20% of the current width

        val layoutParams = imageView.layoutParams
        if (newWidth != null) {
            layoutParams.width = newWidth
//            layoutParams.height = heightNewVF!!
        }
        imageView.layoutParams = layoutParams
        Log.d("imageSW","OW:$currentWidth, newWidth set at $overlapBE% newWidth: $newWidth")
    }
    private fun setImgHeight(imageView: ImageView, overlapBE: Int) {
        val currentHeight = heightNewVF
        val newHeight = (currentHeight?.times(overlapBE))?.div(100) // Calculate 20% of the current width

        val layoutParams = imageView.layoutParams
        if (newHeight != null) {
            layoutParams.height = newHeight
//            layoutParams.width = widthNewVF!!

        }
        imageView.layoutParams = layoutParams
        Log.d("imageSW","OH:$currentHeight, newHeight set at $overlapBE% newHeight: $newHeight")
    }
    // Function to calculate the height and width of a view
    fun calculateViewDimensions(view: View, onDimensionsCalculated: (width: Int, height: Int) -> Unit) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
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
//        Log.d("imageSW", "crop%  $percent")
        val width = bitmap.width
        val height = bitmap.height
        val cropHeight = (height * percent / 100f).toInt()
        val cropY = height - cropHeight
        return Bitmap.createBitmap(bitmap, 0, cropY, width, cropHeight)
    }

    private fun cropBitmapByWidthFromRight(bitmap: Bitmap, percent: Int): Bitmap {
//        Log.d("imageSW", "crop%  $percent")
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = (width * percent / 100f).toInt()
        val cropX = width - cropWidth
        return Bitmap.createBitmap(bitmap, cropX, 0, cropWidth, height)
    }

    private fun cropBitmapByWidthFromLeft(bitmap: Bitmap, percent: Int): Bitmap {
//        Log.d("imageSW", "crop%  $percent")
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = (width * percent / 100f).toInt()
        return Bitmap.createBitmap(bitmap, 0, 0, cropWidth, height)
    }


    fun getImageDimensionsAndResolution(context: Context, imageUri: Uri): Triple<Int, Int, String>? {
        try {
            // Get the display metrics to calculate screen density
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val density = displayMetrics.densityDpi

            // Decode the image to get its dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri), null, options)

            // Calculate the dimensions
            val width = options.outWidth
            val height = options.outHeight

            // Calculate the resolution
            val resolution = "${width}x${height} (${(width * density / 160f).toInt()}dpi)"

            return Triple(width, height, resolution)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }



    private fun resetCroppingImg(cropImageView: CropImageView,mBitmap1: Bitmap, imgUri1: Uri, screen: String) {
        Log.d("imageSW reset", " imgUri==>>  $imgUri1")
        cropImageView.resetCropRect()
        if (imgUri1 != null && imgUri1.toString().isNotEmpty()) {
//            cropImageView.setImageUriAsync(imgUri1)
            cropImageView.setImageBitmap(mBitmap1)

        } else
            Common.showToast(this, "Please capture image before reset")
    }

    //Zooming camera Work
        private fun zoomCamera(zoomPercent: String, camera1: Camera) {
           val cameraControl: CameraControl? = camera1?.cameraControl
           val cameraInfo: CameraInfo? = camera1?.cameraInfo
           val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
    //       var newZoomRatio = currentZoomRatio + zoomPercent.toFloat()
           var newZoomRatio = 1f
           if (zoomPercent.toFloat() == 0f){
               newZoomRatio = 1f
           }else if (zoomPercent.toFloat() == 0.25f){
               newZoomRatio = 1.25f
           }else if (zoomPercent.toFloat() == 0.5f){
               newZoomRatio = 1.75f
           }else if (zoomPercent.toFloat() == 0.75f){
               newZoomRatio = 2.5f
           }else if (zoomPercent.toFloat() == 1.0f){
               newZoomRatio = 3.5f
           }

           Log.d("imageSW ZOOM","newZoomRatio: $newZoomRatio, currentZoomRatio: $currentZoomRatio , zoomPercent: ${zoomPercent.toFloat()}")
           cameraControl?.setZoomRatio(newZoomRatio.coerceIn(cameraInfo?.zoomState?.value!!.minZoomRatio, cameraInfo.zoomState.value!!.maxZoomRatio))
        }



    private fun initializeIDs() {
        // for Camera Layout
        viewFinder = findViewById(R.id.viewFinder)
        captureImg = findViewById(R.id.image_capture_button)
        deleteImg = findViewById(R.id.delete_btn)
        previewPageImgCS = findViewById(R.id.preview_btn)
        crossImg = findViewById(R.id.cross_iv)
        referenceImg = findViewById(R.id.imgReference_iv)
        submitBtn1 = findViewById(R.id.submitImgLL)
        zoomLayout = findViewById(R.id.zoomLL)
        radioGroupZoom = findViewById(R.id.radioGroupZoom)
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
            var index = intent!!.getIntExtra("index",0)
            Log.e("imageSW myBroadcastReceiver","All images Uploaded Successfully of size $index")

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
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private const val REQUEST_CODE_PERMISSIONS = 20
        var backpressedlistener: Backpressedlistener? = null
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val Orientation_PORTRAIT = 0
        private const val ORIENTATION_REVERSE_PORTRAIT = 1
        private const val ORIENTATION_LANDSCAPE = 2
        private const val ORIENTATION_REVERSE_LANDSCAPE = 3
        private const val ORIENTATION_PARALLEL = -1

        private const val BLUR_THRESHOLD = 200
        const val BLURRED_IMAGE = "BLURRED IMAGE"
        const val NOT_BLURRED_IMAGE = "NOT BLURRED IMAGE"
    }

    override fun onResume() {
        super.onResume()
        backpressedlistener = this
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, IntentFilter("thisIsForMyPartner"))// onResume

    }
    override fun onPause() {
        backpressedlistener = null
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopService(Intent(this, MyServices()::class.java)) // onDestroy
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver) // Unbind broadcastR in onDestroy
    }

}