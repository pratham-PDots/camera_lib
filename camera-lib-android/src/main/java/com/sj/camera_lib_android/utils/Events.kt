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
    val NATIVE_AVAILABLE_WIDE_ANGLE = "native-available-wide-angle"
    val FAILED_TO_CHANGE_LANGUAGE = "native-language-change failure"
    val LEFT_ARROW_CLICKED = "native-left-arrow-clicked"
    val RIGHT_ARROW_CLICKED = "native-right-arrow-clicked"
    val DOWN_ARROW_CLICKED = "native-down-arrow-clicked"
    val SHOW_AUTOMATIC_ARROW = "native-show-automatic-arrow"
    val DELETE_CLICKED = "native-delete-clicked"
    val CROP_RETAKE = "native-crop-retake-pressed"
    val CROP_DONE = "native-crop-done-pressed"
    val BLUR_RETAKE = "native-blur-retake-pressed"
    val CROP_METHOD = "native-crop-method"
    val CROP_START = "native-crop-start"
    val BUCKET_NAME = "native-bucket-names"
    val BUCKET_CATCH_BLOCK = "native-bucket-catch-block"
    val LOGOUT = "native-logout"
    val UPLOAD_SERVICE_FAILURE = "native-upload-service-error"
    val INIT_SERVICE_BACKGROUND_FAILURE = "native-init-service-background"
    val IMAGE_SAVED = "native-image-saved"
}