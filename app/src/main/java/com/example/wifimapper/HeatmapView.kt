package com.example.wifimapper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Shows the loaded floor plan image, letterboxed to fit, and draws a
 * translucent colored dot (green = strong signal, red = weak) at every
 * logged reading. Supports pinch-to-zoom and panning.
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var floorPlan: Bitmap? = null
        set(value) {
            field = value
            recomputeImageRect()
            resetZoom()
            invalidate()
        }

    var readings: List<WifiReading> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var showHeatmap: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var useBlankMap: Boolean = false
        set(value) {
            field = value
            recomputeImageRect()
            resetZoom()
            invalidate()
        }

    /** Called with (xFraction, yFraction) in 0..1 relative to the image. */
    var onTapListener: ((Float, Float) -> Unit)? = null

    private val dotRadiusPx = 40f
    private val heatmapRadiusPx = 180f
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 170 }
    private val heatmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var imageRect = RectF()
    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            drawMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            drawMatrix.postTranslate(-distanceX, -distanceY)
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (floorPlan != null || useBlankMap) {
                drawMatrix.invert(inverseMatrix)
                val pts = floatArrayOf(e.x, e.y)
                inverseMatrix.mapPoints(pts)
                val tx = pts[0]
                val ty = pts[1]

                if (imageRect.contains(tx, ty)) {
                    val xFraction = (tx - imageRect.left) / imageRect.width()
                    val yFraction = (ty - imageRect.top) / imageRect.height()
                    onTapListener?.invoke(xFraction, yFraction)
                    return true
                }
            }
            return false
        }
    })

    fun resetZoom() {
        drawMatrix.reset()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeImageRect()
        resetZoom()
    }

    private fun recomputeImageRect() {
        val bmp = floorPlan
        if (bmp == null) {
            if (useBlankMap) {
                val size = minOf(width, height).toFloat()
                val left = (width - size) / 2f
                val top = (height - size) / 2f
                imageRect = RectF(left, top, left + size, top + size)
            } else {
                imageRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            }
            return
        }
        val viewRatio = width.toFloat() / height.toFloat()
        val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
        if (imgRatio > viewRatio) {
            val drawH = width / imgRatio
            val top = (height - drawH) / 2f
            imageRect = RectF(0f, top, width.toFloat(), top + drawH)
        } else {
            val drawW = height * imgRatio
            val left = (width - drawW) / 2f
            imageRect = RectF(left, 0f, left + drawW, height.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.save()
        canvas.concat(drawMatrix)

        labelPaint.color = ContextCompat.getColor(context, R.color.heatmap_label)
        placeholderPaint.color = ContextCompat.getColor(context, R.color.heatmap_placeholder)
        gridPaint.color = ContextCompat.getColor(context, R.color.heatmap_grid)

        val bmp = floorPlan
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, imageRect, null)
        } else if (useBlankMap) {
            drawGrid(canvas)
        } else {
            canvas.drawText(
                "No floor plan loaded yet",
                width / 2f, height / 2f, placeholderPaint
            )
            canvas.restore()
            return
        }

        if (showHeatmap) {
            drawVisualHeatmap(canvas)
        }

        // We need to scale the dots/labels inversely to the zoom so they stay legible
        drawMatrix.getValues(matrixValues)
        val currentScale = matrixValues[Matrix.MSCALE_X]
        val adjustedDotRadius = dotRadiusPx / currentScale
        val adjustedLabelSize = 24f / currentScale
        labelPaint.textSize = adjustedLabelSize

        for (reading in readings) {
            val cx = imageRect.left + reading.xFraction * imageRect.width()
            val cy = imageRect.top + reading.yFraction * imageRect.height()
            val quality = rssiToQuality(reading.avgRssiDbm)
            dotPaint.color = qualityToColor(quality)
            canvas.drawCircle(cx, cy, adjustedDotRadius, dotPaint)
            if (!showHeatmap) {
                canvas.drawText("${reading.avgRssiDbm}", cx, cy + (adjustedLabelSize / 3), labelPaint)
            }
        }
        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas) {
        canvas.drawRect(imageRect, gridPaint)
        val step = imageRect.width() / 10f
        for (i in 1..9) {
            val x = imageRect.left + i * step
            canvas.drawLine(x, imageRect.top, x, imageRect.bottom, gridPaint)
            val y = imageRect.top + i * step
            canvas.drawLine(imageRect.left, y, imageRect.right, y, gridPaint)
        }
    }

    private fun drawVisualHeatmap(canvas: Canvas) {
        drawMatrix.getValues(matrixValues)
        val currentScale = matrixValues[Matrix.MSCALE_X]
        val adjustedHeatmapRadius = heatmapRadiusPx / currentScale

        for (reading in readings) {
            val cx = imageRect.left + reading.xFraction * imageRect.width()
            val cy = imageRect.top + reading.yFraction * imageRect.height()
            val quality = rssiToQuality(reading.avgRssiDbm)
            val color = qualityToColor(quality)
            
            val gradient = android.graphics.RadialGradient(
                cx, cy, adjustedHeatmapRadius,
                intArrayOf(color, Color.TRANSPARENT),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            heatmapPaint.shader = gradient
            heatmapPaint.alpha = 130
            canvas.drawCircle(cx, cy, adjustedHeatmapRadius, heatmapPaint)
        }
    }

    /** Green (strong) -> yellow -> red (weak) */
    private fun qualityToColor(quality: Float): Int {
        val hue = quality.coerceIn(0f, 1f) * 120f // 0=red, 120=green
        return Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.9f))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }
}
