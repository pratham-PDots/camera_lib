package com.sj.camera_lib_android.utils

object Events {
    val CAMERA_LAUNCHED_EVENT = "native-camera-launched"
    val CAPTURE_BUTTON_PRESSED = "native-capture-button-pressed"
    val CAPTURE_FAILED = "native-image-capture-failed"
    val CAPTURE_SUCCESS = "native-image-capture-success"
    val IMAGE_PROXY_CONVERSION = "native-image-proxy-to-bitmap"
    val ROTATE_IMAGE = "native-image-rotated"
    val RESIZE_IMAGE = "native-image-resized"
    val IMAGE_BLUR = "native-image-blur"
    val IMAGE_CROPPED = "native-image-cropped"
    val IMAGE_CROPPED_PREVIEW = "native-image-cropped-preview"
    val UPLOAD_BUTTON_PRESSED = "native-upload-button-pressed"
    val UPLOAD_BUTTON_PRESSED_PREVIEW = "native-upload-button-pressed-preview"
    val IMAGE_UPLOAD_SUCESS = "native-image-upload-success"
    val UPLOADED_IMAGE_METADATA = "native-uploaded-image-metadata"
    val IMAGE_UPLOAD_FAILURE = "native-image-failure"
    val DELETE_UPLOADED_FILE = "native-delete-uploaded-image"
    val CROSS_CLICK = "native-cross-clicked"
    val NATIVE_PARAMS = "native-sdk-params"
    val FAILED_TO_CHANGE_LANGUAGE = "native-language-change failure"
}