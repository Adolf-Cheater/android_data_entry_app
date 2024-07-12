package com.example.data_entry_android

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class SlideToActionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thumbPosition = 0f
    private var lastTouchX = 0f
    private var slideListener: (() -> Unit)? = null
    private val textBounds = Rect()

    init {
        paint.color = ContextCompat.getColor(context, R.color.slider_background)
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.CENTER
        arrowPaint.color = Color.WHITE
        arrowPaint.style = Paint.Style.STROKE
        arrowPaint.strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        // Draw background with square sides and rounded corners
        val cornerRadius = height / 2f
        val path = Path().apply {
            moveTo(cornerRadius, 0f)
            lineTo(width - cornerRadius, 0f)
            arcTo(width - 2 * cornerRadius, 0f, width.toFloat(), 2 * cornerRadius, -90f, 90f, false)
            lineTo(width.toFloat(), height - cornerRadius)
            arcTo(width - 2 * cornerRadius, height - 2 * cornerRadius, width.toFloat(), height.toFloat(), 0f, 90f, false)
            lineTo(cornerRadius, height.toFloat())
            arcTo(0f, height - 2 * cornerRadius, 2 * cornerRadius, height.toFloat(), 90f, 90f, false)
            lineTo(0f, cornerRadius)
            arcTo(0f, 0f, 2 * cornerRadius, 2 * cornerRadius, 180f, 90f, false)
            close()
        }
        canvas.drawPath(path, paint)

        // Draw arrow
        val arrowSize = height / 3f
        val arrowX = thumbPosition + arrowSize / 2
        val arrowY = height / 2f
        canvas.drawLine(arrowX, arrowY - arrowSize / 2, arrowX + arrowSize / 2, arrowY, arrowPaint)
        canvas.drawLine(arrowX, arrowY + arrowSize / 2, arrowX + arrowSize / 2, arrowY, arrowPaint)

        // Draw text with fading effect
        val text = "Slide to get started"
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textX = width / 2f
        val textY = (height + textBounds.height()) / 2f

        val maxAlpha = 255
        val alpha = ((1 - thumbPosition / (width - height)) * maxAlpha).toInt().coerceIn(0, 255)
        textPaint.alpha = alpha

        canvas.drawText(text, textX, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = event.x - lastTouchX
                thumbPosition = (thumbPosition + delta).coerceIn(0f, width - height.toFloat())
                lastTouchX = event.x
                invalidate()
                if (thumbPosition >= width - height.toFloat()) {
                    slideListener?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (thumbPosition < width - height.toFloat()) {
                    ObjectAnimator.ofFloat(this, "thumbPosition", 0f).start()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setSlideListener(listener: () -> Unit) {
        slideListener = listener
    }

    private fun setThumbPosition(position: Float) {
        thumbPosition = position
        invalidate()
    }
}