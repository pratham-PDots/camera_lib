package com.sj.camera_lib_android.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View

class GridOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    //  private val paint = Paint().apply {
//    color = Color.WHITE
//    strokeWidth = 1f
//  }
    init {
        this.setWillNotDraw(false)
    }
    private val paint = Paint()


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

//    val numColumns = 3 // Number of grid columns
//    val numRows = 3 // Number of grid rows
//
//    val columnWidth = width / numColumns
//    val rowHeight = height / numRows
//
//    for (i in 1 until numColumns) {
//      val x = i * columnWidth.toFloat()
//      canvas?.drawLine(x, 0f, x, height.toFloat(), paint)
//    }
//
//    for (i in 1 until numRows) {
//      val y = i * rowHeight.toFloat()
//      canvas?.drawLine(0f, y, width.toFloat(), y, paint)
//    }
        val mDrawBounds: RectF
        val grid = true
        if (grid) {
            //  Find Screen size first
//      val metrics: DisplayMetrics = Resources.getSystem().displayMetrics
//      val screenWidth: Int = metrics.widthPixels
//      val screenHeight = metrics.heightPixels
            val screenWidth: Int = measuredWidth
            val screenHeight = measuredHeight

            //  Set paint options
            paint.isAntiAlias = true
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            paint.color = Color.argb(255, 255, 255, 255)
            canvas!!.drawLine(
                (screenWidth / 3 * 2).toFloat(), 0F,
                (screenWidth / 3 * 2).toFloat(), screenHeight.toFloat(), paint
            )
            canvas.drawLine(
                (screenWidth / 3).toFloat(), 0F, (screenWidth / 3).toFloat(),
                screenHeight.toFloat(), paint
            )
            canvas.drawLine(
                0F,
                (screenHeight / 3 * 2).toFloat(), screenWidth.toFloat(),
                (screenHeight / 3 * 2).toFloat(), paint
            )
            canvas.drawLine(
                0F, (screenHeight / 3).toFloat(),
                screenWidth.toFloat(), (screenHeight / 3).toFloat(), paint
            )
        }
    }
}
