package com.wj.androidm3.business.ui.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 固定中心标尺的视频时间轴。
 *
 * 设计选择：标尺永远绘制在 width / 2，手势只改变 [positionMs]。最大缩放让一个
 * frameDuration 占一个可辨识的单元格，因此位置计算和刻度均可精确吸附到单帧；这比
 * 用 SeekBar 的整数 progress 映射长视频更稳定，也允许在缩放后继续保持亚秒精度。
 */
class VideoTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onScrubStart()
        fun onPositionChanged(positionMs: Long, fromUser: Boolean)
        fun onScrubStop(positionMs: Long)
        fun onThumbnailWindowRequested(timesMs: List<Long>, widthPx: Int, heightPx: Int)
    }

    var listener: Listener? = null

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 13f * resources.displayMetrics.scaledDensity
    }
    private val secondaryTextPaint = Paint(textPaint).apply {
        color = Color.rgb(170, 180, 196)
        textSize = 10f * resources.displayMetrics.scaledDensity
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(101, 214, 255)
        strokeWidth = 2f * density
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(113, 125, 145)
        strokeWidth = density
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val placeholderPaint = Paint().apply { color = Color.rgb(38, 44, 56) }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 36, 48) }

    private val thumbnailTop = 48f * density
    private val thumbnailHeight = 104f * density
    private val thumbnailWidth = 72f * density
    private val frameCellWidth = 42f * density
    private val minTickSpacing = 42f * density
    private val thumbnailDestination = RectF()
    private val thumbnailSource = Rect()
    private val thumbnails = HashMap<Long, Bitmap>()

    private var durationMs = 0L
    private var frameDurationMs = 1000.0 / 30.0
    private var fps = 30.0
    private var positionMs = 0.0
    private var pixelsPerMs = 0.01
    private var minPixelsPerMs = 0.01
    private var maxPixelsPerMs = 1.0
    private var thumbnailStepMs = 1_000L
    private var requestedThumbnailSignature = ""

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val scroller = OverScroller(context)
    private val maximumFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private var velocityTracker: VelocityTracker? = null
    private var lastTouchX = 0f
    private var scrubActive = false

    private val requestThumbnailsRunnable = Runnable { requestVisibleThumbnails() }

    fun configure(durationMs: Long, fps: Double) {
        this.durationMs = max(0L, durationMs)
        this.fps = if (fps.isFinite() && fps > 0.0) fps else 30.0
        frameDurationMs = 1000.0 / this.fps
        recalculateZoomBounds(resetZoom = true)
        setPosition(0L, fromUser = false)
        scheduleThumbnailRequest()
    }

    /** Player 正常播放时调用；正在拖动时不覆盖手势位置。 */
    fun setPlaybackPosition(positionMs: Long) {
        if (!scrubActive && scroller.isFinished) {
            setPosition(positionMs, fromUser = false)
        }
    }

    fun submitThumbnail(timeMs: Long, bitmap: Bitmap) {
        thumbnails[timeMs] = bitmap
        invalidate()
    }

    fun clearThumbnails() {
        thumbnails.clear()
        requestedThumbnailSignature = ""
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateZoomBounds(resetZoom = oldw == 0)
        scheduleThumbnailRequest()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (durationMs <= 0L || width == 0) {
            canvas.drawText("00:00:00:00", width / 2f, 30f * density, textPaint)
            drawIndicator(canvas)
            return
        }

        drawTimestampBubble(canvas)
        drawThumbnails(canvas)
        drawTicks(canvas)
        drawIndicator(canvas)
    }

    private fun drawTimestampBubble(canvas: Canvas) {
        val center = width / 2f
        val bubble = RectF(center - 69f * density, 7f * density, center + 69f * density, 38f * density)
        canvas.drawRoundRect(bubble, 7f * density, 7f * density, bubblePaint)
        canvas.drawText(formatTimecode(positionMs.roundToLong()), center, 28f * density, textPaint)
    }

    private fun drawThumbnails(canvas: Canvas) {
        val range = visibleRange()
        val step = calculateThumbnailStep()
        thumbnailStepMs = step
        var time = floor(range.first.toDouble() / step).toLong() * step
        while (time <= range.last + step) {
            if (time >= 0L && time <= durationMs) {
                val left = timeToX(time.toDouble())
                thumbnailDestination.set(left, thumbnailTop, left + thumbnailWidth, thumbnailTop + thumbnailHeight)
                val bitmap = thumbnails[time]
                if (bitmap == null || bitmap.isRecycled) {
                    canvas.drawRect(thumbnailDestination, placeholderPaint)
                } else {
                    // centerCrop：时间轴单元尺寸恒定，画面本身不被拉伸。
                    val sourceWidth = (bitmap.height * thumbnailWidth / thumbnailHeight).roundToInt()
                        .coerceAtMost(bitmap.width)
                    val sourceLeft = (bitmap.width - sourceWidth) / 2
                    thumbnailSource.set(sourceLeft, 0, sourceLeft + sourceWidth, bitmap.height)
                    canvas.drawBitmap(bitmap, thumbnailSource, thumbnailDestination, null)
                }
                canvas.drawRect(thumbnailDestination, borderPaint)
            }
            time += step
        }
    }

    private fun drawTicks(canvas: Canvas) {
        val tickInterval = calculateTickInterval()
        val range = visibleRange()
        var index = floor(range.first / tickInterval).toLong()
        val lastIndex = ceil(range.last / tickInterval).toLong()
        val baseY = height - 12f * density
        while (index <= lastIndex) {
            val time = index * tickInterval
            if (time in 0.0..durationMs.toDouble()) {
                val x = timeToX(time)
                val major = index % 5L == 0L
                canvas.drawLine(x, baseY - (if (major) 14f else 7f) * density, x, baseY, tickPaint)
                if (major) {
                    canvas.drawText(formatShortTime(time.roundToLong()), x, baseY - 18f * density, secondaryTextPaint)
                }
            }
            index++
        }
    }

    private fun drawIndicator(canvas: Canvas) {
        val center = width / 2f
        canvas.drawLine(center, 38f * density, center, height.toFloat(), indicatorPaint)
        val marker = android.graphics.Path().apply {
            moveTo(center - 6f * density, 38f * density)
            lineTo(center + 6f * density, 38f * density)
            lineTo(center, 46f * density)
            close()
        }
        canvas.drawPath(marker, indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                scroller.forceFinished(true)
                lastTouchX = event.x
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                scrubActive = true
                listener?.onScrubStart()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    setPositionDouble(positionMs - dx / pixelsPerMs, fromUser = true)
                }
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_UP -> {
                var flinging = false
                velocityTracker?.run {
                    addMovement(event)
                    computeCurrentVelocity(1000, maximumFlingVelocity.toFloat())
                    flinging = startFling(xVelocity)
                    recycle()
                }
                velocityTracker = null
                if (!flinging) finishScrub()
                performClick()
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                scroller.forceFinished(true)
                finishScrub()
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun startFling(fingerVelocityX: Float): Boolean {
        if (kotlin.math.abs(fingerVelocityX) < ViewConfiguration.get(context).scaledMinimumFlingVelocity) return false
        val start = (positionMs * pixelsPerMs).roundToInt()
        val end = min(Int.MAX_VALUE.toDouble(), durationMs * pixelsPerMs).roundToInt()
        scroller.fling(start, 0, -fingerVelocityX.roundToInt(), 0, 0, end, 0, 0)
        postInvalidateOnAnimation()
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            setPositionDouble(scroller.currX / pixelsPerMs, fromUser = true)
            if (scroller.isFinished) finishScrub() else postInvalidateOnAnimation()
        } else if (scrubActive && velocityTracker == null) {
            finishScrub()
        }
    }

    private fun finishScrub() {
        if (!scrubActive) return
        scrubActive = false
        val snapped = snapToFrame(positionMs)
        setPositionDouble(snapped, fromUser = true)
        listener?.onScrubStop(snapped.roundToLong())
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            pixelsPerMs = (pixelsPerMs * detector.scaleFactor).coerceIn(minPixelsPerMs, maxPixelsPerMs)
            requestedThumbnailSignature = ""
            scheduleThumbnailRequest()
            invalidate()
            return true
        }
    }

    private fun recalculateZoomBounds(resetZoom: Boolean) {
        if (width <= 0 || durationMs <= 0L) return
        minPixelsPerMs = width.toDouble() / durationMs.coerceAtLeast(1L)
        maxPixelsPerMs = frameCellWidth / frameDurationMs
        if (maxPixelsPerMs < minPixelsPerMs) maxPixelsPerMs = minPixelsPerMs
        pixelsPerMs = if (resetZoom) minPixelsPerMs else pixelsPerMs.coerceIn(minPixelsPerMs, maxPixelsPerMs)
    }

    private fun setPosition(value: Long, fromUser: Boolean) = setPositionDouble(value.toDouble(), fromUser)

    private fun setPositionDouble(value: Double, fromUser: Boolean) {
        val newValue = value.coerceIn(0.0, durationMs.toDouble())
        if (newValue == positionMs && fromUser) return
        positionMs = newValue
        invalidate()
        scheduleThumbnailRequest()
        listener?.onPositionChanged(positionMs.roundToLong(), fromUser)
    }

    private fun snapToFrame(value: Double): Double =
        (value / frameDurationMs).roundToLong().times(frameDurationMs).coerceIn(0.0, durationMs.toDouble())

    private fun timeToX(timeMs: Double): Float =
        (width / 2.0 + (timeMs - positionMs) * pixelsPerMs).toFloat()

    private fun visibleRange(): LongRange {
        val halfWindow = width / 2.0 / pixelsPerMs
        val start = max(0.0, positionMs - halfWindow).toLong()
        val end = min(durationMs.toDouble(), positionMs + halfWindow).toLong()
        return start..end
    }

    private fun calculateThumbnailStep(): Long {
        val rawStep = thumbnailWidth / pixelsPerMs
        return max(frameDurationMs, rawStep).roundToLong().coerceAtLeast(1L)
    }

    private fun calculateTickInterval(): Double {
        val desired = max(frameDurationMs, minTickSpacing / pixelsPerMs)
        val frameMultiples = doubleArrayOf(frameDurationMs, frameDurationMs * 2, frameDurationMs * 5)
        val fixed = doubleArrayOf(100.0, 200.0, 500.0, 1_000.0, 2_000.0, 5_000.0, 10_000.0, 30_000.0, 60_000.0)
        return (frameMultiples + fixed).sorted().firstOrNull { it >= desired } ?: 300_000.0
    }

    private fun scheduleThumbnailRequest() {
        removeCallbacks(requestThumbnailsRunnable)
        postDelayed(requestThumbnailsRunnable, 80L)
    }

    private fun requestVisibleThumbnails() {
        if (width <= 0 || durationMs <= 0L) return
        val step = calculateThumbnailStep()
        val range = visibleRange()
        val start = floor(range.first.toDouble() / step).toLong() * step
        val times = ArrayList<Long>()
        var time = max(0L, start - step)
        while (time <= min(durationMs, range.last + step) && times.size < 40) {
            times += time
            time += step
        }
        val signature = "${step}:${times.firstOrNull()}:${times.lastOrNull()}"
        if (signature == requestedThumbnailSignature) return
        requestedThumbnailSignature = signature
        // View 只持有当前窗口，历史位图由 Activity 的有界 LruCache 管理，避免长视频越拖越占内存。
        thumbnails.keys.retainAll(times.toHashSet())
        listener?.onThumbnailWindowRequested(times, thumbnailWidth.roundToInt(), thumbnailHeight.roundToInt())
    }

    private fun formatTimecode(timeMs: Long): String {
        val totalSeconds = timeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds / 60L) % 60L
        val seconds = totalSeconds % 60L
        val frame = floor((timeMs % 1000L) / frameDurationMs).toInt().coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frame)
    }

    private fun formatShortTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
