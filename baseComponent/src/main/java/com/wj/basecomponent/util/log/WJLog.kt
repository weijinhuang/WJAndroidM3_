package com.wj.basecomponent.util.log

import timber.log.Timber

class WJLog {

    companion object {

        val TAG = "WJAndroid"

        @JvmStatic
        val ENABLE = true

        fun d(msg: String) {
            Timber.d(msg)
        }


    }


}