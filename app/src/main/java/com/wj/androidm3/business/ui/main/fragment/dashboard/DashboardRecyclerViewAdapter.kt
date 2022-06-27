package com.wj.androidm3.business.ui.main.fragment.dashboard

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wj.basecomponent.util.dp2Pixel

class DashboardRecyclerViewAdapter(private val dataList: List<FunctionBean>) :
    RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardRecyclerViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardRecyclerViewHolder {
        val textView = TextView(parent.context)
        val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2Pixel(parent.context, 45).toInt())
        textView.gravity = Gravity.CENTER
        textView.layoutParams = lp
        return DashboardRecyclerViewHolder(textView)
    }

    override fun onBindViewHolder(holder: DashboardRecyclerViewHolder, position: Int) {
        holder.textView.text = dataList[position].name
        holder.textView.setOnClickListener { dataList[position].action.invoke() }
    }

    override fun getItemCount() = dataList.size

    inner class DashboardRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view as TextView

    }
}