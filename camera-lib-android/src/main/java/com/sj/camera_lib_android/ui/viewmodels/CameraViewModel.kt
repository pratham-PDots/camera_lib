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


class CameraViewModel() : ViewModel()  {

  // Expose screen UI state
  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
//  val currentImageList = arrayListOf<CaptureImageModel>()
//  val imageCapturedList: MutableLiveData<ArrayList<CaptureImageModel>> = MutableLiveData()

  var mode = ""
  var overlapBE = 0.2f
  var upload_param= ""
  var resolution = ""
  var referenceUrl = ""
  // val referenceUrl = "https://www.gstatic.com/webp/gallery/1.jpg"
  var isBlurFeature = ""
  var isCropFeature = ""
  var uploadFrom = ""

  private var directionSelected =""
  var directionForOverlap =""
  var rowSum:Int = 0

  val imageUploadList: MutableList<ImageUploadModel> = mutableListOf()

  val currentImageList = arrayListOf<ImageDetailsModel>()
  val imageCapturedListLive: MutableLiveData<ArrayList<ImageDetailsModel>> = MutableLiveData()



//  private val imageDetails:ImageDetails = ViewModelProvider.AndroidViewModelFactory.getInstance(Application()).create(ImageDetails::class.java)
  //ImageDetails
  private var directionID: String = ""
  private var isAutomaticID: Boolean = false
  private var rowID: Double = 0.0
  private var stepsTakenID: ArrayList<String> = ArrayList<String>()
  private var nextStepID: String =""



  private var positionMatrix: IntArray = intArrayOf(0,0)
  private var dimensionMatrix: IntArray = intArrayOf(0,0)
  private var overlapArray: Array<Any> = arrayOf(0,0,0,0.0f,0.0f,0.0f)

//  private var overlapArray: FloatArray = floatArrayOf(0f,0f,0f,0.0f,0.0f,0.0f)
  // Format  [isTop, isLeft, isRight, top_overlap_value, left_overlap_value, right_overlap_value]

  private var mySharedPref: String = "MyPrefsSW"
  private lateinit var mContext: Context



  fun handleClickedImage(
//    fileUri: Uri,
    bmp1: Bitmap,
    coordinatesCropped: Array<Int>,
    file1: File,
    captureTime: String,
    context: Context
  ){

    rowSum = if (currentImageList.isEmpty()){ // condition for 1st image capture
      1
    }else{
      currentImageList.last().row.toInt()
    }

    if (directionSelected.isNotEmpty() && directionSelected.equals("down",true)){
      rowSum = currentImageList.last().row.toInt() + 1
    }
    val zoomLevel = if (Common.zoomSelected != null) Common.zoomSelected else "0"


//    currentImageList.add(CaptureImageModel(bmp1,currentImageList.size,coordinatesCropped, file1, directionSelected,rowSum.toString()))


    // Save Details in class
    Log.d("imageSW imageDetails Saving","START ===>> directionSelected: $directionSelected")
    rowID = rowSum.toDouble()

    if (directionSelected.isNotEmpty() && !directionSelected.equals("down",true)) {
      directionID = directionSelected
    }

    // stepsTaken Work
    if (currentImageList.isNotEmpty() ){
       stepsTakenID = currentImageList.last().stepsTaken

      if (currentImageList.last().isAutomatic){
        stepsTakenID.add("")
      }else stepsTakenID.add(directionSelected)
    }else stepsTakenID.add("")

    // Position and Dimension Work
    if (currentImageList.isNotEmpty()) {
      if (currentImageList.last().stepsTaken.size > 1 && currentImageList.last().stepsTaken[1] == "down"){
        val sum = currentImageList.size % rowID.toInt()
        val col = currentImageList.size/rowID.toInt()

        positionMatrix = intArrayOf(sum,col)
        dimensionMatrix = intArrayOf(rowID.toInt(),col+1)

        Log.d("imageSW sumDown", " $sum, positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix")

      }else{
        val sum = currentImageList.size % rowID.toInt()
        positionMatrix = intArrayOf(sum,currentImageList.size)
        dimensionMatrix = intArrayOf(rowID.toInt(), currentImageList.size+1)

        Log.d("imageSW sumLR", " $sum, positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix")

      }
    }else {
      positionMatrix = intArrayOf(0,0)
      dimensionMatrix = intArrayOf(1,1)

      Log.d("imageSW sum 1stImg",  "positionMatrix: $positionMatrix, dimensionMatrix: $dimensionMatrix")
    }

    // getOverlapArray Work
    overlapArray = getOverlapArray()
    Log.d("imageSW overlapArray",overlapArray.contentToString())


    currentImageList.add(ImageDetailsModel(positionMatrix,dimensionMatrix, captureTime,zoomLevel, mode
      ,directionID, isAutomaticID,rowID,stepsTakenID,nextStepID, overlapArray.contentToString(),upload_param,
      ImageModel("$file1","image/png","$captureTime.png"), file1, bmp1, coordinatesCropped))

    Log.d("imageSW handle_img", " currentImageList.size: ${currentImageList.size} , rowSum: $rowSum")
    imageCapturedListLive.value = currentImageList
    Log.d("imageSW imageDetails Saving"," END")

    if (currentImageList.size > 1) directionSelected = ""
    // isAutomatic Process work
    isAutomaticID = if (currentImageList.isNotEmpty()){
      if (!currentImageList.last().isAutomatic){
        checkSetAutomatic()
      }else {
        currentImageList.last().isAutomatic
      }
    }else false
    Log.d("imageSW isAutomaticID" ,"$isAutomaticID")

    clearSharedPref()
    imageUploadList.clear()
//    resetAllData() // handleClickedImage
    renderUi() // handleClickedImage

  }

