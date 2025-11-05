package com.wj.androidm3.business.ui.test

import android.os.Bundle
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.bean.User
import com.wj.androidm3.databinding.TestBindadapterBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel

/**
 *@Create by H.W.J 2024/12/20/020
 */
class TestBindingAdapterActivity: BaseMVVMActivity<BaseViewModel, TestBindadapterBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.test_bindadapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = User("张三","19")
        mViewBinding?.user = user
    }

}