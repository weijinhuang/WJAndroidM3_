package com.wj.androidm3.business.ui.main.fragment.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wj.androidm3.BuildConfig
import com.wj.androidm3.R
import com.wj.androidm3.business.services.BackgroundService
import com.wj.androidm3.databinding.FragmentDashboardBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

class DashboardFragment : BaseMVVMFragment<DashboardViewModel, FragmentDashboardBinding>() {



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private val mFunctionList = listOf(
        FunctionBean("StartNormalService") {
            val serviceIntent = Intent(requireActivity(), BackgroundService::class.java)
            requireActivity().startService(serviceIntent)
        },
        FunctionBean("SystemAlarmDialog") {
            val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
            alertDialogBuilder.setMessage("This is a SystemAlarmDialog")
            alertDialogBuilder.setTitle("SystemAlarmDialog")
            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            alertDialogBuilder.setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            alertDialog.show()
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