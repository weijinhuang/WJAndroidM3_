package com.wj.androidm3.business.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.databinding.ItemRecyclerviewPractiseBinding
import com.wj.basecomponent.util.log.WJLog

class RecyclerViewPractiseAdapter : ListAdapter<RecyclerViewPractiseItem, RecyclerViewPractiseAdapter.RecyclerViewPractiseViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecyclerViewPractiseItem>() {
            override fun areItemsTheSame(oldItem: RecyclerViewPractiseItem, newItem: RecyclerViewPractiseItem): Boolean {
                val areItemsTheSame = oldItem == newItem
                WJLog.d("areItemsTheSame：$areItemsTheSame (oldItem:$oldItem, newItem:$newItem)")
                return areItemsTheSame
            }

            override fun areContentsTheSame(oldItem: RecyclerViewPractiseItem, newItem: RecyclerViewPractiseItem): Boolean {
                WJLog.i("areContentsTheSame: (oldItem:$oldItem, newItem:$newItem)")
                return newItem.areContentsTheSame
            }

            override fun getChangePayload(oldItem: RecyclerViewPractiseItem, newItem: RecyclerViewPractiseItem): Any? {
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }

    inner class RecyclerViewPractiseViewHolder(val binding: ItemRecyclerviewPractiseBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewPractiseViewHolder {
        return RecyclerViewPractiseViewHolder(ItemRecyclerviewPractiseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerViewPractiseViewHolder, position: Int) {
        val adapterPosition = holder.adapterPosition
        val oldPosition = holder.oldPosition
        val layoutPosition = holder.layoutPosition
        getItem(position).let { item ->
            holder.binding.root.setOnClickListener {
                WJLog.d("点击： $item position:$position adapterPosition:$adapterPosition oldPosition:$oldPosition layoutPosition:$layoutPosition")
                WJLog.d("点击2： $item position:$position adapterPosition:${holder.adapterPosition} oldPosition:${holder.oldPosition} layoutPosition:${holder.layoutPosition}")

            }
            holder.binding.tvId.text = "${item.id}"
            holder.binding.tvName.text = item.name
        }
    }
}