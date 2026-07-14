package com.wj.androidm3.business.ui.anim

import android.os.Bundle
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityAnimationBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel

class AnimationActivity : BaseMVVMActivity<BaseViewModel, ActivityAnimationBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_animation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, AnimationFragment.newInstance())
                .commitNow()
        }
    }
}
