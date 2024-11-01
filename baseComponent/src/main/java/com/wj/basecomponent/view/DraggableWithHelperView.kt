package com.wj.basecomponent.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.customview.widget.ViewDragHelper
import com.wj.basecomponent.util.log.WJLog.Companion.d

class DraggableWithHelperView : FrameLayout {
    private var mDragHelper: ViewDragHelper? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    private fun init() {
        mDragHelper = ViewDragHelper.create(this, 1f, DragCallback())
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return mDragHelper!!.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mDragHelper!!.processTouchEvent(event)
        return true
    }

    private inner class DragCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            d("--tryCaptureView:$pointerId:Int")
            if (child is TextView) {
                if (child.text == "2")
                    return true
            }
            return false
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            d("--onViewDragStateChanged($state: Int)")
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            d("--onViewPositionChanged(changedView, left:$left, top:$top, x:$dx, y:$dy)")
            changedView.layout(left, top, left + changedView.width, top + changedView.height)
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            super.onViewCaptured(capturedChild, activePointerId)
            d("--onViewCaptured(capturedChild: View, $activePointerId: Int)")
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            d("--onViewReleased(releasedChild: View, $xvel: Float, $yvel: Float)")
        }

        override fun onEdgeTouched(edgeFlags: Int, pointerId: Int) {
            super.onEdgeTouched(edgeFlags, pointerId)
            d("--onEdgeTouched($edgeFlags: Int, $pointerId: Int)")
        }

        override fun onEdgeLock(edgeFlags: Int): Boolean {
            d("--onEdgeLock($edgeFlags: Int)")
            return super.onEdgeLock(edgeFlags)
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            d("-- onEdgeDragStarted($edgeFlags: Int, $pointerId: Int)")
            super.onEdgeDragStarted(edgeFlags, pointerId)
        }

        override fun getOrderedChildIndex(index: Int): Int {
            d("-- getOrderedChildIndex($index: Int)")
            return super.getOrderedChildIndex(index)
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            d("-- getViewHorizontalDragRange($child: Int)")
            return width
        }

        override fun getViewVerticalDragRange(child: View): Int {
            d("-- getViewVerticalDragRange($child: Int)")
            return height
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            d("--clampViewPositionHorizontal(child: View, $left: Int, $dx: Int)")
            return super.clampViewPositionHorizontal(child, left, dx)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            d("--clampViewPositionVertical(child: View, $top: Int, $dy: Int)")
            return super.clampViewPositionVertical(child, top, dy)
        }
    }
}