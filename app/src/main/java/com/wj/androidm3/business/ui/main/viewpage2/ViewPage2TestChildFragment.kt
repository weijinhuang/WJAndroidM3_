package com.wj.androidm3.business.ui.main.viewpage2

import com.wj.androidm3.R
import com.wj.androidm3.business.ui.anim.AnimationViewModel
import com.wj.androidm3.databinding.FragmentViewpage2TestChildBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog

/**
 *@Create by H.W.J 2022/11/1/001
 */
class ViewPage2TestChildFragment(private val name: String) : BaseMVVMFragment<AnimationViewModel, FragmentViewpage2TestChildBinding>() {

    override fun firstCreateView() {

        mViewBinding?.contentTv?.text = "Fragment -> $name"
    }

    fun getTextView() = mViewBinding?.contentTv

    override fun onResume() {
        super.onResume()
        WJLog.i("${name}   -> onResume")
    }

    override fun onPause() {
        super.onPause()
        WJLog.i("${name}   -> onPause")
    }

    override fun onStop() {
        super.onStop()
        WJLog.i("${name}   -> onStop")
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_viewpage2_test_child
    }
}