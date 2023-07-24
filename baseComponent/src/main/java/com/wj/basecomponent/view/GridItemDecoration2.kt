package com.wj.basecomponent.view

import android.graphics.*
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wj.basecomponent.BaseApplication
import com.wj.basecomponent.util.dp2Pixel

/**
 *@Create by H.W.J 2023/6/25/025
 */
class GridItemDecoration2(spaceInDp: Float) : RecyclerView.ItemDecoration() {

    private val mPaint: Paint = Paint()

    private val spaceInPx: Int

    private val mPath: Path

    init {
        spaceInPx = dp2Pixel(BaseApplication.INSTANCE, spaceInDp.toInt()).toInt()
        mPaint.strokeWidth = spaceInPx.toFloat()
        mPaint.style = Paint.Style.STROKE
        mPath = Path()
    }

    private var mSelectedPos = 0

    private var mSelectedColor = Color.RED

    private var mUnselectedColor = Color.WHITE

    fun setSelectedPos(selectedPos: Int) {
        mSelectedPos = selectedPos
    }

    fun setSelectedColor(selectedColor: Int) {
        mSelectedColor = selectedColor
    }

    fun setUnselectedColor(unselectedColor: Int) {
        mUnselectedColor = unselectedColor
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        parent.adapter?.let { adapter ->
            val itemCount = parent.childCount
            if (itemCount > 0) {
                val layoutManager = parent.layoutManager
                val selectedRect = RectF()
                mPaint.color = mUnselectedColor
                mPaint.strokeWidth = spaceInPx.toFloat()
                for (index in 0 until itemCount) {
                    val itemView = parent.getChildAt(index)
                    val position = layoutManager?.getPosition(itemView)
                    val left: Float
                    val top: Float
                    val right: Float
                    val bottom: Float
                    if (position == mSelectedPos) {
                        left = itemView.left.toFloat()
                        if (position % 2 == 0) {
                            top = itemView.top.toFloat() + spaceInPx
                            bottom = itemView.bottom.toFloat()
                        } else {
                            top = itemView.top.toFloat()
                            bottom = itemView.bottom.toFloat() - spaceInPx
                        }
                        right = itemView.right.toFloat()
                        selectedRect.set(left, top, right, bottom)
                    } else {
                        left = itemView.left.toFloat()
                        top = itemView.top.toFloat()
                        right = itemView.right.toFloat()
                        bottom = itemView.bottom.toFloat()
                        mPath.reset()
                        mPath.moveTo(left, top)
                        mPath.lineTo(right, top)
                        mPath.lineTo(right, bottom)
                        mPath.lineTo(left, bottom)
                        c.drawPath(mPath, mPaint)
                    }
                }
                mPaint.color = mSelectedColor
                mPaint.strokeWidth = spaceInPx * 1.5f
                c.drawRect(selectedRect, mPaint)

            }
        }
    }
}