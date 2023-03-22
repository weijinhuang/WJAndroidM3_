package com.wj.androidm3.business.ui.main.fragment.home

import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentHomeBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.nativelib.NativeLib

class HomeFragment : BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {


    override fun firstCreateView() {
        mViewBinding?.viewModel = mViewModel
        mViewModel.getSystemInfo()
        mViewModel.getDensityInfo(requireActivity())
        WJLog.d("{NativeLib().ffmpegVersion()->${NativeLib().ffmpegVersion()}")
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }
}