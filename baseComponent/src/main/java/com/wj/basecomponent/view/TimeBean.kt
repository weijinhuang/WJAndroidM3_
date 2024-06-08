package com.wj.basecomponent.view

import android.graphics.Color

class TimeBean(val startTime: Long, val endTime: Long) : IVideo {

    var mColor = Color.BLUE
    override fun getBeginTimeInMs(): Long {
        return startTime
    }

    override fun getEndTimeInMs(): Long {
        return endTime
    }

    override fun getColor(): Int {
        return mColor
    }


}
