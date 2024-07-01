package com.example.data_entry_android

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrix = Matrix()
    private var mode = NONE

    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 5f
    private var m: FloatArray = FloatArray(9)
    private var redundantXSpace = 0f
    private var redundantYSpace = 0f
    private var width = 0f
    private var height = 0f
    private var saveScale = 1f
    private var right = 0f
    private var bottom = 0f
    private var origWidth = 0f
    private var origHeight = 0f

    private var scaleDetector: ScaleGestureDetector

    init {
        super.setClickable(true)
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        matrix.setTranslate(1f, 1f)
        setImageMatrix(matrix)
        scaleType = ScaleType.MATRIX
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }
            right = width * saveScale - width - (2 * redundantXSpace * saveScale)
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale)
            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2)
                if (mScaleFactor < 1) {
                    matrix.getValues(m)
                    val x = m[Matrix.MTRANS_X]
                    val y = m[Matrix.MTRANS_Y]
                    if (mScaleFactor < 1) {
                        if (Math.round(origWidth * saveScale) < width) {
                            if (y < -bottom)
                                matrix.postTranslate(0f, -(y + bottom))
                            else if (y > 0)
                                matrix.postTranslate(0f, -y)
                        } else {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0f)
                            else if (x > 0)
                                matrix.postTranslate(-x, 0f)
                        }
                    }
                }
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                matrix.getValues(m)
                val x = m[Matrix.MTRANS_X]
                val y = m[Matrix.MTRANS_Y]
                if (mScaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0f)
                    else if (x > 0)
                        matrix.postTranslate(-x, 0f)
                    if (y < -bottom)
                        matrix.postTranslate(0f, -(y + bottom))
                    else if (y > 0)
                        matrix.postTranslate(0f, -y)
                }
            }
            return true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        // Fit to screen.
        val scale: Float
        val scaleX = width / drawable.intrinsicWidth
        val scaleY = height / drawable.intrinsicHeight
        scale = scaleX.coerceAtMost(scaleY)
        matrix.setScale(scale, scale)
        setImageMatrix(matrix)
        saveScale = 1f
        // Center the image
        redundantYSpace = height - (scale * drawable.intrinsicHeight)
        redundantXSpace = width - (scale * drawable.intrinsicWidth)
        redundantYSpace /= 2f
        redundantXSpace /= 2f
        matrix.postTranslate(redundantXSpace, redundantYSpace)
        origWidth = width - 2 * redundantXSpace
        origHeight = height - 2 * redundantYSpace
        right = width * saveScale - width - (2 * redundantXSpace * saveScale)
        bottom = height * saveScale - height - (2 * redundantYSpace * saveScale)
        setImageMatrix(matrix)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val currentPoint = PointF(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(currentPoint)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                val dx = currentPoint.x - last.x
                val dy = currentPoint.y - last.y
                val fixTransX = getFixDragTrans(dx, width, origWidth * saveScale)
                val fixTransY = getFixDragTrans(dy, height, origHeight * saveScale)
                matrix.postTranslate(fixTransX, fixTransY)
                fixTrans()
                last.set(currentPoint.x, currentPoint.y)
            }
            MotionEvent.ACTION_UP -> {
                mode = NONE
                val xDiff = Math.abs(currentPoint.x - start.x).toInt()
                val yDiff = Math.abs(currentPoint.y - start.y).toInt()
                if (xDiff < CLICK && yDiff < CLICK)
                    performClick()
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        setImageMatrix(matrix)
        invalidate()
        return true
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else {
            delta
        }
    }

    private fun fixTrans() {
        matrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, width, origWidth * saveScale)
        val fixTransY = getFixTrans(transY, height, origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f)
            matrix.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans)
            return -trans + minTrans
        return if (trans > maxTrans) -trans + maxTrans else 0f
    }

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        const val CLICK = 3
    }
}