package com.sj.camera_lib_android.ui
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.sj.camera_lib_android.utils.imageutils.ZoomingImage
import com.sj.camera_lib_android.R

class ImageDialog(context: Context, referenceUrl: String) : Dialog(context), View.OnClickListener {
   private val referenceUrl2 = referenceUrl
   private val context1 = context
    private lateinit var crossImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_image_design)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

        crossImg = findViewById(R.id.cross_iv)

        val zoomableImageView: ZoomingImage = findViewById(R.id.imgD_iv)

        Log.d("imageSW referenceUrl2"," $referenceUrl2")
        Glide.with(context1).load(referenceUrl2).into(zoomableImageView)

        crossImg.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.cross_iv) {
            dismiss()
        }
    }
}
