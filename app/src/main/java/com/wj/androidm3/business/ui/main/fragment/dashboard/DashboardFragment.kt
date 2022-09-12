package com.wj.androidm3.business.ui.main.fragment.dashboard

import android.content.Intent
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.wj.androidm3.R
import com.wj.androidm3.business.services.BackgroundService
import com.wj.androidm3.business.ui.conversationincome.PhoneConversationActivity
import com.wj.androidm3.databinding.FragmentDashboardBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.notification.sendNotification

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
        },
        FunctionBean("HeadsUpNotification") {
            sendNotification(
                requireActivity(),
                PhoneConversationActivity::class.java,
                "HWJ",
                1,
                R.mipmap.ic_launcher_round,
                "Title",
                "This is a heads up notification"
            )
        },
        FunctionBean("Get current FCM token") {
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (!it.isSuccessful) {
                    WJLog.e("Get current FCM token false : ${it.exception?.message ?: "error"}")
                }
                WJLog.i("current FCM token -> ${it.result}")
            }
        },
        FunctionBean("Install Google Play") {
            GoogleApiAvailability().makeGooglePlayServicesAvailable(requireActivity())
        },
        FunctionBean("Native Test") {
//            WJLog.i("NativeLib().stringFromJNI() -> ${NativeLib().stringFromJNI()}")
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