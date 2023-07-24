package com.wj.androidm3.business.ui.main.fragment.home

import android.os.Environment
import androidx.constraintlayout.widget.ConstraintLayout
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