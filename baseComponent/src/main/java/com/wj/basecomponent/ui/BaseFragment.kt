package com.wj.basecomponent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.wj.basecomponent.ui.constraint.BaseUI
import com.wj.basecomponent.util.log.WJLog

abstract class BaseFragment : Fragment(), BaseUI {

    /**
     * 是否缓存View
     */
    open fun enableCacheView() = true

    var mRootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WJLog.d(">>${javaClass.simpleName}:onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null || !enableCacheView()) {
            mRootView = layoutInflater.inflate(getLayoutId(), container, false)
            firstCreateView()
        }
        return mRootView
    }

    abstract fun firstCreateView()

    override fun showLoadingProgress(cancelable: Boolean) {
        if (isAdded) {
            val hostAct = requireActivity()
            if (hostAct is BaseUI) {
                hostAct.showLoadingProgress(cancelable)
            }
        }
    }

    override fun dismissLoadingProgressDialog() {
        if (isAdded) {
            val hostAct = requireActivity()
            if (hostAct is BaseUI) {
                hostAct.dismissLoadingProgressDialog()
            }
        }
    }


}