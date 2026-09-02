package com.wj.androidm3.business.ui.main.fragment.home

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.wj.androidm3.R
import com.wj.androidm3.business.countdown.data.CountdownPreferences
import com.wj.androidm3.business.countdown.data.CountdownRepository
import com.wj.androidm3.business.countdown.overlay.CountdownOverlayService
import com.wj.androidm3.databinding.FragmentHomeBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.nativelib.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {
    private val preferences by lazy { CountdownPreferences(requireContext()) }
    private val repository by lazy { CountdownRepository.getInstance(requireContext()) }
    private var ignoreSwitchChange = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                requireContext(),
                "未授予通知权限，倒计时仍可运行，但结束提醒可能不可见",
                Toast.LENGTH_LONG
            ).show()
        }
        requestOverlayPermissionOrEnable()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || Settings.canDrawOverlays(requireContext())) {
            completeEnable()
        } else {
            preferences.assistantEnabled = false
            updateSwitchAndStatus()
            Toast.makeText(requireContext(), "需要悬浮窗权限才能开启助手", Toast.LENGTH_SHORT).show()
        }
    }


    override fun firstCreateView() {
        mViewBinding?.viewModel = mViewModel
        mViewModel.getSystemInfo()
        mViewModel.getDensityInfo(requireActivity())
        WJLog.d("----${NativeLib().ffmpegVersion()}----")
        mViewBinding?.textDensity?.let {
            val params = it.layoutParams
            if (params is ConstraintLayout.LayoutParams) {
                WJLog.i("params.endToEnd:${params.endToEnd}")
                WJLog.i("params.endToEnd:${params.startToStart}")
                WJLog.i("params.topToBottom:${params.topToBottom}")
                WJLog.i("params.bottomToBottom:${params.bottomToBottom}")
            }
        }
        dir()
        WJLog.d("{NativeLib().ffmpegVersion()->${NativeLib().ffmpegVersion()}")

        mViewBinding?.countdownAssistantSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreSwitchChange) return@setOnCheckedChangeListener
            if (isChecked) beginEnableFlow() else requestDisable()
        }
        updateSwitchAndStatus()
    }

    override fun onResume() {
        super.onResume()
        if (mViewBinding != null) updateSwitchAndStatus()
    }

    private fun beginEnableFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestOverlayPermissionOrEnable()
        }
    }

    private fun requestOverlayPermissionOrEnable() {
        if (Settings.canDrawOverlays(requireContext())) {
            completeEnable()
            return
        }
        val packageUri = "package:${requireContext().packageName}".toUri()
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
        try {
            overlayPermissionLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun completeEnable() {
        preferences.assistantEnabled = true
        CountdownOverlayService.start(requireContext())
        updateSwitchAndStatus()
    }

    private fun requestDisable() {
        mViewBinding?.countdownAssistantSwitch?.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val hasRunning = withContext(Dispatchers.IO) { repository.hasRunningTasks() }
            mViewBinding?.countdownAssistantSwitch?.isEnabled = true
            if (hasRunning) {
                Toast.makeText(
                    requireContext(),
                    "请先暂停或删除所有进行中的倒计时",
                    Toast.LENGTH_SHORT
                ).show()
                updateSwitchAndStatus(forceChecked = true)
            } else {
                preferences.assistantEnabled = false
                CountdownOverlayService.stop(requireContext())
                updateSwitchAndStatus()
            }
        }
    }

    private fun updateSwitchAndStatus(forceChecked: Boolean? = null) {
        val hasOverlayPermission = Settings.canDrawOverlays(requireContext())
        val enabled = forceChecked ?: (preferences.assistantEnabled && hasOverlayPermission)
        ignoreSwitchChange = true
        mViewBinding?.countdownAssistantSwitch?.isChecked = enabled
        ignoreSwitchChange = false
        mViewBinding?.countdownAssistantStatus?.text = when {
            preferences.assistantEnabled && !hasOverlayPermission -> "悬浮窗权限已失效，请重新开启并授权"
            enabled -> "已开启 · 点击屏幕边缘的悬浮按钮使用"
            else -> getString(R.string.countdown_assistant_summary)
        }
    }

    fun dir() {
        WJLog.d(" Activity().externalCacheDir ${requireActivity().externalCacheDir}")
        WJLog.d(" Activity.externalCacheDirs: ${requireActivity().externalCacheDirs}")
        WJLog.d(" Activity.getExternalFilesDir.DIRECTORY_MUSIC: ${requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)}")
        WJLog.d(" Activity.externalCacheDir: ${requireActivity().externalCacheDir}")
        WJLog.d(" Environment.getDataDirectory: ${Environment.getDataDirectory().absolutePath}")
        WJLog.d(" Environment.getDownloadCacheDirectory: ${Environment.getDownloadCacheDirectory().absolutePath}")
        WJLog.d(" Environment.getExternalStorageDirectory: ${Environment.getExternalStorageDirectory().absolutePath}")
        WJLog.d(" Environment.getExternalStoragePublicDirectory: ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath}")
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }
}
