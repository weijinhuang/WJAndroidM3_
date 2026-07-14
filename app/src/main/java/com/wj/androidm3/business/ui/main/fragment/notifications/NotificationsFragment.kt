package com.wj.androidm3.business.ui.main.fragment.notifications

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentNotificationsBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.permission.canDrawOverlays
import com.wj.basecomponent.view.TimeBean
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NotificationsFragment : BaseMVVMFragment<NotificationsViewModel, FragmentNotificationsBinding>() {

    private val binding: FragmentNotificationsBinding
        get() = requireNotNull(mViewBinding)

    override fun enableCacheView() = false

    private val requestDrawOverlays = registerForActivityResult(object : ActivityResultContract<Unit, Unit>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            WJLog.d("onActivityResultCallback -> parseResult:$resultCode")
            binding.swDrawOverlays.isChecked = canDrawOverlays(requireActivity())
        }
    }) {
        WJLog.i("onActivityResultCallback -> invoke")
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_notifications
    }

    override fun firstCreateView() = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textView = binding.textNotifications
        mViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        setDrawOverlays()
        testTimeRuler()
    }

    private fun testTimeRuler() {
        val timeData = ArrayList<TimeBean>(10)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        for (i in 0 until 10) {
            val instance = Calendar.getInstance()
            instance.set(Calendar.HOUR, i)
            instance.set(Calendar.MINUTE, i)
            val startTime = instance.timeInMillis
            instance.set(Calendar.MINUTE, i * 5)
            val endTime = instance.timeInMillis
            val timeBean = TimeBean(startTime, endTime)
            if (i % 2 == 0) {
                timeBean.mColor = Color.RED
            }
            timeData.add(timeBean)
            WJLog.d("TimeData $i -> ${simpleDateFormat.format(startTime)}:${simpleDateFormat.format(endTime)}")
        }
        binding.timeRuler.setData(timeData)
    }

    private fun setDrawOverlays() {
        binding.swDrawOverlays.isChecked = canDrawOverlays(requireActivity())
        binding.swDrawOverlays.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) {
//                if (!canDrawOverlays(requireActivity())) {
            requestDrawOverlays.launch(Unit)
//                }
//            }
        }
        binding.timeRuler.setOnTimeSelectListener { time, timeZone ->
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            simpleDateFormat.timeZone = timeZone
            WJLog.d("onTimeSelected -> $time -> ${simpleDateFormat.format(time)}")
        }
        binding.timeRuler.setCurrentTime(System.currentTimeMillis())
    }
}
