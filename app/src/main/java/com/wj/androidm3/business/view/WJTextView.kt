package com.wj.androidm3.business.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.wj.basecomponent.util.log.WJLog

class WJTextView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    val strs = listOf<String>("GPS坐标2", "5800.5700.2560.3440.516.14.2160.294.72.5", "GPS坐标1:。", "5500.5500.3000.4000.507.00.2160.309.99.5")

    val mPaint = TextPaint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var currentY = 100f
        val currentX = 100f
        mPaint.setColor(Color.BLUE)
        canvas?.let { cv ->
            mPaint.color = Color.BLUE
            mPaint.textSize = 40f
            canvas.translate(currentX, currentY)
            for (i in strs.indices) {
                val str = strs[i]
                val layout = if (Build.VERSION.SDK_INT >= 23) {
                    StaticLayout.Builder.obtain(str, 0, str.length, mPaint, width).build()
                } else {
                    StaticLayout(str, mPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true)
                }
                WJLog.d("layout.height -> ${layout.height}")
                canvas.translate(0f, layout.height.toFloat())
                layout.draw(canvas)
            }
        }
    }
}