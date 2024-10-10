package com.wj.androidm3.business.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.databinding.ItemRecyclerviewPractiseBinding
import com.wj.basecomponent.util.log.WJLog

class RecyclerViewPractiseAdapter2 : RecyclerView.Adapter<RecyclerViewPractiseAdapter2.RecyclerViewPractiseViewHolder2>() {

    var mList: MutableList<RecyclerViewPractiseItem> = mutableListOf()


    fun submitList(newList: MutableList<RecyclerViewPractiseItem>) {
        val callback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return mList.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = mList[oldItemPosition]
                val newItem = newList[newItemPosition]

                val areItemsTheSame = oldItem.id == newItem.id
                WJLog.d("areItemsTheSame $areItemsTheSame (oldItemPosition: $oldItemPosition $oldItem, -> newItemPosition: $newItemPosition $newItem)")
                return areItemsTheSame
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = mList[oldItemPosition]
                val newItem = newList[newItemPosition]
                WJLog.i("areContentsTheSame (oldItemPosition: $oldItemPosition $oldItem, -> newItemPosition: $newItemPosition $newItem)")
                return oldItem.areContentsTheSame && newItem.areContentsTheSame
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldItem = mList[oldItemPosition]
                val newItem = newList[newItemPosition]
                WJLog.i("areContentsTheSame (oldItemPosition: $oldItemPosition $oldItem, -> newItemPosition: $newItemPosition $newItem)")
                return mapOf("name" to newItem.name)
            }

        }
        val calculateDiff = DiffUtil.calculateDiff(callback, true)
        mList = newList
        calculateDiff.dispatchUpdatesTo(this)

    }

    inner class RecyclerViewPractiseViewHolder2(val binding: ItemRecyclerviewPractiseBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewPractiseViewHolder2 {
        return RecyclerViewPractiseViewHolder2(ItemRecyclerviewPractiseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return mList.size
    }


    private fun getItem(position: Int): RecyclerViewPractiseItem {
        return mList[position]
    }

    override fun onBindViewHolder(holder: RecyclerViewPractiseViewHolder2, position: Int, payloads: MutableList<Any>) {
        WJLog.d("onBindViewHolder(holder: RecyclerViewPractiseViewHolder2, $position: Int, $payloads: MutableList<Any>)")
        if(null == payloads || payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        }else{
            val first = payloads.first()
            if(first is Map<*,*>) {
                holder.binding.tvName.text = first["name"].toString()
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerViewPractiseViewHolder2, position: Int) {
        WJLog.d("onBindViewHolder(holder: RecyclerViewPractiseViewHolder2, $position: Int)")

        val adapterPosition = holder.adapterPosition
        val oldPosition = holder.oldPosition
        val layoutPosition = holder.layoutPosition

        getItem(position).let { item ->
            WJLog.d("onBindViewHolder $item position:$position adapterPosition:$adapterPosition oldPosition:$oldPosition layoutPosition:$layoutPosition")

            holder.binding.root.setOnClickListener {
                WJLog.d("点击： $item position:$position adapterPosition:$adapterPosition oldPosition:$oldPosition layoutPosition:$layoutPosition")
                WJLog.d("点击2： $item position:$position adapterPosition:${holder.adapterPosition} oldPosition:${holder.oldPosition} layoutPosition:${holder.layoutPosition}")

            }
            holder.binding.tvId.text = "${item.id}"
            holder.binding.tvName.text = item.name
        }
    }
}