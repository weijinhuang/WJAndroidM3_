package com.wj.androidm3.business.ui.main.fragment.dashboard

import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.wj.androidm3.R
import com.wj.androidm3.business.services.BackgroundService
import com.wj.androidm3.databinding.FragmentDashboardBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

class DashboardFragment : BaseMVVMFragment<DashboardViewModel, FragmentDashboardBinding>() {

    private val mFunctionList = listOf(
        FunctionBean("startNormalService") {
            val serviceIntent = Intent(requireActivity(), BackgroundService::class.java)
            requireActivity().startService(serviceIntent)
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
        return R.layout.fragment_dashboard
    }
}