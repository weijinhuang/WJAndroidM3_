package com.wj.androidm3.business.ui

import com.wj.androidm3.R
import com.wj.androidm3.business.ui.anim.AnimationViewModel
import com.wj.androidm3.databinding.ActivityTestViewBinding
import com.wj.basecomponent.ui.BaseActivity
import com.wj.basecomponent.ui.BaseMVVMActivity

class TestViewActivity: BaseMVVMActivity<AnimationViewModel,ActivityTestViewBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.activity_test_view
    }

}