  fun uploadImages(context: Context) {

    val deviceName = getDeviceModel()
    val utils = Utils()
    if (utils.checkInternetConnection(mContext)) {
      clearSharedPref()
      imageUploadList.clear()

      if (currentImageList.isNotEmpty()){
      imageUploadList.addAll(currentImageList.map {imageDetails ->
        ImageUploadModel(
          // Map properties from ImageDetailsModel to ImageUploadModel
          imageDetails.position.contentToString(),
          imageDetails.dimension.contentToString(),"","", "${currentImageList.size}",
          imageDetails.appTimestamp,
          imageDetails.orientation,
          imageDetails.zoomLevel, "",
          imageDetails.croppedCoordinates.contentToString(), "${imageDetails.overlapPercent}",
          upload_param,"${imageDetails.file}","image/png","${imageDetails.appTimestamp}.png", imageDetails.file
        )

      })
      Log.d("imageSW uploadImages","${imageUploadList.size} Last Image ===>> ${imageUploadList.last()}")

      //Saving for offlineMode
      saveToSharedPref(imageUploadList)

        // Upload to Firebase or send List to 3rd Party
        // upload to Firebase
        // Call My Service for image Upload to firebase in the background
        val utils = Utils()
        if (utils.checkInternetConnection(context)) {

          val projectId = JSONObject(upload_param).get("project_id")

          if (imageUploadList.isNotEmpty()) {
            // Start Service
            val intent = Intent(context, MyServices()::class.java) // image Upload from gallery
            intent.putParcelableArrayListExtra("mediaList", ArrayList(imageUploadList))
            intent.putExtra("deviceName",deviceName)
            intent.putExtra("project_id", projectId.toString())
            context.startService(intent)

          }else{
            Log.e("imageSW startFbService","imageUploadList is EMPTY/ NULL")
            Toast.makeText(context,"imageUploadList isEmpty", Toast.LENGTH_SHORT).show()
          }
        } else {
          Toast.makeText(context, "Opps! No Internet\nPlease Connect to Internet!", Toast.LENGTH_SHORT).show()
        }

//        uploadToFirebase(mContext, imageUploadList.last(), firebaseDBReference1, imageUploadList) //uploadImages

        //        Broadcast only when uploadFrom from 3rd party

        if (uploadFrom.isNotEmpty() && uploadFrom != "Shelfwatch") {
          Log.d("imageSW UploadTo", "3rd party")
        val intent = Intent("DataSaved")
        intent.putExtra("status","Done")
        intent.putParcelableArrayListExtra("imageListSaved",ArrayList(imageUploadList))
        intent.putExtra("deviceName",getDeviceModel())
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
      }


    }else{
      Common.showToast(mContext,"Image list is empty now!")
    }
    } else {
      hideLoader()
      Toast.makeText(mContext, "Opps! No Internet\nPlease Connect to Internet", Toast.LENGTH_SHORT).show()
    }

  }

/*
// Firebase RealtimeDB
  private fun uploadToFirebase(
    context: Context,
    imageUploadModel1: ImageUploadModel,
    firebaseDBReference1: DatabaseReference,
    imageUploadList: MutableList<ImageUploadModel>
  ) {
    Log.d("imageSW uploadToFirebase", ", ListSize: ${imageUploadList.size}, firebaseDBReference1: $firebaseDBReference1")

    try {
      firebaseDBReference1.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
// Get Post object and use the values to update the UI
//        firebaseDBReference1.setValue(imageUploadModel1)
          firebaseDBReference1.setValue(imageUploadList)

          Log.d("imageSW Firebase ", "data added , Size: ${imageUploadList.size}")

          Toast.makeText(context, "Images Uploaded Successfully", Toast.LENGTH_SHORT).show()
          hideLoader()

        }

        override fun onCancelled(error: DatabaseError) {
          Log.d("imageSW Firebase ", "Fail to add data:$error , Size: ${imageUploadList.size}")

          Toast.makeText(context, "Fail to add data $error", Toast.LENGTH_SHORT).show()
          hideLoader()
        }
      })
    }catch (e: Exception){
      Log.e("imageSW uploadToFirebase","Exception: ${e.toString()}")
    }
  }
*/

