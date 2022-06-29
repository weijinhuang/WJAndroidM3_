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
        fun e(msg: String) {
            Timber.e(msg)
        }

        fun i(msg: String) {
            Timber.i(msg)
        }

        fun i() {
            val stackTrace = Thread.currentThread().stackTrace
            if (stackTrace.size > 2) {
                val parentMethod = stackTrace[2]
                i("${parentMethod.className}.${parentMethod.methodName}:${parentMethod.lineNumber} -> invoke")
            }
        }

    }


}