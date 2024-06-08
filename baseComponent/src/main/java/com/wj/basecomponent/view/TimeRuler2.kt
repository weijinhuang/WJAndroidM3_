package com.wj.basecomponent.view

import android.content.Context
import android.graphics.*

import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.LinearLayout
import com.wj.basecomponent.R
import java.text.SimpleDateFormat
import java.util.*

/**
 *最大时间数
 */ 

/**
 * 时间刻度尺
 */
class TimeRuler2(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    private var mTimeZone = TimeZone.getTimeZone("GMT+08:00")

    private var mTimeList: List<IVideo>? = null

    private val mPaint: Paint = Paint()

    private var mOrientationMode = LinearLayout.HORIZONTAL

    private var mMsInScreen = MAX_MS// 一个屏幕包含的毫秒
    private var mPixelPerMS = 0f//1毫秒的像素长度
    private var mMSPerPixel = 0//1像素的毫秒数

    private var mDrawStrategy = TEXT_FIRST

    var mFirstScaleTime = 0L

    private var mLineSize = 10f
    private var mLineStrokeWidth = 3f

    private var mLeftTime = 0L
    private var mLeftTimeTemp = 0L
    private var mMiddleTime = 0L
    private var mMiddleTimeTemp = 0L

    private var mBeginTime = 0L//刻度尺的开始时间
    private var mEndTime = 0L//刻度尺的结束时间

    private var mScaleMsStep = 1L//2个小刻度之间的毫秒数
    private var mTimeTextStep = 0L//2个文字刻度的毫秒数

    private var mFirstTouchPoint = PointF()

    private var mScaleGestureDetector: ScaleGestureDetector? = null

    private val mSimpleDateFormat = SimpleDateFormat("HH:mm")
    private val mSimpleDateFormatFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private var mTextSize = 16
    private var mTextColor = Color.BLACK

    private var mTextWidth = 16

    private var mOnTimeSelected: ((Long, TimeZone) -> Unit)? = null

    private var mTimeOrder = TIME_ORDER_REVERSE

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null, 0)

    private var mOnTimeMoveListener: ((timeOffsetInMs: Long) -> Unit)? = null
    private var mOnScaleListener: ((scaleFactor: Float) -> Unit)? = null

    fun setOnTimeSelectListener(onTimeSelect: (Long, TimeZone) -> Unit) {
        mOnTimeSelected = onTimeSelect
    }

    fun setOnScaleListener(onScaleListener: (scaleFactor: Float) -> Unit) {
        mOnScaleListener = onScaleListener
    }

    init {
        attrs?.let {
            context?.let { ctx ->
                ctx.obtainStyledAttributes(it, R.styleable.TimeRuler).let { obtainStyledAttributes ->
                    mTextColor = obtainStyledAttributes.getColor(R.styleable.TimeRuler_android_textColor, Color.BLACK)
                    mDrawStrategy = obtainStyledAttributes.getInt(R.styleable.TimeRuler_draw_strategy, TEXT_FIRST)
                    mTextSize = obtainStyledAttributes.getDimensionPixelSize(R.styleable.TimeRuler_android_textSize, 16)
                    mLineSize = obtainStyledAttributes.getDimensionPixelSize(R.styleable.TimeRuler_time_ruler_line_height, 10).toFloat()
                    mLineStrokeWidth = obtainStyledAttributes.getDimensionPixelSize(R.styleable.TimeRuler_time_ruler_line_width, 3).toFloat()
                    mOrientationMode = obtainStyledAttributes.getInt(R.styleable.TimeRuler_android_orientation, LinearLayout.HORIZONTAL)
                    mTimeOrder = obtainStyledAttributes.getInt(R.styleable.TimeRuler_time_order, TIME_ORDER_REVERSE)
                    mPaint.color = mTextColor
                    mPaint.textSize = mTextSize.toFloat()
                    mTextWidth = mPaint.measureText("00:00").toInt()
                    obtainStyledAttributes.recycle()
                }
            }
        }
        initTime()
        initGestureDetector()
    }

    private fun initTime() {
        mSimpleDateFormat.timeZone = mTimeZone
        val calendar = Calendar.getInstance()
        calendar.timeZone = mTimeZone
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStartTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val dayEndTime = calendar.timeInMillis
        mMiddleTime = (dayStartTime + dayEndTime) shr 1
        if (mTimeOrder == TIME_ORDER_POSITIVE) {
            mBeginTime = dayStartTime
            mEndTime = dayEndTime
            mLeftTime = mMiddleTime - (mMsInScreen shr 1)
        } else {
            mBeginTime = dayEndTime
            mEndTime = dayStartTime
            mLeftTime = mMiddleTime + (mMsInScreen shr 1)
        }

        mPaint.color = mTextColor
        mPaint.textSize = mTextSize.toFloat()
    }

    fun setCurrentTime(currentTime: Long) {
        if (mOnTouching) {
            return
        }
        mMiddleTime = currentTime
        if (mTimeOrder == TIME_ORDER_POSITIVE) {
            mLeftTime = mMiddleTime - (mMsInScreen shr 1)
        } else {
            mLeftTime = mMiddleTime + (mMsInScreen shr 1)
        }
        invalidate()
    }

    fun setData(timeData: List<IVideo>) {
        mTimeList = timeData
        invalidate()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
//        LogUtils.d("onVisibilityChanged ${changedView.javaClass.simpleName} -> $visibility")
        if (changedView == this && visibility == View.VISIBLE) {
            calcPixel()
        }
    }

    private var mCurrentScaleFactor: Float = 1f

    private fun onMsInScreenChange(newMsInScreen: Int) {
        var msInScreen = newMsInScreen
        if (msInScreen > MAX_MS) {
            msInScreen = MAX_MS
        } else if (msInScreen < MIN_MS) {
            msInScreen = MIN_MS
        }
        mMsInScreen = msInScreen
        mLeftTime = if (mTimeOrder == TIME_ORDER_POSITIVE) {
            mMiddleTime - (mMsInScreen shr 1)
        } else {
            mMiddleTime + (mMsInScreen shr 1)
        }
//        LogUtils.d("mMsInScreen:$mMsInScreen")
        calcPixel()
        invalidate()
    }

    fun setScale(scaleFactor: Float) {
//        LogUtils.d("setScale($scaleFactor: Float)")
        mCurrentScaleFactor = scaleFactor
        var msInScreen = (MAX_MS * mCurrentScaleFactor).toInt()
        onMsInScreenChange(msInScreen)
    }

    private fun initGestureDetector() {
        mScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                return if (mMsInScreen > MAX_MS || mMsInScreen < MIN_MS) {
                    false
                } else {
                    detector.let {
                        if (detector.scaleFactor != 1f) {
                            var msInScreen = mMsInScreen / detector.scaleFactor
//                            LogUtils.d("$mMsInScreen/ ${detector.scaleFactor}  -> $msInScreen")
                            onMsInScreenChange(msInScreen.toInt())
                            mCurrentScaleFactor = msInScreen / MAX_MS
                            mOnScaleListener?.invoke(mCurrentScaleFactor)
                        }
                    }
                    true
                }
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                detector.let {
//                    LogUtils.d("onScaleBegin currentSpan -> ${detector.currentSpan}")
                }
                mScaleEnable = true
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                detector.let {
//                    LogUtils.d("onScaleEnd scaleFactor -> ${detector.scaleFactor}")
                }
//                mScaleEnable = false
            }

        })
    }

    private var mScaleEnable = false
    private var mOnTouching = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        LogUtils.d("actionMasked -> ${event.actionMasked} action -> ${event.action} pointerCount -> ${event.pointerCount}")
        mScaleGestureDetector?.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mOnTouching = true
                mLeftTimeTemp = mLeftTime
                mMiddleTimeTemp = mMiddleTime
                mFirstTouchPoint.set(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
//                    LogUtils.d("mScaleGestureDetector?.onTouchEvent(event)")
                    mScaleGestureDetector?.onTouchEvent(event)
                } else {
                    if (!mScaleEnable) {
//                        LogUtils.d("ACTION_MOVE")
                        onMove(event)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_UP -> {
//                LogUtils.d("selected time -> ${mLeftTime + (mMsInScreen shr 1)}")
                if (!mScaleEnable) {
                    mOnTimeSelected?.invoke(mMiddleTime + mTimeRawOffsetInMs, mTimeZone)
                }
                mScaleEnable = false
                mOnTouching = false
            }
        }
        return true

    }


    private fun onMove(event: MotionEvent) {
        val historySize = event.historySize
        for (i in 0 until historySize) {

            val historicalCoordinate: Float
            val moveCoordinate: Float
            if (mOrientationMode == LinearLayout.HORIZONTAL) {
                historicalCoordinate = event.getHistoricalX(i)
                moveCoordinate = historicalCoordinate - mFirstTouchPoint.x
            } else {//LinearLayout.VERTICAL
                historicalCoordinate = event.getHistoricalY(i)
                moveCoordinate = historicalCoordinate - mFirstTouchPoint.y
            }
            val timeShift: Long = (mMSPerPixel * moveCoordinate).toLong()
            if (mTimeOrder == TIME_ORDER_POSITIVE) {
                mLeftTime = mLeftTimeTemp - timeShift
                mMiddleTime = mMiddleTimeTemp - timeShift
            } else {
                mLeftTime = mLeftTimeTemp + timeShift
                mMiddleTime = mMiddleTimeTemp + timeShift
            }
            val timeSecondShift = mLeftTime % mScaleMsStep
            mFirstScaleTime = if (timeSecondShift == 0L) {
                mLeftTime
            } else {
                if (mTimeOrder == TIME_ORDER_POSITIVE) {
                    mLeftTime + mScaleMsStep - timeSecondShift
                } else {
                    mLeftTime - timeSecondShift
                }
            }
            invalidate()
            mOnTimeMoveListener?.invoke(timeShift)
        }

    }

    override fun onDraw(canvas: Canvas) {
        calcPixel()
        canvas?.let {
            if (mOrientationMode == LinearLayout.HORIZONTAL) {
                drawTimeDataHorizontal(canvas)
                drawScaleHorizontal(canvas)
            } else {
                drawTimeDataVertical(canvas)
                drawScaleVertical(canvas)
            }
        }
    }


    private fun calcPixel() {
        val usefulViewPixel = if (mOrientationMode == LinearLayout.HORIZONTAL) (width) else (height)
        if (usefulViewPixel == 0) {
            return
        }
        mPixelPerMS = usefulViewPixel / (mMsInScreen.toFloat())
        mMSPerPixel = mMsInScreen / usefulViewPixel
        when (mMsInScreen) {
            MIN_MS -> {// 6分钟
                mScaleMsStep = 12000
            }
            in MIN_MS..30 * 60 * 1000 -> {//6到30分钟
                mScaleMsStep = 60000
            }
            in 30 * 60 * 1000..90 * 60 * 1000 -> {//30到90分钟
                mScaleMsStep = 180000
            }
            in 90 * 60 * 1000..180 * 60 * 1000 -> {//1个半小时到3个小时
                mScaleMsStep = 360000
            }
            in 180 * 60 * 1000..MAX_MS -> {//3小时到6小时
                mScaleMsStep = 720000
            }
        }
        mTimeTextStep = mScaleMsStep * 5

        val timeSecondShift = mLeftTime % mScaleMsStep
        mFirstScaleTime = if (timeSecondShift == 0L) {
            mLeftTime
        } else {
            if (mTimeOrder == TIME_ORDER_POSITIVE) {
                mLeftTime + mScaleMsStep - timeSecondShift
            } else {
                mLeftTime - timeSecondShift
            }
        }
//        LogUtils.d("mMsInScreen:$mMsInScreen mMiddleTime:${mSimpleDateFormatFull.format(mMiddleTime)} mFirstScaleTime:${mSimpleDateFormatFull.format(mFirstScaleTime)}")
    }

    private fun drawScaleHorizontal(canvas: Canvas) {
        mPaint.color = mTextColor
        var scaleX = (mFirstScaleTime - mLeftTime) * mPixelPerMS
        val leftBound = -mTextWidth
        val rightBound = width + mTextWidth

        val scaleStartY: Float
        val textY: Float
        val bigScaleStopY: Float
        val smallScaleStopY: Float

        if (mDrawStrategy == SCALE_FIRST) {
            scaleStartY = 0f
            textY = (height - mTextSize).toFloat()
            bigScaleStopY = height - mTextSize * 2f
            smallScaleStopY = bigScaleStopY * 0.7f

        } else {
            textY = mTextSize.toFloat()
            scaleStartY = mTextSize * 1.5f
            bigScaleStopY = height.toFloat()
            smallScaleStopY = bigScaleStopY * 0.7f
        }

//        LogUtils.d("mFirstScaleTime:${mSimpleDateFormatFull.format(mFirstScaleTime)}")
        while (scaleX < rightBound && scaleX > leftBound) {
            val fl = mFirstScaleTime % mTimeTextStep
            if (fl == 0L) {
                val timeText = mSimpleDateFormat.format(mFirstScaleTime)
                canvas.drawText(timeText, scaleX - (mTextWidth shr 1), textY, mPaint)
                mPaint.strokeWidth = mLineStrokeWidth * 1.5f
                canvas.drawLine(scaleX, scaleStartY, scaleX, bigScaleStopY, mPaint)
            } else {
                mPaint.strokeWidth = mLineStrokeWidth
                canvas.drawLine(scaleX, scaleStartY, scaleX, smallScaleStopY, mPaint)
            }

            scaleX += mScaleMsStep * mPixelPerMS
            mFirstScaleTime += mScaleMsStep
        }
        val middleScalePos = (mMsInScreen shr 1) * mPixelPerMS
        mPaint.color = Color.RED
        canvas.drawLine(middleScalePos, 0f, middleScalePos, height.toFloat(), mPaint)
    }

    private fun drawScaleVertical(canvas: Canvas) {
        mPaint.color = mTextColor
        val startBound = -mTextSize
        val endBound = height + mTextSize

        var scaleY = if (mTimeOrder == TIME_ORDER_POSITIVE) (mFirstScaleTime - mLeftTime) * mPixelPerMS else (mLeftTime - mFirstScaleTime) * mPixelPerMS
        var smallScaleStartX: Float
        var bigScaleStartX: Float
        var textX: Float
        var smallScaleStopX: Float
        var bigScaleStopX: Float
        val textWidth = mPaint.measureText("00:00")
        if (mDrawStrategy == TEXT_FIRST) {
            textX = 0f
            bigScaleStartX = textWidth * 1.2f
            bigScaleStopX = width.toFloat()
            smallScaleStartX = textWidth * 1.7f
            smallScaleStopX = bigScaleStopX
        } else {//SCALE_FIRST
            textX = width - textWidth
            smallScaleStartX = 0f
            bigScaleStartX = 0f
            smallScaleStopX = width - textWidth * 1.7f
            bigScaleStopX = width - textWidth * 1.2f
        }
        while (scaleY < endBound && scaleY > startBound) {
            val fl = mFirstScaleTime % mTimeTextStep
            if (fl == 0L) {
                val timeText = mSimpleDateFormat.format(mFirstScaleTime)
                canvas.drawText(timeText, textX, scaleY + mTextSize * 0.3f, mPaint)
                mPaint.strokeWidth = mLineStrokeWidth * 1.5f
                canvas.drawLine(bigScaleStartX, scaleY, bigScaleStopX, scaleY, mPaint)
            } else {
                mPaint.strokeWidth = mLineStrokeWidth
                canvas.drawLine(smallScaleStartX, scaleY, smallScaleStopX, scaleY, mPaint)
            }
            if (mTimeOrder == TIME_ORDER_POSITIVE) {
                scaleY += mScaleMsStep * mPixelPerMS
                mFirstScaleTime += mScaleMsStep
            } else {
                scaleY += mScaleMsStep * mPixelPerMS
                mFirstScaleTime -= mScaleMsStep
            }
        }
        val middleScalePos = (mMsInScreen shr 1) * mPixelPerMS
        mPaint.color = Color.RED
        mPaint.strokeWidth = mLineStrokeWidth * 2
        canvas.drawLine(0f, middleScalePos, width.toFloat(), middleScalePos, mPaint)
    }

    private val timeDataRect = RectF()
    private var mTimeRawOffsetInMs = 0

    fun setTimeRawOffset(timeOffsetInMs: Int) {
        mTimeRawOffsetInMs = timeOffsetInMs
    }

    private fun drawTimeDataHorizontal(canvas: Canvas) {
        mTimeList?.forEach { timeBean ->
            val left = (timeBean.beginTimeInMs - mTimeRawOffsetInMs - mLeftTime) * mPixelPerMS
            var top: Float
            var right = (timeBean.endTimeInMs - mTimeRawOffsetInMs - mLeftTime) * mPixelPerMS
            val bottom: Float
            if (mDrawStrategy == SCALE_FIRST) {
                top = 0f
                bottom = (height - mTextSize * 2f) * 0.7f
            } else {
                top = mTextSize * 1.5f
                bottom = height.toFloat() * 0.7f
            }
            if (right - left < 5) {
                right = left + 5
            }
            timeDataRect.set(left, top, right, bottom)
            mPaint.color = timeBean.color
            canvas.drawRect(timeDataRect, mPaint)
        }
    }

    private fun drawTimeDataVertical(canvas: Canvas) {
        var left: Float
        var top: Float
        var right: Float
        var bottom: Float
        val textWidth = mPaint.measureText("00:00")
        if (mDrawStrategy == TEXT_FIRST) {
            right = width.toFloat()
            left = textWidth * 1.7f
        } else {//SCALE_FIRST
            right = width - textWidth * 1.7f
            left = 0f
        }
        mTimeList?.forEach { timeBean ->
            if (mTimeOrder == TIME_ORDER_POSITIVE) {
                top = (timeBean.beginTimeInMs - mTimeRawOffsetInMs - mLeftTime) * mPixelPerMS
                bottom = (timeBean.endTimeInMs - mTimeRawOffsetInMs - mLeftTime) * mPixelPerMS
                timeDataRect.set(left, top, right, bottom)
                mPaint.color = timeBean.color
                canvas.drawRect(timeDataRect, mPaint)
            } else {
                top = (mLeftTime - timeBean.endTimeInMs - mTimeRawOffsetInMs) * mPixelPerMS
                bottom = (mLeftTime - timeBean.beginTimeInMs - mTimeRawOffsetInMs) * mPixelPerMS
                timeDataRect.set(left, top, right, bottom)
                mPaint.color = timeBean.color
                canvas.drawRect(timeDataRect, mPaint)
            }
        }
    }
}
