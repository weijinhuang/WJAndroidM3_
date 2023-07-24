package com.wj.androidm3.business.ui.main.viewpage2

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


/**
 *@Create by H.W.J 2022/11/17/017
 */
class StringPicker : RecyclerView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    private var mDataSet: List<String> = ArrayList()
    private var mAdapter: StringPickerAdapter? = null
    private val mPaint = Paint()

    private var mItemHeight = 0

    init {
        mPaint.color = Color.BLACK
        mPaint.isAntiAlias = true
    }

    fun setDataSet(dataSet: List<String>) {
        mDataSet = dataSet
        if (null == mAdapter) {
            mAdapter = StringPickerAdapter(mItemHeight, mDataSet)
            adapter = mAdapter
            layoutManager = WJLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        } else {
            mAdapter?.setDataSet(dataSet)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.run {

            mPaint.strokeWidth = 5f
            drawLine(0f, mItemHeight.toFloat(), width.toFloat(), mItemHeight.toFloat(), mPaint)
            drawLine(0f, (mItemHeight * 2).toFloat(), width.toFloat(), (mItemHeight * 2).toFloat(), mPaint)
        }
    }

    private var mSelectedPos: Int = 0
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        when (state) {
            SCROLL_STATE_IDLE -> {
                var scrolledHeight = computeVerticalScrollOffset()
                val itemHeight = height / 3f
                val shift = scrolledHeight % itemHeight
                var topPos = (scrolledHeight / itemHeight).toInt()
                if (shift > 0) {
                    topPos += 1
                }
                smoothScrollToPosition(topPos)
                mSelectedPos = topPos + 1
            }
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        var size = MeasureSpec.getSize(heightSpec)
        mItemHeight = size / 3
        size = mItemHeight * 3
        setMeasuredDimension(MeasureSpec.getSize(widthSpec),MeasureSpec.getSize(size))
    }
}

class WJLinearLayoutManager : LinearLayoutManager {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(recyclerView!!.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                // 返回：滑过1px时经历的时间(ms)。
                return 150f / displayMetrics.densityDpi
            }

            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                return boxStart - viewStart
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)

    }
}

class StringPickerAdapter(val itemHeight: Int, dataSet: List<String>) : RecyclerView.Adapter<StringPickerViewHolder>() {

    private var mDataSet: List<String> = dataSet

    fun setDataSet(dataSet: List<String>) {
        mDataSet = dataSet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringPickerViewHolder {
        val textView = TextView(parent.context)
        textView.setTextColor(Color.BLACK)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        textView.gravity = Gravity.CENTER
        textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        return StringPickerViewHolder(textView)
    }

    override fun onBindViewHolder(holder: StringPickerViewHolder, position: Int) {
        holder.textView.text = mDataSet[position]

    }

    override fun getItemCount(): Int {
        return mDataSet.size
    }
}

class StringPickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView: TextView = itemView as TextView
}