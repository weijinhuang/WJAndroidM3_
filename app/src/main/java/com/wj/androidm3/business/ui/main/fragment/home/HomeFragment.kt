package com.wj.androidm3.business.ui.main.fragment.home

import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentHomeBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

class HomeFragment : BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {


    override fun firstCreateView() {
        mViewBinding?.viewModel = mViewModel
        mViewModel.getSystemInfo()
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }
}