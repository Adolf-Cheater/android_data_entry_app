// CropView.kt
package com.example.data_entry_android

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val paint = Paint()
    private val cropRect = RectF()
    private var activeCorner: Int = -1

    private val cornerSize = 50f
    private val minCropSize = 100f

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        cropRect.set(0f, 0f, width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { bmp ->
            canvas.drawBitmap(bmp, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        }

        // Draw semi-transparent overlay
        paint.color = Color.argb(128, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, paint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), paint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, paint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, paint)

        // Draw crop rectangle
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(cropRect, paint)

        // Draw corner handles
        paint.style = Paint.Style.FILL
        canvas.drawRect(cropRect.left - cornerSize / 2, cropRect.top - cornerSize / 2, cropRect.left + cornerSize / 2, cropRect.top + cornerSize / 2, paint)
        canvas.drawRect(cropRect.right - cornerSize / 2, cropRect.top - cornerSize / 2, cropRect.right + cornerSize / 2, cropRect.top + cornerSize / 2, paint)
        canvas.drawRect(cropRect.left - cornerSize / 2, cropRect.bottom - cornerSize / 2, cropRect.left + cornerSize / 2, cropRect.bottom + cornerSize / 2, paint)
        canvas.drawRect(cropRect.right - cornerSize / 2, cropRect.bottom - cornerSize / 2, cropRect.right + cornerSize / 2, cropRect.bottom + cornerSize / 2, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeCorner = getActiveCorner(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeCorner != -1) {
                    updateCropRect(activeCorner, event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                activeCorner = -1
            }
        }
        return true
    }

    private fun getActiveCorner(x: Float, y: Float): Int {
        val corners = arrayOf(
            Pair(cropRect.left, cropRect.top),
            Pair(cropRect.right, cropRect.top),
            Pair(cropRect.left, cropRect.bottom),
            Pair(cropRect.right, cropRect.bottom)
        )

        for (i in corners.indices) {
            if (Math.abs(x - corners[i].first) < cornerSize && Math.abs(y - corners[i].second) < cornerSize) {
                return i
            }
        }
        return -1
    }

    private fun updateCropRect(corner: Int, x: Float, y: Float) {
        when (corner) {
            0 -> { // Top-left
                cropRect.left = x.coerceIn(0f, cropRect.right - minCropSize)
                cropRect.top = y.coerceIn(0f, cropRect.bottom - minCropSize)
            }
            1 -> { // Top-right
                cropRect.right = x.coerceIn(cropRect.left + minCropSize, width.toFloat())
                cropRect.top = y.coerceIn(0f, cropRect.bottom - minCropSize)
            }
            2 -> { // Bottom-left
                cropRect.left = x.coerceIn(0f, cropRect.right - minCropSize)
                cropRect.bottom = y.coerceIn(cropRect.top + minCropSize, height.toFloat())
            }
            3 -> { // Bottom-right
                cropRect.right = x.coerceIn(cropRect.left + minCropSize, width.toFloat())
                cropRect.bottom = y.coerceIn(cropRect.top + minCropSize, height.toFloat())
            }
        }
    }

    fun getCroppedBitmap(): Bitmap? {
        bitmap?.let { bmp ->
            val scaledRect = RectF(
                cropRect.left / width * bmp.width,
                cropRect.top / height * bmp.height,
                cropRect.right / width * bmp.width,
                cropRect.bottom / height * bmp.height
            )
            return Bitmap.createBitmap(
                bmp,
                scaledRect.left.toInt(),
                scaledRect.top.toInt(),
                scaledRect.width().toInt(),
                scaledRect.height().toInt()
            )
        }
        return null
    }
}