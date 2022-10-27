package com.wj.androidm3.business.ui.kotlintest

import android.os.Bundle
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityKotlinTestBinding
import com.wj.basecomponent.ui.BaseMVVMActivity

class KotlinTestActivity : BaseMVVMActivity<KotlinTestViewModel, ActivityKotlinTestBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_kotlin_test
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.startMainTask()
        mViewModel.startIOTask()
    }
}