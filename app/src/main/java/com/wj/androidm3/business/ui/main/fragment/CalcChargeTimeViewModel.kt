package com.wj.androidm3.business.ui.main.fragment

import android.icu.util.Calendar
import androidx.databinding.Bindable
import com.wj.androidm3.BR
import com.wj.basecomponent.vm.BaseViewModel

/**
 *@Create by H.W.J 2024/10/21/021
 */
class CalcChargeTimeViewModel : BaseViewModel() {

    @get:Bindable
    var startHour: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.startHour)
        }

    @get:Bindable
    var startMinute: Int = 5
        set(value) {
            field = value
            notifyPropertyChanged(BR.startMinute)
        }

    private var mCalendar = Calendar.getInstance()



    @get:Bindable
    var endHour: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.endHour)
        }

    @get:Bindable
    var endMinute: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.endMinute)
        }

    @get:Bindable
    var startPercent: Int = 25
        set(value) {
            field = value
            notifyPropertyChanged(BR.startPercent)
        }

    @get:Bindable
    var endPercent: Int = 90
        set(value) {
            field = value
            notifyPropertyChanged(BR.endPercent)
        }

    private var mMinutePerOne = 3.9f



    fun calcStartTime() {
        val socDiff = endPercent - startPercent
        if (socDiff <= 0) {
            mErrorMD.postValue("结束SOC必须大于开始SOC")
        } else {
            val chargeMinute  = (mMinutePerOne * socDiff).toInt()

            mCalendar.set(Calendar.HOUR,endHour)
            mCalendar.set(Calendar.MINUTE, endMinute)

            val currentTime = mCalendar.timeInMillis
            val newTime = currentTime - (chargeMinute * 60000)
            mCalendar.timeInMillis = newTime
            startHour = mCalendar.get(Calendar.HOUR)
            startMinute = mCalendar.get(Calendar.MINUTE)

        }
    }
    fun calcEndTime() {
        val socDiff = endPercent - startPercent
        if (socDiff <= 0) {
            mErrorMD.postValue("结束SOC必须大于开始SOC")
        } else {
            val chargeMinute  = (mMinutePerOne * socDiff).toInt()

            mCalendar.set(Calendar.HOUR,startHour)
            mCalendar.set(Calendar.MINUTE, startMinute)

            val startTime = mCalendar.timeInMillis
            val newTime = startTime + (chargeMinute * 60000)
            mCalendar.timeInMillis = newTime
            endHour = mCalendar.get(Calendar.HOUR)
            endMinute = mCalendar.get(Calendar.MINUTE)

        }
    }
}