  private fun saveToSharedPref(imageUploadList1: MutableList<ImageUploadModel>) {
    // Convert the list to a JSON string
    val gson = Gson()
    val json = gson.toJson(imageUploadList1)
    val sharedPrefs: SharedPreferences = mContext.getSharedPreferences(mySharedPref, Context.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPrefs.edit()
    editor.putString("imageUploadListOffline", json)
    editor.apply()
    Log.d("imageSW offline SAVE","DONE, ${imageUploadList1.size} Last Image ===>> ${imageUploadList1.last()}")
  }


  fun getOfflineImageList(context: Context): MutableList<ImageUploadModel> {
    Log.d("imageSW OfflineList","Fetching Data start")
    var imageUploadOfflineList: MutableList<ImageUploadModel> = mutableListOf()

    //Retrieve the list from SharedPreferences
    val sharedPrefs: SharedPreferences = context.getSharedPreferences("MyPrefsSW", Context.MODE_PRIVATE)
    val json = sharedPrefs.getString("imageUploadListOffline", null)

    if (json != null) {
      val gson = Gson()
      val type = object : TypeToken<MutableList<ImageUploadModel>>() {}.type
      imageUploadOfflineList = gson.fromJson(json, type) ?: mutableListOf()
    }

    return imageUploadOfflineList
  }

  private fun clearSharedPref(){
    val sharedPrefs: SharedPreferences = mContext.getSharedPreferences("MyPrefsSW", Context.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPrefs.edit()
    editor.clear()
    editor.apply()
  }

  fun renderUi(){

    if (currentImageList.isNotEmpty()){
      Log.d("imageSW currentImageList Last"," size: ${currentImageList.size}, ${currentImageList.last()}")
/*    Log.d("imageSW currentImageList Last", "size: ${currentImageList.size} ==>>> D= ${currentImageList.last().direction}," +
            " isAuto= ${currentImageList.last().isAutomatic}, row: ${currentImageList.last().row}, stepsTaken: ${currentImageList.last().stepsTaken} " +
            "nextStep: ${currentImageList.last().nextStep}, overlap%: ${currentImageList.last().overlapPercent}, " +
            "previewFile: ${currentImageList.last().file}, previewImage: ${currentImageList.last().image}, coordinatesCropped: ${currentImageList.last().croppedCoordinates.contentToString()}," +
        " fileLeftOvlp: ${currentImageList.last().fileLeftOvlp},  leftOverlapBtmp: ${currentImageList.last().leftOverlapBtmp}, "+
        "fileRightOvlp: ${currentImageList.last().fileRightOvlp},rightOverlapBtmp: ${currentImageList.last().rightOverlapBtmp}," +
        " fileTopOvlp: ${currentImageList.last().fileTopOvlp}, topOverlapBtmp: ${currentImageList.last().topOverlapBtmp}, "
    )*/
    }else Log.d("imageSW currentImageList Last"," Empty")



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

    }else{
      nextStepID = ""
      _uiState.update { state ->
        state.copy(
          nextStep = ""
        )
      }
    }


//    Log.d("imageSW renderUi", " state_received: $currentData")
    _uiState.update { state->
      state.copy(
        isImageListEmpty = currentImageList.isEmpty()
      )
    }

    Log.d("imageSW renderUI","ArrowClicked: ${_uiState.value.directionArrowClicked}, isAutomaticID: $isAutomaticID, currentImageList.Size: ${currentImageList.size}")
    if( _uiState.value.directionArrowClicked || isAutomaticID || currentImageList.size == 1){
      _uiState.update { state->
        state.copy(
          enableCaptureBtn = true

        )
      }
    } else {
      _uiState.update { state->
        state.copy(
          enableCaptureBtn = false
        )
      }
    }

//    val guideResult1 = showGuide()
    val guideResult = showDirectionResult()
    Log.d("imageSW guideResult"," $guideResult ")

//    Log.d("imageSW","preViewList: ${preViewList.iterator()} , overlapPercentages: ${overlapPercentages.iterator()} ")
    if(!isAutomaticID){
      _uiState.update { state->
        state.copy(
          showArrowAllDirection = true
        )
      }
    }

    if(currentImageList.isNotEmpty() && !isAutomaticID){
      _uiState.update { state->
        state.copy(
          showArrowAllDirection = true
        )
      }
    }else {
      _uiState.update { state->
        state.copy(
          showArrowAllDirection = false
        )
      }
    }

    if( guideResult.toString() == "left"){
      _uiState.update { state->
        state.copy(
          showLeftArrow = true,
          showDownArrow = false,
          showRightArrow = false
        )
      }
    }
    else if( guideResult.toString() == "down"){
      _uiState.update { state->
        state.copy(
          showLeftArrow = false,
          showDownArrow = true,
          showRightArrow = false
        )
      }
    }
    else if( guideResult.toString() == "right"){
      _uiState.update { state->
        state.copy(
          showLeftArrow = false,
          showDownArrow = false,
          showRightArrow = true
        )
      }
    }
    else if( guideResult == null){
      _uiState.update { state->
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

  private fun getImageDetails() {
//    directionID = currentImageList.last().direction
    isAutomaticID = currentImageList.last().isAutomatic
    rowID = currentImageList.last().row
    stepsTakenID = currentImageList[currentImageList.size-1].stepsTaken

    Log.d("imageSW getImageDetails", "isAutomaticID: $isAutomaticID, rowID: $rowID, stepsTakenID Size: ${stepsTakenID.size}")
  }

  private fun doImageOverlay(guideResult: String?) {

    var direction = if (currentImageList.isNotEmpty() && currentImageList.size > 1){
      currentImageList.last().direction
    }else directionSelected

    Log.d("imageSW doImageOverlay", "direction: $direction ,directionSelected: $directionSelected, guideResult: $guideResult, currentImageList.Size: ${currentImageList.size}")

    // for left right direction
    try {
      if ((direction.isNotEmpty() && (direction == "left" || direction == "right")) ||
        (directionSelected == "right" || directionSelected == "left") || (guideResult == "right" || guideResult == "left")) {
        if (currentImageList.size == 1){
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
      }
      else {
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
    }
    catch ( e: ClassCastException){
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
      if (directionSelected == "down" || guideResult == "down"){
        try {
          if (currentImageList.size == 1){
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
      }else {
        _uiState.update { state ->
          state.copy(
            topOverlayImage = null
          )
        }
      }

    }
    catch ( e: ClassCastException){
//      Log.d("overlay_image_top","topoverlay excption")
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

    if (uiState.value.topOverlayImage != null){
      overlapArray = if (uiState.value.leftOverlayImage != null){
        arrayOf(1,1,0, overlapBE, overlapBE,0) // Top + Left
      }else if (uiState.value.rightOverlayImage != null){
        arrayOf(1,0,1,overlapBE, 0,overlapBE) // Top + Right
      }else{
        arrayOf(1,0,0, overlapBE,0,0) // Top Only
      }
    }else if (uiState.value.leftOverlayImage != null){
      overlapArray = arrayOf(0,1,0,0, overlapBE,0) // Only Left
    }else if (uiState.value.rightOverlayImage != null){
      overlapArray = arrayOf(0,0,1,0,0,overlapBE) // only Right
    }else{
      overlapArray = arrayOf(0,0,0,0,0,0) // No One for 1st image

    }
    return overlapArray
  }


  fun leftArrowClicked(context: Context){
    directionSelected = "left"
    setArrowClicked()
    renderUi() //leftArrowClicked
  }

  fun rightArrowClicked(context: Context){
    Log.d("rightArrow","clicked")
    directionSelected = "right"
    setArrowClicked()
    renderUi() //rightArrowClicked
  }

  fun bottomArrowClicked(context: Context){
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
      }

      if (currentImageList.last().stepsTaken.isNotEmpty() && currentImageList.last().stepsTaken[1] == "down"
        && (currentImageList.last().stepsTaken.last() == "right" || currentImageList.last().stepsTaken.last() == "left")){
        isAuto1 = true
      }

    } else isAuto1 = false

    return isAuto1
  }
  private fun showDirectionResult(): String?{
    var result: String = ""
    val isAutomatic = isAutomaticID
    val row = if (currentImageList.isNotEmpty()) currentImageList.last().row.toInt() else rowSum
    var direction = if (currentImageList.isNotEmpty() && currentImageList.size > 1){
      currentImageList.last().direction
    }else directionSelected
    Log.d("imageSW showDirectionResult", "isAutomatic: $isAutomatic,row: $row, direction: $direction , " +
            "List.Size: ${currentImageList.size}, stepsTaken.size: ${stepsTakenID.size}")

    result = if (currentImageList.isNotEmpty()) {
      if (stepsTakenID.isNotEmpty() && stepsTakenID.size >= 2) {
        val nextStepTaken = stepsTakenID[1]
        if (nextStepTaken == "down") {
          // Work for DOWN direction
         /* val index = currentImageList.size - currentImageList.last().row
          if (index.toInt() == row) direction  // Overlapping only in one direction
          else "down"   // Overlapping in both

*/
          if (currentImageList.size % row == 0) direction else "down"

        } else direction // work for LEFT RIGHT Direction

      }else direction
    }else ""

    Log.d("imageSW resultSDR", "$result")
    directionForOverlap = result

//    result = if (currentImageList.size > 1 && currentImageList.size % row == 0) direction else "down"

    return if(isAutomatic && currentImageList.size > 0) result
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
  ){
//    Log.d("dataFromOpenCam: ", mMode)
//    Log.d("dataFromOpenCam: ", "$mMode, ${mOverlapBE.toString()}, $mUploadParam")
    mContext = context
    mode = mMode
    overlapBE = mOverlapBE/100  //TODO: enable overlap percent for all overlap images
    upload_param = mUploadParam
    resolution = mResolution
    referenceUrl = mReferenceUrl
    isBlurFeature = mIsBlurFeature
    isCropFeature = mIsCropFeature
    uploadFrom = mUploadFrom
//    Log.d("dataFromOpenCam1: ", "$mode, ${overlapBE.toString()}, $upload_param")

    /*currentData["left"] = "Left Value"
    currentData["right"] = "Right Value"
    currentData["top"] = "Top Value"
    currentData["direction"] = "Direction Value"
    currentData["nextStep"] = "Next Step Value"*/

    if (currentImageList.size > 0) {
      renderUi() // dataFromOpenCameraEvent
    } else{
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
    Log.d("imageSW delete ","before Size1: ${imageCapturedListLive.value?.size}")

    currentImageList.removeLast()
    imageCapturedListLive.value = currentImageList
    resetArrowClicked()
    if (currentImageList.isNotEmpty()) getImageDetails()

    Log.d("imageSW delete","after Size2: ${currentImageList.size}")

    if (imageCapturedListLive.value?.size == 0 || currentImageList.size == 0){
      discardAllImages() // delete size =0
      return
    }


  }

  fun discardAllImages(){
//    currentData = defaultData
    currentImageList.clear()
    imageCapturedListLive.value?.clear()

    resetUIState()
    resetAllData() // discardAllImages
  }

  fun showLoader(){
    Log.d("imageSW loader","show called")
    _uiState.update { state->
      state.copy(
        showLoader = true
      )
    }
  }

  fun hideLoader(){
    Log.d("imageSW loader","hide called")
    _uiState.update { state->
      state.copy(
        showLoader = false
      )
    }
  }


  fun setArrowClicked(){
    _uiState.update { state ->
      state.copy(
        directionArrowClicked = true
      )
    }
  }

  fun resetArrowClicked(){
    _uiState.update { state ->
      state.copy(
        directionArrowClicked = false
      )
    }
  }

  fun resetUIState(){
    _uiState.update {state->
      state.copy(
        null,null,null,false,
        false, false, false,true,
        false,"","",true
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
    Common.zoomSelected = "0"

  }

 fun resetOverlapImage(){
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

  /*fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }


  fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it])
    {
      is JSONArray ->
      {
        val map = (0 until value.length()).associate { intArrayOf(it.toString(), value[it]) }
        JSONObject(map).toMap().values.toList()
      }
      is JSONObject -> value.toMap()
      JSONObject.NULL -> null
      else            -> value
    }
  }
*/

  // Function to get the device model name ID
  fun getDeviceModel(): String {
    return Build.MANUFACTURER+"_"+Build.DEVICE
  }

}


private operator fun Any?.set(s: String, value: Array<Int>) {

}

data class CameraUiState(
  val rightOverlayImage: Bitmap? = null,
  val leftOverlayImage : Bitmap? = null,
  val topOverlayImage : Bitmap? = null,
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
