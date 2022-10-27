package com.wj.androidm3.business.ui.kotlintest

import androidx.recyclerview.widget.LinearLayoutManager
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.main.fragment.dashboard.DashboardRecyclerViewAdapter
import com.wj.androidm3.business.ui.main.fragment.dashboard.FunctionBean
import com.wj.androidm3.databinding.FragmentKotlinTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

class KotlinTestFragment : BaseMVVMFragment<KotlinTestViewModel, FragmentKotlinTestBinding>() {


    private val mFunctionList = listOf(
        FunctionBean("startMainTask") {
            mViewModel.startMainTask()
        },
        FunctionBean("startIOTask") {
            mViewModel.startIOTask()
        }
    )


    override fun firstCreateView() {
        val adapter = DashboardRecyclerViewAdapter(mFunctionList)
        mViewBinding?.run {
            functionRv.adapter = adapter
            functionRv.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_kotlin_test
    }

}