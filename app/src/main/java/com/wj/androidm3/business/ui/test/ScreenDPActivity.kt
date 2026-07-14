package com.wj.androidm3.business.ui.test

import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityScreenDpBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel

/**
 *@Create by H.W.J 2022/9/27/027
 */
class ScreenDPActivity: BaseMVVMActivity<BaseViewModel, ActivityScreenDpBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.activity_screen_dp
    }
}
