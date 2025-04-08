package com.wj.androidm3.business.ui.media

import android.os.Environment
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.media.audio.MediaAdapter
import com.wj.androidm3.databinding.FragmentMediaListBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import java.io.File

class MediaListFragment : BaseMVVMFragment<MediaViewModel, FragmentMediaListBinding>() {


    private val mMediaAdapter = MediaAdapter()

    override fun createViewModel(attachActivity: Boolean): MediaViewModel {
        return super.createViewModel(true)
    }

    override fun firstCreateView() {
        mViewBinding?.run {
            recyclerView.adapter = mMediaAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)

            searchFile()
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_media_list
    }

    private fun searchFile() {
        val fileList = mutableListOf<File>()
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { searchFile(it, fileList) }
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { searchFile(it, fileList) }
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.let { searchFile(it, fileList) }
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { searchFile(it, fileList) }
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { searchFile(it, fileList) }
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { searchFile(it, fileList) }
        fileList.sortBy { file: File ->
            file.createNewFile()
        }
        mMediaAdapter.setSelectedFile(mViewModel.mFilePath)
        mMediaAdapter.setData(fileList)
        mMediaAdapter.setItemClickListener { file: File ->
            mViewModel.mFilePath = file.absolutePath
            WJLog.d("点击${file.absolutePath}，大小：${file.length()}")
            Toast.makeText(requireActivity(), "点击${file.nameWithoutExtension}，大小：${file.length()}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun searchFile(file: File, fileList: MutableList<File>) {
        WJLog.d("搜索：${file.absolutePath}")
        if (file.isFile) {
            fileList.add(file)
        } else {
            val childFiles = file.listFiles()
            childFiles.forEach { childFile ->
                searchFile(childFile, fileList)
            }
        }
    }

}