package com.sj.camera_lib_android.ui.viewmodels

/**
 * @author Saurabh Kumar 11 September 2023
 * **/

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sj.camera_lib_android.CameraActivity
import com.sj.camera_lib_android.models.ImageDetailsModel
import com.sj.camera_lib_android.models.ImageModel
import com.sj.camera_lib_android.models.ImageUploadModel
import com.sj.camera_lib_android.services.MyServices
import com.sj.camera_lib_android.utils.Common
import com.sj.camera_lib_android.utils.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.max


class CameraViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _previewFlow = MutableStateFlow<Int>(0)
    val previewFlow: StateFlow<Int> = _previewFlow.asStateFlow()

    var mode = ""
    var overlapBE = 0.2f
    var upload_param = ""
    var resolution = ""
    var referenceUrl = ""
    var isBlurFeature = ""
    var isCropFeature = ""
    var uploadFrom = ""
    var imageName = ""
    var lastDirection = ""

    private var directionSelected = ""
    var directionForOverlap = ""
    var rowSum: Int = 0
    val uuid = UUID.randomUUID()
    var maxRow = 0
    var maxCol = 0

    var wideAngleSet = true
    var imageSavedCount = 0
    var submitClicked = false
    var isRetake: Boolean = false

    var currentZoomRatio = 1.0
    var zoomEnabled: Boolean = true

    val imageUploadList: MutableList<ImageUploadModel> = mutableListOf()

    var imageWidth: Int = 0
    var imageHeight: Int = 0

    val currentImageList = arrayListOf<ImageDetailsModel>()
    val imageCapturedListLive: MutableLiveData<ArrayList<ImageDetailsModel>> = MutableLiveData()


    //ImageDetails
    private var directionID: String = ""
    private var isAutomaticID: Boolean = false
    private var rowID: Double = 0.0
    private var stepsTakenID: ArrayList<String> = ArrayList<String>()
    private var nextStepID: String = ""


    private var positionMatrix: IntArray = intArrayOf(0, 0)
    private var dimensionMatrix: IntArray = intArrayOf(0, 0)
    private var overlapArray: Array<Any> = arrayOf(0, 0, 0, 0.0f, 0.0f, 0.0f)

    // Format  [isTop, isLeft, isRight, top_overlap_value, left_overlap_value, right_overlap_value]

    private var mySharedPref: String = "MyPrefsSW"
    private lateinit var mContext: Context

    /* Overlap toggle */
    var backendToggle: Boolean = true
    var overlapToggleChecked = false

    fun handleClickedImage(
//    fileUri: Uri,
        bmp1: Bitmap,
        coordinatesCropped: Array<Int>,
        file1: File,
        captureTime: String,
        context: Context
    ) {

        rowSum = if (currentImageList.isEmpty()) { // condition for 1st image capture
            1
        } else {
            currentImageList.last().row.toInt()
        }

        if (directionSelected.isNotEmpty() && directionSelected.equals("down", true)) {
            rowSum = currentImageList.last().row.toInt() + 1
        }


        // Save Details in class
        Log.d("imageSW imageDetails Saving", "START ===>> directionSelected: $directionSelected")
        rowID = rowSum.toDouble()

        if (directionSelected.isNotEmpty() && !directionSelected.equals("down", true)) {
            directionID = directionSelected
        }

        // stepsTaken Work
        if (currentImageList.isNotEmpty()) {
            stepsTakenID = currentImageList.last().stepsTaken

            if (currentImageList.last().isAutomatic) {
                stepsTakenID.add("")
            } else stepsTakenID.add(directionSelected)
        } else stepsTakenID.add("")

        // Position and Dimension Work
        if (currentImageList.isNotEmpty()) {
            if (currentImageList.last().stepsTaken.size > 1 && currentImageList.last().stepsTaken[1] == "down") {
                val sum = currentImageList.size % rowID.toInt()
                val col = currentImageList.size / rowID.toInt()

                val lastDir = if (stepsTakenID.last() != "") stepsTakenID.last() else lastDirection
                val direction = if (lastDir == "left") -1 else 1

                positionMatrix = intArrayOf(direction * col, sum)
                dimensionMatrix = intArrayOf(col + 1, rowID.toInt())
                maxRow = max(maxRow, col + 1)
                maxCol = max(maxCol, rowID.toInt())

                Log.d(
                    "imageSW sumDown",
                    " $sum, positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix"
                )

            } else {
                val sum = currentImageList.size % rowID.toInt()

                val lastDir = if (stepsTakenID.last() != "") stepsTakenID.last() else lastDirection
                val direction = if (lastDir == "left") -1 else 1

                positionMatrix = intArrayOf(direction * currentImageList.size, sum)
                dimensionMatrix = intArrayOf(currentImageList.size + 1, rowID.toInt())
                maxRow = max(maxRow, currentImageList.size + 1)
                maxCol = max(maxCol, rowID.toInt())

                Log.d(
                    "imageSW sumLR",
                    " $sum, positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix"
                )

            }
        } else {
            positionMatrix = intArrayOf(0, 0)
            dimensionMatrix = intArrayOf(1, 1)

            Log.d(
                "imageSW sum 1stImg",
                "positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix"
            )
        }

        Log.d("imageSW grid last Step", stepsTakenID.last())
        Log.d(
            "imageSW grid positionMatrix",
            "${currentImageList.size + 1} ${positionMatrix[0]} ${positionMatrix[1]}"
        )
        Log.d(
            "imageSW grid dimensionMatrix",
            "${currentImageList.size + 1} ${dimensionMatrix[0]} ${dimensionMatrix[1]}"
        )

        // getOverlapArray Work
        overlapArray = getOverlapArray()
        Log.d("imageSW overlapArray", overlapArray.contentToString())


        currentImageList.add(
            ImageDetailsModel(
                positionMatrix,
                dimensionMatrix,
                SimpleDateFormat(
                    CameraActivity.FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()),
                currentZoomRatio.toString(),
                mode,
                directionID,
                isAutomaticID,
                rowID,
                stepsTakenID,
                nextStepID,
                overlapArray.contentToString(),
                upload_param,
                ImageModel("$file1", "image/jpeg", "$captureTime.jpg"),
                file1,
                bmp1,
                coordinatesCropped
            )
        )

        Log.d(
            "imageSW handle_img",
            " currentImageList.size: ${currentImageList.size} , rowSum: $rowSum"
        )
        imageCapturedListLive.value = currentImageList
        Log.d("imageSW imageDetails Saving", " END")

        if (currentImageList.size > 1) directionSelected = ""
        // isAutomatic Process work
        isAutomaticID = if (currentImageList.isNotEmpty()) {
            if (!currentImageList.last().isAutomatic) {
                checkSetAutomatic()
            } else {
                currentImageList.last().isAutomatic
            }
        } else false
        Log.d("imageSW isAutomaticID", "$isAutomaticID")

        clearSharedPref()
        imageUploadList.clear()
        renderUi() // handleClickedImage

    }

    fun handleDeletedImage() {
        // Assume rowSum as row Index (1-indexed)
        rowSum = if (currentImageList.isEmpty()) { // condition for 1st image capture
            1
        } else {
            currentImageList.last().row.toInt()
        }

        if (directionSelected.isNotEmpty() && directionSelected.equals("down", true)) {
            rowSum = currentImageList.last().row.toInt() + 1
        }

        rowID = rowSum.toDouble()

        // Why?
        if (directionSelected.isNotEmpty() && !directionSelected.equals("down", true)) {
            directionID = directionSelected
        }

        // stepsTaken Work
        stepsTakenID.removeLast()

        // getOverlapArray Work
        overlapArray = getOverlapArray()
        Log.d("imageSW overlapArray", overlapArray.contentToString())

        if (currentImageList.size > 1) directionSelected = ""
        // isAutomatic Process work
        isAutomaticID = if (currentImageList.isNotEmpty()) {
            if (!currentImageList.last().isAutomatic) {
                checkSetAutomatic()
            } else {
                currentImageList.last().isAutomatic
            }
        } else false
        Log.d("imageSW isAutomaticID", "$isAutomaticID")
    }

    fun uploadImages(context: Context) {
        Log.d("imageSW", "Image Submitted")
        val deviceName = getDeviceModel()
        val utils = Utils()

        clearSharedPref()
        imageUploadList.clear()

        if (currentImageList.isNotEmpty()) {
            var dimension = intArrayOf(maxCol, maxRow)
            Log.d("imageSW grid", "${dimension[0]} ${dimension[1]}")

            imageUploadList.addAll(currentImageList.mapIndexed { index, imageDetails ->
                var cropCoordinates = arrayOf(0, 0, 0, 0)
                if(imageDetails.croppedCoordinates.isEmpty())
                    imageDetails.croppedCoordinates = arrayOf(0, 0, imageWidth, imageHeight)
                imageDetails.croppedCoordinates.let {
                    cropCoordinates = arrayOf(it[0], it[1], it[2] - it[0], it[3] - it[1])
                }

                var position = imageDetails.position

                if(backendToggle && !overlapToggleChecked) {
                    position = intArrayOf(0, 0)
                    dimension = intArrayOf(1, 1)
                }

                val uploadParam = JSONObject(upload_param)

                val metadata = JSONObject(uploadParam.getString("metadata"))
                metadata.put("is_wide_angle", if (wideAngleSet) 1 else 0)
                uploadParam.put("metadata", metadata)

                ImageUploadModel(
                    // Map properties from ImageDetailsModel to ImageUploadModel
                    position.contentToString(),
                    dimension.contentToString(), "", "", "${currentImageList.size}",
                    imageDetails.appTimestamp,
                    imageDetails.orientation,
                    imageDetails.zoomLevel, uuid.toString(),
                    cropCoordinates.contentToString(), "${imageDetails.overlapPercent}",
                    uploadParam.toString(), "${imageDetails.file}", "image/jpeg",
                    imageDetails.file.toString().substringAfterLast("/"),
                    last_image_flag = if(index == currentImageList.size - 1) "1" else "0"
                )

            })
            Log.d(
                "imageSW uploadImages",
                "${imageUploadList.size} Last Image ===>> ${imageUploadList.last()}"
            )

            //Saving for offlineMode
            saveToSharedPref(imageUploadList)

            // Upload to Firebase or send List to 3rd Party
            // upload to Firebase
            // Call My Service for image Upload to firebase in the background

            val projectId = JSONObject(upload_param).get("project_id")

            if (imageUploadList.isNotEmpty()) {
                // Start Service
                val intent = Intent(context, MyServices()::class.java) // image Upload from gallery
                intent.putParcelableArrayListExtra("mediaList", ArrayList(imageUploadList))
                intent.putExtra("deviceName", deviceName)
                intent.putExtra("project_id", projectId.toString())
                intent.putExtra("uuid", uuid.toString())
                context.startService(intent)

            } else {
                Log.e("imageSW startFbService", "imageUploadList is EMPTY/ NULL")
                Toast.makeText(context, "imageUploadList isEmpty", Toast.LENGTH_SHORT).show()
            }

//        uploadToFirebase(mContext, imageUploadList.last(), firebaseDBReference1, imageUploadList) //uploadImages

            //        Broadcast only when uploadFrom from 3rd party

            if (uploadFrom.isNotEmpty() && uploadFrom != "Shelfwatch") {
                Log.d("imageSW UploadTo", "3rd party")
                val intent = Intent("DataSaved")
                intent.putExtra("status", "Done")
                intent.putParcelableArrayListExtra("imageListSaved", ArrayList(imageUploadList))
                intent.putExtra("deviceName", getDeviceModel())
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
            }


        } else {
            Common.showToast(mContext, "Image list is empty now!")
        }

    }

    private fun saveToSharedPref(imageUploadList1: MutableList<ImageUploadModel>) {
        // Convert the list to a JSON string
        val gson = Gson()
        val json = gson.toJson(imageUploadList1)
        val sharedPrefs: SharedPreferences =
            mContext.getSharedPreferences(mySharedPref, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.putString("imageUploadListOffline", json)
        editor.apply()
        Log.d(
            "imageSW offline SAVE",
            "DONE, ${imageUploadList1.size} Last Image ===>> ${imageUploadList1.last()}"
        )
    }


    private fun clearSharedPref() {
        val sharedPrefs: SharedPreferences =
            mContext.getSharedPreferences("MyPrefsSW", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.clear()
        editor.apply()
    }

    fun renderUi() {

        if (currentImageList.isNotEmpty()) {
            Log.d(
                "imageSW currentImageList Last",
                " size: ${currentImageList.size}, ${currentImageList.last()}"
            )
        } else Log.d("imageSW currentImageList Last", " Empty")



        if (currentImageList.isNotEmpty() && currentImageList.size == 1) {
            rowSum = 1 // condition for 1st image capture
        }

        if (!isAutomaticID) {
            nextStepID = directionSelected
            _uiState.update { state ->
                state.copy(
                    nextStep = directionSelected
                )
            }

        } else {
            nextStepID = ""
            _uiState.update { state ->
                state.copy(
                    nextStep = ""
                )
            }
        }


        _uiState.update { state ->
            state.copy(
                isImageListEmpty = currentImageList.isEmpty()
            )
        }

        Log.d(
            "imageSW renderUI",
            "ArrowClicked: ${_uiState.value.directionArrowClicked}, isAutomaticID: $isAutomaticID, currentImageList.Size: ${currentImageList.size}"
        )
        if (_uiState.value.directionArrowClicked || isAutomaticID || currentImageList.size == 1) {
            _uiState.update { state ->
                state.copy(
                    enableCaptureBtn = true

                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    enableCaptureBtn = false
                )
            }
        }

        val guideResult = showDirectionResult()
        Log.d("imageSW guideResult", " $guideResult ")

        if (!isAutomaticID) {
            _uiState.update { state ->
                state.copy(
                    showArrowAllDirection = true
                )
            }
        }

        if (currentImageList.isNotEmpty() && !isAutomaticID) {
            _uiState.update { state ->
                state.copy(
                    showArrowAllDirection = true
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    showArrowAllDirection = false
                )
            }
        }

        if (guideResult.toString() == "left") {
            _uiState.update { state ->
                state.copy(
                    showLeftArrow = true,
                    showDownArrow = false,
                    showRightArrow = false
                )
            }
        } else if (guideResult.toString() == "down") {
            _uiState.update { state ->
                state.copy(
                    showLeftArrow = false,
                    showDownArrow = true,
                    showRightArrow = false
                )
            }
        } else if (guideResult.toString() == "right") {
            _uiState.update { state ->
                state.copy(
                    showLeftArrow = false,
                    showDownArrow = false,
                    showRightArrow = true
                )
            }
        } else if (guideResult == null) {
            _uiState.update { state ->
                state.copy(
                    showLeftArrow = false,
                    showDownArrow = false,
                    showRightArrow = false
                )
            }
        }

        if (currentImageList.isNotEmpty()) {
            doImageOverlay(guideResult)
        }
    }

    private fun doImageOverlay(guideResult: String?) {

        var direction = if (currentImageList.isNotEmpty() && currentImageList.size > 1) {
            currentImageList.last().direction
        } else directionSelected

        Log.d(
            "imageSW doImageOverlay",
            "direction: $direction ,directionSelected: $directionSelected, guideResult: $guideResult, currentImageList.Size: ${currentImageList.size}"
        )

        // for left right direction
        try {
            if ((direction.isNotEmpty() && (direction == "left" || direction == "right")) ||
                (directionSelected == "right" || directionSelected == "left") || (guideResult == "right" || guideResult == "left")
            ) {
                if (currentImageList.size == 1) {
                    _uiState.update { state ->
                        state.copy(
                            topOverlayImage = null
                        )
                    }

                }
                var index = currentImageList.size - currentImageList.last().row
                val sizeID = currentImageList.size
                val imageOverlapLR = currentImageList[index.toInt()].image

                if (imageOverlapLR.toString().isNotEmpty()) {
                    //Left Overlapping
                    if (currentImageList.last().direction == "right" || directionSelected == "right" || guideResult == "right") {
                        _uiState.update { state ->
                            state.copy(
                                rightOverlayImage = null,
                            )
                        }
                        try {
                            _uiState.update { state ->
                                state.copy(
                                    leftOverlayImage = imageOverlapLR
                                )
                            }
                        } catch (e: NoSuchElementException) {
                            _uiState.update { state ->
                                state.copy(
                                    leftOverlayImage = null
                                )
                            }
                        }

                    } else if (currentImageList.last().direction == "left" || directionSelected == "left" || guideResult == "left") {
                        // right Overlapping
                        _uiState.update { state ->
                            state.copy(
                                leftOverlayImage = null,
                            )
                        }
                        try {
                            _uiState.update { state ->
                                state.copy(
                                    rightOverlayImage = imageOverlapLR
                                )
                            }
                        } catch (e: NoSuchElementException) {
                            _uiState.update { state ->
                                state.copy(
                                    rightOverlayImage = null
                                )
                            }
                        }

                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            leftOverlayImage = null
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            rightOverlayImage = null
                        )
                    }

                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        leftOverlayImage = null
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        rightOverlayImage = null
                    )
                }
            }
        } catch (e: ClassCastException) {
            _uiState.update { state ->
                state.copy(
                    rightOverlayImage = null
                )
            }
            _uiState.update { state ->
                state.copy(
                    leftOverlayImage = null
                )
            }
        }


        // Top Overlapping
        try {
            if (directionSelected == "down" || guideResult == "down") {
                try {
                    if (currentImageList.size == 1) {
                        _uiState.update { state ->
                            state.copy(
                                rightOverlayImage = null
                            )
                        }

                        _uiState.update { state ->
                            state.copy(
                                leftOverlayImage = null
                            )
                        }
                    }
                    _uiState.update { state ->
                        state.copy(
                            topOverlayImage = currentImageList.last().image
                        )
                    }
                } catch (e: NoSuchElementException) {
                    _uiState.update { state ->
                        state.copy(
                            topOverlayImage = null
                        )
                    }
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        topOverlayImage = null
                    )
                }
            }

        } catch (e: ClassCastException) {
            _uiState.update { state ->
                state.copy(
                    topOverlayImage = null
                )
            }
        }

    }

    private fun getOverlapArray(): Array<Any> {
        // Overlap Array Work
        // overlapArray Format  [isTop, isLeft, isRight, top_overlap_value, left_overlap_value, right_overlap_value]

        if (uiState.value.topOverlayImage != null) {
            overlapArray = if (uiState.value.leftOverlayImage != null) {
                arrayOf(1, 1, 0, overlapBE, overlapBE, 0) // Top + Left
            } else if (uiState.value.rightOverlayImage != null) {
                arrayOf(1, 0, 1, overlapBE, 0, overlapBE) // Top + Right
            } else {
                arrayOf(1, 0, 0, overlapBE, 0, 0) // Top Only
            }
        } else if (uiState.value.leftOverlayImage != null) {
            overlapArray = arrayOf(0, 1, 0, 0, overlapBE, 0) // Only Left
        } else if (uiState.value.rightOverlayImage != null) {
            overlapArray = arrayOf(0, 0, 1, 0, 0, overlapBE) // only Right
        } else {
            overlapArray = arrayOf(0, 0, 0, 0, 0, 0) // No One for 1st image

        }
        return overlapArray
    }


    fun leftArrowClicked(context: Context) {
        directionSelected = "left"
        setArrowClicked()
        renderUi() //leftArrowClicked
    }

    fun rightArrowClicked(context: Context) {
        Log.d("rightArrow", "clicked")
        directionSelected = "right"
        setArrowClicked()
        renderUi() //rightArrowClicked
    }

    fun bottomArrowClicked(context: Context) {
        directionSelected = "down"
        setArrowClicked()
        renderUi() //bottomArrowClicked

    }

    private fun checkSetAutomatic(): Boolean {
        var isAuto1 = false
        if (currentImageList.isNotEmpty() && currentImageList.size >= 2) {
            // for Left Right overlapping only
            if (currentImageList[1].direction == "right" || currentImageList[1].direction == "left") {
                isAuto1 = true
                lastDirection = currentImageList[1].direction
            }

            if (currentImageList.last().stepsTaken.isNotEmpty() && currentImageList.last().stepsTaken[1] == "down"
                && (currentImageList.last().stepsTaken.last() == "right" || currentImageList.last().stepsTaken.last() == "left")
            ) {
                isAuto1 = true
                lastDirection = currentImageList.last().stepsTaken.last()
            }

        } else isAuto1 = false

        return isAuto1
    }

    private fun showDirectionResult(): String? {
        var result: String = ""
        val isAutomatic = isAutomaticID
        val row = if (currentImageList.isNotEmpty()) currentImageList.last().row.toInt() else rowSum
        var direction = if (currentImageList.isNotEmpty() && currentImageList.size > 1) {
            currentImageList.last().direction
        } else directionSelected
        Log.d(
            "imageSW showDirectionResult",
            "isAutomatic: $isAutomatic,row: $row, direction: $direction , " +
                    "List.Size: ${currentImageList.size}, stepsTaken.size: ${stepsTakenID.size}"
        )

        result = if (currentImageList.isNotEmpty()) {
            if (stepsTakenID.isNotEmpty() && stepsTakenID.size >= 2) {
                val nextStepTaken = stepsTakenID[1]
                if (nextStepTaken == "down") {
                    // Work for DOWN direction
                    if (currentImageList.size % row == 0) direction else "down"

                } else direction // work for LEFT RIGHT Direction

            } else direction
        } else ""

        Log.d("imageSW resultSDR", "$result")
        directionForOverlap = result


        return if (isAutomatic && currentImageList.size > 0) result
        else null
    }


    fun dataFromOpenCameraEvent(
        context: Context,
        mMode: String,
        mOverlapBE: Float,
        mUploadParam: String,
        mResolution: String,
        mReferenceUrl: String,
        mIsBlurFeature: String,
        mIsCropFeature: String,
        mUploadFrom: String
    ) {
        mContext = context
        mode = mMode
        overlapBE = mOverlapBE / 100  //TODO: enable overlap percent for all overlap images
        upload_param = mUploadParam
        resolution = mResolution
        referenceUrl = mReferenceUrl
        isBlurFeature = mIsBlurFeature
        isCropFeature = mIsCropFeature
        uploadFrom = mUploadFrom

        if (currentImageList.size > 0) {
            renderUi() // dataFromOpenCameraEvent
        } else {
            directionSelected = ""
            rowSum = 0
            isAutomaticID = false

        }
    }

    fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return file.delete()
    }

    fun deleteLastCapturedImage() {
        Log.d("imageSW delete ", "before Size1: ${imageCapturedListLive.value?.size}")

        currentImageList.removeLast()
        imageCapturedListLive.value = currentImageList
        directionSelected = ""
        if(currentImageList.isNotEmpty()) handleDeletedImage()
        _previewFlow.update { currentImageList.size }

        Log.d("imageSW delete", "after Size2: ${currentImageList.size}")

        if (imageCapturedListLive.value?.size == 0 || currentImageList.size == 0) {
            discardAllImages() // delete size =0
            return
        }


    }

    fun discardAllImages() {
        currentImageList.clear()
        imageCapturedListLive.value?.clear()

        resetUIState()
        resetAllData() // discardAllImages
    }

    fun showLoader() {
        Log.d("imageSW loader", "show called")
        _uiState.update { state ->
            state.copy(
                showLoader = true
            )
        }
    }

    fun hideLoader() {
        Log.d("imageSW loader", "hide called")
        _uiState.update { state ->
            state.copy(
                showLoader = false
            )
        }
    }


    fun setArrowClicked() {
        _uiState.update { state ->
            state.copy(
                directionArrowClicked = true
            )
        }
    }

    fun resetArrowClicked() {
        _uiState.update { state ->
            state.copy(
                directionArrowClicked = false
            )
        }
    }

    fun resetUIState() {
        _uiState.update { state ->
            state.copy(
                null, null, null, false,
                false, false, false, true,
                false, "", "", true
            )
        }
    }

    fun resetAllData() {
        directionID = ""
        directionSelected = ""
        directionForOverlap = ""
        rowSum = 0
        rowID = 0.0
        isAutomaticID = false
        stepsTakenID = ArrayList<String>()
        nextStepID = ""

    }

    fun resetOverlapImage() {
        directionSelected = ""
        _uiState.update { state ->
            state.copy(
                topOverlayImage = null
            )
        }

        _uiState.update { state ->
            state.copy(
                leftOverlayImage = null
            )
        }

        _uiState.update { state ->
            state.copy(
                rightOverlayImage = null
            )
        }
    }

    // Function to get the device model name ID
    fun getDeviceModel(): String {
        return Build.MANUFACTURER + "_" + Build.DEVICE
    }

}

data class CameraUiState(
    val rightOverlayImage: Bitmap? = null,
    val leftOverlayImage: Bitmap? = null,
    val topOverlayImage: Bitmap? = null,
    val showArrowAllDirection: Boolean = false,
    val showLeftArrow: Boolean = false,
    val showDownArrow: Boolean = false,
    val showRightArrow: Boolean = false,
    val isImageListEmpty: Boolean = true,
    val showLoader: Boolean = false,
    val nextStep: String = "",
    val directionSelected: String = "",
    val directionArrowClicked: Boolean = false,
    val enableCaptureBtn: Boolean = true,
)
