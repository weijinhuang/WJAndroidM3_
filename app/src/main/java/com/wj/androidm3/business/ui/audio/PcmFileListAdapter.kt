package com.wj.androidm3.business.ui.audio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.databinding.ItemPcmFileBinding
import com.wj.basecomponent.util.log.WJLog
import java.io.File

class PcmFileListAdapter(
    private var files: List<File>,
    private val onSelect: (Int, File) -> Unit
) : RecyclerView.Adapter<PcmFileListAdapter.PcmFileViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var isPlaying: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PcmFileViewHolder {
        val binding = ItemPcmFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PcmFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PcmFileViewHolder, position: Int) {
        holder.bind(files[position], position, position == selectedPosition, isPlaying)
        holder.itemView.setOnClickListener {
            val old = selectedPosition
            selectedPosition = position
            WJLog.d("${files[holder.adapterPosition].absolutePath} :  ${files[holder.adapterPosition].length()}")
            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
            notifyItemChanged(position)
            onSelect(position, files[position])
            notifyItemChanged(selectedPosition)

        }
        holder.binding.playButton.setOnClickListener {
            files[position].deleteOnExit()
            files = files.filterIndexed { index, _ -> index != position }
            notifyItemRemoved(position)

        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFiles(newFiles: List<File>) {
        this.files = newFiles
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position)
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition)
    }

    class PcmFileViewHolder(val binding: ItemPcmFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, position: Int, selected: Boolean, isPlaying: Boolean) {
            binding.fileName.text = file.name
            binding.rootContainer.setBackgroundColor(
                if (selected) 0xFFE0F7FA.toInt() else 0x00000000
            )

        }
    }
}
