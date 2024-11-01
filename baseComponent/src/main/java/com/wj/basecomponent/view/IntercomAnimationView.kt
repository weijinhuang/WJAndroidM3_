package com.wj.basecomponent.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.wj.basecomponent.R
import com.wj.basecomponent.util.log.WJLog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 *@Create by H.W.J 2024/5/25/025
 */
class IntercomAnimationView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    private val mPain = Paint()

    private var mColor = Color.WHITE
    private var mStrokeWidth = 10f

    private var mMinHeight = 10f

    private var mHoldTime = 2000L

    private var mAnimationTime = 500

    private var mCurrentDelayTime = 42L

    private var mLauncherDelayTime = 0
    private var mHideTime = 0L


    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.IntercomAnimationView)
            mStrokeWidth = typedArray.getDimension(R.styleable.IntercomAnimationView_intercom_animationview_line_width, 5f)
            mMinHeight = typedArray.getDimension(R.styleable.IntercomAnimationView_intercom_animationview_line_min_height, 5f)
            mColor = typedArray.getColor(R.styleable.IntercomAnimationView_intercom_animationview_color, Color.WHITE)
            mHoldTime = typedArray.getInt(R.styleable.IntercomAnimationView_intercom_animationview_line_hold_time, 2000).toLong()
            mAnimationTime = typedArray.getInt(R.styleable.IntercomAnimationView_intercom_animationview_line_anim_time, 500)
            mLauncherDelayTime = typedArray.getInt(R.styleable.IntercomAnimationView_intercom_animationview_line_launcher_delay_time, 500)
            mHideTime = typedArray.getInt(R.styleable.IntercomAnimationView_intercom_animationview_line_launcher_hide_time, 500).toLong()
            typedArray.recycle()
        }

        mPain.style = Paint.Style.FILL
        mPain.strokeCap = Paint.Cap.ROUND
        mPain.isAntiAlias = true
        mPain.strokeWidth = mStrokeWidth
        mPain.setColor(mColor)
    }


    var startX: Float = 0f
    var stopX: Float = 0f

    var startY: Float = 0f
    var stopY: Float = 0f

    private var mStepY = 0f

    private var mDelayTime1 = 16

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredHeight < mMinHeight) {
            mMinHeight = measuredHeight.toFloat()
        }
        mDelayTime1 = mAnimationTime / 32

        mStepY = ((measuredHeight - mMinHeight) / 2) / mDelayTime1

        startX = (measuredWidth + mStrokeWidth) / 2

        stopX = startX


        mFirstStartY = (measuredHeight - mMinHeight) / 2

        mFirstStopY = (measuredHeight + mMinHeight) / 2

        WJLog.d("onMeasure:: $mStepY = (($measuredHeight - $mMinHeight) / 2) / 24")

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mGlobalJob?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        if (mHide || (startY == stopY)) {
            super.onDraw(canvas)
        } else {
            WJLog.d("mCurrentDelayTime:$mCurrentDelayTime startY: $startY mFirstStartY:$mFirstStartY stopY: $stopY  mCurrentStep: $mCurrentStep ")
            canvas.drawLine(startX, startY, stopX, stopY, mPain)
        }
    }

    private var mCurrentStep = 1

    private var direction = 1

    private var mGlobalJob: Job? = null

    var mFirstStartY: Float = 0f
    var mFirstStopY: Float = 0f
    private var mHide = false

    fun startAnimation() {
        mGlobalJob?.cancel()
        mGlobalJob = GlobalScope.launch {
            delay(mLauncherDelayTime.toLong())
            while (isActive) {
                if (mStepY != 0f) {
                    if (mCurrentDelayTime == mHoldTime) {
                        mHide = true
                        postInvalidate()
                        mCurrentDelayTime = mDelayTime1.toLong()
                        delay(mLauncherDelayTime.toLong())
                        mHide = false
                    } else {
                        mHide  = false
                        var yShift = mCurrentStep * mStepY
                        startY = mFirstStartY - yShift
                        stopY = (mFirstStopY) + yShift

                        if (startY <= 0) {
                            WJLog.d("direction = -1")
                            direction = -1
                        } else if (startY > mFirstStartY) {
                            WJLog.d("direction = 1")
                            direction = 1
                            postInvalidate()
                            mCurrentDelayTime = mHoldTime.toLong()
                        } else {
                            postInvalidate()
                            mCurrentDelayTime = mDelayTime1.toLong()
                        }
                        delay(mCurrentDelayTime)
                        mCurrentStep += direction
                    }

                }
            }
        }
    }


}