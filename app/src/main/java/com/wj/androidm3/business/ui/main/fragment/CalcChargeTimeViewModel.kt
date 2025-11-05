package com.wj.androidm3.business.ui.main.fragment

import android.icu.util.Calendar
import androidx.databinding.Bindable
import com.wj.androidm3.BR
import com.wj.basecomponent.util.SPUtils
import com.wj.basecomponent.vm.BaseViewModel


const val START_HOUR = "START_HOUR"
const val START_MINUTE = "START_MINUTE"
const val END_HOUR = "END_HOUR"
const val END_MINUTE = "END_MINUTE"
const val START_SOC = "START_SOC"
const val END_SOC = "END_SOC"

const val CALC_START = "CALC_START"

/**
 *@Create by H.W.J 2024/10/21/021
 */
class CalcChargeTimeViewModel : BaseViewModel() {

    var calcStart = SPUtils.getInstance().getBoolean(CALC_START, true)

    @get:Bindable
    var startHour: Int = SPUtils.getInstance().getInt(START_HOUR)
        set(value) {
            field = value
            notifyPropertyChanged(BR.startHour)
        }

    @get:Bindable
    var startMinute: Int = SPUtils.getInstance().getInt(START_MINUTE)
        set(value) {
            field = value
            notifyPropertyChanged(BR.startMinute)
        }

    private var mCalendar = Calendar.getInstance()


    @get:Bindable
    var endHour: Int = SPUtils.getInstance().getInt(END_HOUR)
        set(value) {
            field = value
            notifyPropertyChanged(BR.endHour)
        }

    @get:Bindable
    var endMinute: Int = SPUtils.getInstance().getInt(END_MINUTE)
        set(value) {
            field = value
            notifyPropertyChanged(BR.endMinute)
        }

    @get:Bindable
    var startPercent: Int = SPUtils.getInstance().getInt(START_SOC)
        set(value) {
            field = value
            notifyPropertyChanged(BR.startPercent)
        }

    @get:Bindable
    var endPercent: Int = SPUtils.getInstance().getInt(END_SOC)
        set(value) {
            field = value
            notifyPropertyChanged(BR.endPercent)
        }

    private var mMinutePerOne = 3.4f


    fun calcStartTime() {
        calcStart = true
        SPUtils.getInstance().getBoolean(CALC_START, calcStart)
        val socDiff = endPercent - startPercent
        if (socDiff <= 0) {
            mErrorMD.postValue("结束SOC必须大于开始SOC")
        } else {
            val chargeMinute = (mMinutePerOne * socDiff).toInt()

            mCalendar.set(Calendar.HOUR, endHour)
            mCalendar.set(Calendar.MINUTE, endMinute)

            val currentTime = mCalendar.timeInMillis
            val newTime = currentTime - (chargeMinute * 60_000)
            mCalendar.timeInMillis = newTime
            startHour = mCalendar.get(Calendar.HOUR)
            startMinute = mCalendar.get(Calendar.MINUTE)
            SPUtils.getInstance().putInt(START_HOUR, startHour)
            SPUtils.getInstance().putInt(START_MINUTE, startMinute)

        }
    }

    fun calcEndTime() {
        calcStart = false
        SPUtils.getInstance().getBoolean(CALC_START, calcStart)
        val socDiff = endPercent - startPercent
        if (socDiff <= 0) {
            mErrorMD.postValue("结束SOC必须大于开始SOC")
        } else {
            val chargeMinute = (mMinutePerOne * socDiff).toInt()

            mCalendar.set(Calendar.HOUR, startHour)
            mCalendar.set(Calendar.MINUTE, startMinute)

            val startTime = mCalendar.timeInMillis
            val newTime = startTime + (chargeMinute * 60000)
            mCalendar.timeInMillis = newTime
            endHour = mCalendar.get(Calendar.HOUR)
            endMinute = mCalendar.get(Calendar.MINUTE)
            SPUtils.getInstance().putInt(END_HOUR, endHour)
            SPUtils.getInstance().putInt(END_MINUTE, endMinute)

        }
    }
}