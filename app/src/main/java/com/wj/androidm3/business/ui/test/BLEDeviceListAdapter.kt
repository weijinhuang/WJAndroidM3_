package com.wj.androidm3.business.ui.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble3.scan.ScanResult
import com.wj.androidm3.databinding.ItemBleBinding
import com.wj.basecomponent.util.log.WJLog

class BLEDeviceListAdapter : ListAdapter<ScanResult, BLEDeviceListAdapter.BLEViewHolder>(DIFF_UTIL) {

    private var mOnClickListener: (ScanResult) -> Unit = {}

    fun setOnClickListener(listener: (ScanResult) -> Unit) {
        mOnClickListener = listener
    }

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<ScanResult>() {
            override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
                val areItemsTheSame = oldItem.bleDevice.macAddress == newItem.bleDevice.macAddress
                WJLog.d("areItemsTheSame $areItemsTheSame ($oldItem: ScanResult, $newItem: ScanResult) ")
                return areItemsTheSame
            }

            override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
                return oldItem.rssi == newItem.rssi
            }
        }

    }

    inner class BLEViewHolder(val binding: ItemBleBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEViewHolder {
        return BLEViewHolder(ItemBleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BLEViewHolder, position: Int) {
        getItem(position).let { scanResult ->
            holder.binding.bleDeviceResult = scanResult
            holder.binding.root.setOnClickListener {
                mOnClickListener.invoke(scanResult)
            }
        }
    }
}