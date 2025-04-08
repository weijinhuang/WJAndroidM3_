package com.wj.androidm3.business.ui.media.audio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.databinding.ItemMediaListBinding
import java.io.File

class MediaAdapter : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private var mFileList = mutableListOf<File>()

    private var mItemClickListener: (File) -> Unit = {}

    private var mSelectedFile: String? = null

    fun setSelectedFile(fileName: String) {
        mSelectedFile = fileName
    }

    fun setItemClickListener(itemClickListener: (File) -> Unit) {
        mItemClickListener = itemClickListener
    }


    fun setData(file: MutableList<File>) {
        mFileList = file
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return mFileList.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        mFileList[position].let { file: File ->
            holder.rootBinding.tvFileName.text = file.name

            if (file.absolutePath == mSelectedFile) {
                holder.rootBinding.tvFileName.setTextColor(Color.BLUE)
            } else {
                holder.rootBinding.tvFileName.setTextColor(Color.BLACK)
            }

            holder.rootBinding.tvFileName.setOnClickListener {
                mItemClickListener.invoke(file)
                mSelectedFile = file.name
                notifyDataSetChanged()
            }


            holder.rootBinding.deleteBtn.setOnClickListener {
                file.delete()
                mFileList.remove(file)
                notifyItemRemoved(holder.adapterPosition)
            }
        }
    }


    inner class MediaViewHolder(val rootBinding: ItemMediaListBinding) : RecyclerView.ViewHolder(rootBinding.root) {
    }
}