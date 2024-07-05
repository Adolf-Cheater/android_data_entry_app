// CropView.kt
package com.example.data_entry_android

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
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

    fun updateGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val exclusionRects = mutableListOf<Rect>()
            val handleSize = 100 // Adjust based on your actual handle size

            // Left edge
            exclusionRects.add(Rect(cropRect.left.toInt() - handleSize, cropRect.top.toInt() - handleSize,
                cropRect.left.toInt() + handleSize, cropRect.bottom.toInt() + handleSize))
            // Right edge
            exclusionRects.add(Rect(cropRect.right.toInt() - handleSize, cropRect.top.toInt() - handleSize,
                cropRect.right.toInt() + handleSize, cropRect.bottom.toInt() + handleSize))
            // Top edge
            exclusionRects.add(Rect(cropRect.left.toInt() - handleSize, cropRect.top.toInt() - handleSize,
                cropRect.right.toInt() + handleSize, cropRect.top.toInt() + handleSize))
            // Bottom edge
            exclusionRects.add(Rect(cropRect.left.toInt() - handleSize, cropRect.bottom.toInt() - handleSize,
                cropRect.right.toInt() + handleSize, cropRect.bottom.toInt() + handleSize))

            systemGestureExclusionRects = exclusionRects
        }
    }

    private fun initializeCropRect() {
        val padding = 50f // Adjust this value to control how far from the edges the crop rect starts
        val initialWidth = (width * 0.8f).coerceAtMost(height * 0.8f) // 80% of the smaller dimension
        val initialHeight = initialWidth // For a square crop, or adjust as needed

        val left = (width - initialWidth) / 2
        val top = (height - initialHeight) / 2
        val right = left + initialWidth
        val bottom = top + initialHeight

        cropRect.set(
            left.coerceIn(padding, width - padding),
            top.coerceIn(padding, height - padding),
            right.coerceIn(padding, width - padding),
            bottom.coerceIn(padding, height - padding)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initializeCropRect()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) {
            Log.e("CropView", "Invalid view dimensions: ${width}x${height}")
            return
        }
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
        Log.d("CropView", "Updating crop rect: corner=$corner, x=$x, y=$y")
        Log.d("CropView", "Current cropRect: $cropRect")
        Log.d("CropView", "View dimensions: ${width}x${height}")
        val viewWidth = width.toFloat().coerceAtLeast(1f)
        val viewHeight = height.toFloat().coerceAtLeast(1f)

        when (corner) {
            0 -> { // Top-left
                cropRect.left = x.coerceIn(0f, (cropRect.right - minCropSize).coerceAtLeast(0f))
                cropRect.top = y.coerceIn(0f, (cropRect.bottom - minCropSize).coerceAtLeast(0f))
            }
            1 -> { // Top-right
                cropRect.right = x.coerceIn((cropRect.left + minCropSize).coerceAtMost(viewWidth), viewWidth)
                cropRect.top = y.coerceIn(0f, (cropRect.bottom - minCropSize).coerceAtLeast(0f))
            }
            2 -> { // Bottom-left
                cropRect.left = x.coerceIn(0f, (cropRect.right - minCropSize).coerceAtLeast(0f))
                cropRect.bottom = y.coerceIn((cropRect.top + minCropSize).coerceAtMost(viewHeight), viewHeight)
            }
            3 -> { // Bottom-right
                cropRect.right = x.coerceIn((cropRect.left + minCropSize).coerceAtMost(viewWidth), viewWidth)
                cropRect.bottom = y.coerceIn((cropRect.top + minCropSize).coerceAtMost(viewHeight), viewHeight)
            }
        }
        Log.d("CropView", "Updated cropRect: $cropRect")
        updateGestureExclusion()
        invalidate()
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