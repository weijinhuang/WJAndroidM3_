package com.wj.androidm3.business.ui.recyclerview

import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel

class RecyclerViewPractiseViewModel : BaseViewModel() {

    val mOriginalList = mutableListOf<RecyclerViewPractiseItem>()

    var mCurrentIndex = 0

    init {
        mOriginalList.addAll(getData())
    }

    fun getData(): MutableList<RecyclerViewPractiseItem> {
        val resultList = mutableListOf<RecyclerViewPractiseItem>()
        for (i in 0..3) {
            val item = RecyclerViewPractiseItem(mCurrentIndex, "name:$mCurrentIndex")
            WJLog.d("create item : $item")
            resultList.add(item)
            mCurrentIndex++
        }
        return resultList
    }
}