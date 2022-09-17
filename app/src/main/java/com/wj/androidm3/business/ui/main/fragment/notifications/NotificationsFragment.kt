package com.wj.androidm3.business.ui.main.fragment.notifications

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.wj.androidm3.databinding.FragmentNotificationsBinding
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.permission.canDrawOverlays
import com.wj.basecomponent.view.TimeBean
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this)[NotificationsViewModel::class.java]

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                timeBean.color = Color.RED
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}