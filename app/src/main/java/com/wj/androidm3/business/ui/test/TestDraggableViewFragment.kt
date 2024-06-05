package com.wj.androidm3.business.ui.test

import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentTestDraggableviewBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.vm.BaseViewModel

class TestDraggableViewFragment : BaseMVVMFragment<BaseViewModel, FragmentTestDraggableviewBinding>() {
    override fun firstCreateView() {

    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_test_draggableview
    }
}