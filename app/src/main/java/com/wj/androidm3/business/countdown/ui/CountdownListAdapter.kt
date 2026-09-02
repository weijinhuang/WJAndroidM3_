package com.wj.androidm3.business.countdown.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownScreenshotStore
import com.wj.androidm3.business.countdown.data.CountdownStatus
import com.wj.androidm3.business.countdown.data.CountdownTime
import com.wj.androidm3.databinding.ItemCountdownBinding

class CountdownListAdapter(
    private val screenshotStore: CountdownScreenshotStore,
    private val onAction: (CountdownEntity) -> Unit,
    private val onScreenshot: (CountdownEntity) -> Unit,
    private val onDelete: (CountdownEntity) -> Unit
) : ListAdapter<CountdownEntity, CountdownListAdapter.CountdownViewHolder>(DiffCallback) {
    private var nowEpochMs: Long = System.currentTimeMillis()

    fun updateClock(now: Long) {
        nowEpochMs = now
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, CLOCK_PAYLOAD)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountdownViewHolder {
        val binding = ItemCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountdownViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountdownViewHolder, position: Int) {
        holder.bind(getItem(position), nowEpochMs)
    }

    override fun onBindViewHolder(holder: CountdownViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(CLOCK_PAYLOAD)) {
            holder.updateTime(getItem(position), nowEpochMs)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class CountdownViewHolder(
        private val binding: ItemCountdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: CountdownEntity, now: Long) {
            binding.nameText.text = task.displayName
            binding.statusText.text = when (task.status) {
                CountdownStatus.PENDING -> "未开始"
                CountdownStatus.RUNNING -> "进行中"
                CountdownStatus.PAUSED -> "已暂停"
                CountdownStatus.COMPLETED -> "已结束"
            }
            binding.actionButton.visibility = if (task.status == CountdownStatus.COMPLETED) View.GONE else View.VISIBLE
            binding.actionButton.text = when (task.status) {
                CountdownStatus.PENDING -> "开始"
                CountdownStatus.RUNNING -> "暂停"
                CountdownStatus.PAUSED -> "继续"
                CountdownStatus.COMPLETED -> ""
            }
            binding.actionButton.setOnClickListener { onAction(task) }
            val thumbnailSizePx = (150 * binding.root.resources.displayMetrics.density).toInt()
            val thumbnail = task.screenshotPath?.let { path ->
                screenshotStore.decodeSampled(path, thumbnailSizePx, thumbnailSizePx)
            }
            binding.screenshotThumbnail.setImageBitmap(thumbnail)
            binding.screenshotThumbnail.visibility = if (thumbnail == null) View.INVISIBLE else View.VISIBLE
            binding.screenshotThumbnail.isClickable = thumbnail != null
            binding.screenshotThumbnail.setOnClickListener {
                if (thumbnail != null) onScreenshot(task)
            }
            binding.deleteButton.setOnClickListener { onDelete(task) }
            updateTime(task, now)
        }

        fun updateTime(task: CountdownEntity, now: Long) {
            binding.remainingText.text = CountdownTime.format(task, now)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CountdownEntity>() {
        override fun areItemsTheSame(oldItem: CountdownEntity, newItem: CountdownEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CountdownEntity, newItem: CountdownEntity): Boolean =
            oldItem == newItem
    }

    companion object {
        private const val CLOCK_PAYLOAD = "clock"
    }
}
