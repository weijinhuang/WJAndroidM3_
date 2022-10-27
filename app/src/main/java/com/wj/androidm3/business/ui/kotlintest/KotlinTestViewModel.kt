package com.wj.androidm3.business.ui.kotlintest

import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.delay

class KotlinTestViewModel : BaseViewModel() {

    fun startMainTask() {
        launch {
            repeat(5) {
                WJLog.d("currentThread : ${Thread.currentThread().name}")
                delay(1000)
            }

        }
    }

    fun startIOTask() {
        launchBackground {
            repeat(5) {
                WJLog.d("currentThread : ${Thread.currentThread().name}")
                delay(1000)
            }

        }
    }


}