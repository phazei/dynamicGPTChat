package com.phazei.taginputview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics

class TagInputUnderlineDrawable(
    context: Context,
    private val backgroundColor: Int,
    private val focusedColor: Int
) : Drawable() {

    private val defaultPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val focusedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val defaultColor = context.getColor(R.color.default_underline_color)
    private var lineStart = bounds.width().toFloat() / 2
    private var lineEnd = bounds.width().toFloat() / 2

    init {

        defaultPaint.strokeWidth = 2.dpToPx(context).toFloat()

        focusedPaint.color = focusedColor
        focusedPaint.strokeWidth = 4.dpToPx(context).toFloat()

    }

    override fun draw(canvas: Canvas) {
        // Draw the background color
        defaultPaint.color = backgroundColor
        // canvas.drawRect(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), defaultPaint)

        // Draw the default underline
        defaultPaint.color = defaultColor
        canvas.drawLine(0f, bounds.bottom.toFloat(), bounds.width().toFloat(), bounds.bottom.toFloat(), defaultPaint)

        // Draw the focused underline
        canvas.drawLine(lineStart, bounds.bottom.toFloat(), lineEnd, bounds.bottom.toFloat(), focusedPaint)

    }

    override fun setAlpha(alpha: Int) {
        defaultPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    fun animateFocus(hasFocus: Boolean) {
        val animator = if (hasFocus) {
            ValueAnimator.ofFloat(0f, 1f)
        } else {
            ValueAnimator.ofFloat(1f, 0f)
        }

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            lineStart = bounds.width() * (0.5f - value * 0.5f)
            lineEnd = bounds.width() * (0.5f + value * 0.5f)
            invalidateSelf()
        }

        animator.duration = 250
        animator.start()
    }

    private fun Int.dpToPx(context: Context): Int {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        return (this * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }

}
