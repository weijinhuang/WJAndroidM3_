package com.wj.androidm3.business.ui.anim

import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentAnimBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

class AnimationFragment : BaseMVVMFragment<AnimationViewModel, FragmentAnimBinding>() {

    companion object {
        fun newInstance() = AnimationFragment()
    }

    private lateinit var viewModel: AnimationViewModel
    override fun firstCreateView() {
    }

    override fun getLayoutId() = R.layout.fragment_anim


}