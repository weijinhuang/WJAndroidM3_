package com.wj.basecomponent.view

import android.content.Context
import android.graphics.*

import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.LinearLayout
import com.wj.basecomponent.R
import com.wj.basecomponent.util.log.WJLog
import java.text.SimpleDateFormat
import java.util.*

/**
 *最大时间数
 */
const val MAX_MS = 6 * 60 * 60 * 1000

const val MIN_MS = 6 * 60 * 1000

/**
 * 时间刻度尺
 */
class TimeRuler(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    private var mTimeZone = TimeZone.getTimeZone("GMT+08:00")

    private var mTimeList: List<TimeBean>? = null

    private val mPaint: Paint = Paint()

    private var mOrientationMode = LinearLayout.HORIZONTAL

    private var mMsInScreen = MAX_MS// 一个屏幕包含的毫秒
    private var mPixelPerMS = 0f//1毫秒的像素长度
    private var mMSPerPixel = 0//1像素的毫秒数

    private var mTimeDataWidth = 0f

    var mFirstScaleTime = 0L

    private var mLineSize = 10f

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

    private var mTextSize = 16
    private var mTextColor = Color.BLACK

    private var mTextWidth = 16

    private var mOnTimeSelected: ((Long, TimeZone) -> Unit)? = null

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null, 0)

    fun setOnTimeSelectListener(onTimeSelect: (Long, TimeZone) -> Unit) {
        mOnTimeSelected = onTimeSelect
    }

    init {
        attrs?.let {
            context?.let { ctx ->
                ctx.obtainStyledAttributes(it, R.styleable.TimeRuler).let { obtainStyledAttributes ->
                    mTextColor = obtainStyledAttributes.getColor(R.styleable.TimeRuler_android_textColor, Color.BLACK)
                    mTextSize = obtainStyledAttributes.getDimensionPixelSize(R.styleable.TimeRuler_android_textSize, 16)
                    mLineSize = obtainStyledAttributes.getDimensionPixelSize(R.styleable.TimeRuler_time_ruler_lineSize, 10).toFloat()
                    mOrientationMode = obtainStyledAttributes.getInt(R.styleable.TimeRuler_android_orientation, LinearLayout.HORIZONTAL)
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
        mBeginTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        mEndTime = calendar.timeInMillis
        mMiddleTime = (mBeginTime + mEndTime) shr 1
        mLeftTime = mMiddleTime - (mMsInScreen shr 1)
//        WJLog.d("mBeginTime:$mBeginTime mEndTime:$mEndTime mMiddleTime:$mMiddleTime mLeftTime:$mLeftTime ")
        mPaint.color = mTextColor
        mPaint.textSize = mTextSize.toFloat()
    }

    fun setCurrentTime(currentTime: Long) {
        mMiddleTime = currentTime
        mLeftTime = mMiddleTime - (mMsInScreen shr 1)
        invalidate()
    }

    fun setData(timeData: List<TimeBean>) {
        mTimeList = timeData
        invalidate()
    }

    private fun calcBoundsTime() {

    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        WJLog.d("onVisibilityChanged ${changedView.javaClass.simpleName} -> $visibility")
        if (changedView == this && visibility == View.VISIBLE) {
            calcPixel()
        }
    }

    private fun initGestureDetector() {
        mScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                WJLog.d("onScale -> ${detector?.scaleFactor}")
                return if (mMsInScreen > MAX_MS || mMsInScreen < MIN_MS) {
                    false
                } else {
                    detector?.let {
                        if (detector.scaleFactor != 1f) {
                            var msInScreen = (mMsInScreen / detector.scaleFactor).toInt()
                            WJLog.d("$mMsInScreen/ ${detector.scaleFactor}  -> $msInScreen")
                            if (msInScreen > MAX_MS) {
                                msInScreen = MAX_MS
                            } else if (msInScreen < MIN_MS) {
                                msInScreen = MIN_MS
                            }
                            mMsInScreen = msInScreen
                            mLeftTime = mMiddleTime - (mMsInScreen shr 1)
                            calcPixel()
                            invalidate()
                        }
                    }
                    true
                }
            }

            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                detector?.let {
                    WJLog.d("onScaleBegin currentSpan -> ${detector.currentSpan}")
                }
                mScaleEnable = true
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
                detector?.let {
                    WJLog.d("onScaleEnd scaleFactor -> ${detector.scaleFactor}")
                }
//                mScaleEnable = false
            }
        })
    }

    private var mScaleEnable = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        WJLog.d("actionMasked -> ${event.actionMasked} action -> ${event.action} pointerCount -> ${event.pointerCount}")
        mScaleGestureDetector?.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLeftTimeTemp = mLeftTime
                mMiddleTimeTemp = mMiddleTime
                mFirstTouchPoint.set(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
//                    WJLog.d("mScaleGestureDetector?.onTouchEvent(event)")
                    mScaleGestureDetector?.onTouchEvent(event)
                } else {
                    if (!mScaleEnable) {
//                        WJLog.d("ACTION_MOVE")
                        move(event)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_UP -> {
                mScaleEnable = false
//                WJLog.d("selected time -> ${mLeftTime + (mMsInScreen shr 1)}")
                mOnTimeSelected?.invoke(mLeftTime + (mMsInScreen shr 1), mTimeZone)
            }
        }
        return true

    }


    private fun move(event: MotionEvent) {
        val historySize = event.historySize
        for (i in 0 until historySize) {

            val historicalCoordinate: Float
            val moveCoordinate: Float
            if (mOrientationMode == LinearLayout.HORIZONTAL) {
                historicalCoordinate = event.getHistoricalX(i)
                moveCoordinate = historicalCoordinate - mFirstTouchPoint.x
            } else {
                historicalCoordinate = event.getHistoricalY(i)
                moveCoordinate = historicalCoordinate - mFirstTouchPoint.y
            }
            val timeShift: Long = (mMSPerPixel * moveCoordinate).toLong()
            mLeftTime = mLeftTimeTemp - timeShift
            mMiddleTime = mMiddleTimeTemp - timeShift
            val timeSecondShift = mLeftTime % mScaleMsStep
            mFirstScaleTime = if (timeSecondShift == 0L) {
                mLeftTime
            } else {
                mLeftTime + mScaleMsStep - timeSecondShift
            }
//            WJLog.d("mLeftTime -> $mLeftTime mMiddleTime -> $mMiddleTime")
            invalidate()

        }

    }

    override fun onDraw(canvas: Canvas?) {
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
        val viewPixel = if (mOrientationMode == LinearLayout.HORIZONTAL) width else height
        if (viewPixel == 0) {
            return
        }
        mPixelPerMS = viewPixel / (mMsInScreen.toFloat())
        mMSPerPixel = mMsInScreen / viewPixel
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
            mLeftTime + mScaleMsStep - timeSecondShift
        }
    }

    private fun drawScaleHorizontal(canvas: Canvas) {
        mPaint.color = mTextColor
        var scaleX = (mFirstScaleTime - mLeftTime) * mPixelPerMS
        val leftBound = -mTextWidth
        val rightBound = width + mTextWidth
        while (scaleX < rightBound && scaleX > leftBound) {
            val fl = mFirstScaleTime % mTimeTextStep
//            WJLog.d("$scaleX % $mTimeTextStep = $fl")
            if (fl == 0L) {
                val timeText = mSimpleDateFormat.format(mFirstScaleTime)
                canvas.drawText(timeText, scaleX - (mTextWidth shr 1), mLineSize * 2, mPaint)
                canvas.drawLine(scaleX, 0f, scaleX, mLineSize * 1.5f, mPaint)
            } else {
                canvas.drawLine(scaleX, 0f, scaleX, mLineSize, mPaint)
            }

            scaleX += mScaleMsStep * mPixelPerMS
            mFirstScaleTime += mScaleMsStep
        }
        val middleScalePos = (mMsInScreen shr 1) * mPixelPerMS
        val middleScalePos2 = (mMiddleTime - mLeftTime) * mPixelPerMS
        mPaint.color = Color.RED
        canvas.drawLine(middleScalePos, 0f, middleScalePos, height.toFloat(), mPaint)
        canvas.drawLine(middleScalePos2, 0f, middleScalePos2, height.toFloat(), mPaint)
    }

    private fun drawTimeDataHorizontal(canvas: Canvas) {
        mTimeList?.forEach { timeBean ->
            val left = (timeBean.startTime - mLeftTime) * mPixelPerMS
            val top = 0f
            val right = (timeBean.endTime - mLeftTime) * mPixelPerMS
            val bottom = mLineSize
            timeDataRect.set(left, top, right, bottom)
            mPaint.color = timeBean.color
            canvas.drawRect(timeDataRect, mPaint)
        }
    }

    private fun drawScaleVertical(canvas: Canvas) {
        mPaint.color = mTextColor
        var scaleY = (mFirstScaleTime - mLeftTime) * mPixelPerMS
        val leftBound = -mTextSize
        val rightBound = height + mTextSize
        while (scaleY < rightBound && scaleY > leftBound) {
            val fl = mFirstScaleTime % mTimeTextStep
//            WJLog.d("$scaleX % $mTimeTextStep = $fl")
            if (fl == 0L) {
                val timeText = mSimpleDateFormat.format(mFirstScaleTime)
//                WJLog.d("$mFirstScaleTime -> $timeText")
                canvas.drawText(timeText, (mTextSize shl 1).toFloat(), scaleY + mTextSize * 0.3f, mPaint)
                canvas.drawLine(0f, scaleY,  mLineSize * 1.5f , scaleY, mPaint)
            } else {
                canvas.drawLine(0f, scaleY, mLineSize, scaleY, mPaint)
            }

            scaleY += mScaleMsStep * mPixelPerMS
            mFirstScaleTime += mScaleMsStep
        }
        val middleScalePos = (mMsInScreen shr 1) * mPixelPerMS
        mPaint.color = Color.RED
        canvas.drawLine(0f, middleScalePos, width.toFloat(), middleScalePos, mPaint)
//        WJLog.d("middleScalePos : $middleScalePos")
    }

    val timeDataRect = RectF()
    private fun drawTimeDataVertical(canvas: Canvas) {

        mTimeList?.forEach { timeBean ->
            val left = 0f
            val top = (timeBean.startTime - mLeftTime) * mPixelPerMS
            val right = mLineSize
            val bottom = (timeBean.endTime - mLeftTime) * mPixelPerMS
            timeDataRect.set(left, top, right, bottom)
            mPaint.color = timeBean.color
            canvas.drawRect(timeDataRect, mPaint)
        }
    }
}

data class TimeBean(val startTime: Long, val endTime: Long, var color: Int = Color.GREEN)