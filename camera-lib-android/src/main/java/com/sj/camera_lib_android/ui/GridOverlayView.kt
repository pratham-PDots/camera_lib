package com.sj.camera_lib_android.ui
/**
 * @author Saurabh Kumar 11 September 2023
 * **/

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GridOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    init {
        this.setWillNotDraw(false)
    }
    private val paint = Paint()


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val grid = true
        if (grid) {
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
