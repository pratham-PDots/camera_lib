package com.sj.camera_lib_android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class CustomBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rectanglePaint = Paint()
    private val ballPaint = Paint()
    private val textPaint = Paint()

    private var textValue = 0f
    private var value = 0f
    private var isHorizontal = true

    init {
        rectanglePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = 0xFFFFFFFF.toInt()
        }

        ballPaint.apply {
            style = Paint.Style.FILL
            color = 0xFFFF0000.toInt()
        }

        textPaint.apply {
            textSize = 30f
            color = 0xFFFFFFFF.toInt()
        }
    }

    fun setValue(newValue: Float) {
        value = newValue * .1f
        textValue = newValue
        invalidate()
    }

    fun setHorizontalMode(horizontal: Boolean) {
        isHorizontal = horizontal
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewGap = (20f * resources.displayMetrics.density + 0.5f).toInt()
        val width = width.toFloat() - viewGap
        val height = height.toFloat() - viewGap

        if (isHorizontal) {
            // Draw horizontal rounded rectangle with increased space inside
            val rectLeft = 20f
            val rectTop = 40f
            val rectRight = width - 20
            val rectBottom = height - 40
            val cornerRadius = 5f
            canvas.drawRoundRect(
                rectLeft,
                rectTop,
                rectRight,
                rectBottom,
                cornerRadius,
                cornerRadius,
                rectanglePaint
            )

            // Draw the ball
            val ballRadius = (rectBottom - rectTop) / 2
            val centerX = (rectRight + rectLeft) / 2
            val centerY = (rectBottom + rectTop) / 2
            val ballX = centerX + value * (width - 40) / 2

            // Calculate the minimum and maximum positions for the ball
            val minBallX = rectLeft + ballRadius
            val maxBallX = rectRight - ballRadius

            // Clamp the ballX value to stay within the limits
            val clampedBallX = ballX.coerceIn(minBallX, maxBallX)

            canvas.drawCircle(clampedBallX, centerY, ballRadius, getColor())
        } else {
            // Draw vertical rounded rectangle with increased space inside
            val rectLeft = 40f
            val rectTop = 20f
            val rectRight = width - 40
            val rectBottom = height - 20
            val cornerRadius = 5f
            canvas.drawRoundRect(
                rectLeft,
                rectTop,
                rectRight,
                rectBottom,
                cornerRadius,
                cornerRadius,
                rectanglePaint
            )

            // Draw the ball
            val ballRadius = (rectRight - rectLeft) / 2
            val centerX = (rectRight + rectLeft) / 2
            val centerY = (rectBottom + rectTop) / 2
            val ballY = centerY + value * (height - 40) / 2

            // Calculate the minimum and maximum positions for the ball
            val minBallY = rectTop + ballRadius
            val maxBallY = rectBottom - ballRadius

            // Clamp the ballY value to stay within the limits
            val clampedBallY = ballY.coerceIn(minBallY, maxBallY)

            canvas.drawCircle(centerX, clampedBallY, ballRadius, getColor())
        }

    }

    private fun getColor() : Paint {
        if((abs(textValue) > 6f)) {
            ballPaint.apply {
                style = Paint.Style.FILL
                color = 0xFFFF0000.toInt()
            }
        } else {
            ballPaint.apply {
                style = Paint.Style.FILL
                color = 0xFF00FF00.toInt()
            }
        }
        return ballPaint
    }
}