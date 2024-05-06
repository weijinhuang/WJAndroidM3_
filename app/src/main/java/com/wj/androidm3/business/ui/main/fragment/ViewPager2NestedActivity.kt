package com.wj.androidm3.business.ui.main.fragment

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.wj.androidm3.R
import com.wj.basecomponent.ui.BaseActivity

class ViewPager2NestedActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_viewpager2_nested)
        val dataArray = Array(2) { it1 -> Array(4) { it2 -> "$it1: $it2" } }
        val adapter = OuterViewpagerAdapter(this, dataArray)
        val viewPager2 = findViewById<ViewPager2>(R.id.vp_outer)
        viewPager2?.adapter = adapter
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_viewpager2_nested
    }


}

class OuterViewpagerAdapter(val context: Context, val dataList: Array<Array<String>>) : RecyclerView.Adapter<OuterViewpagerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewpagerViewHolder {
        val recyclerView = RecyclerView(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recyclerView.layoutParams = layoutParams
        return OuterViewpagerViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: OuterViewpagerViewHolder, position: Int) {
        val data = dataList[position]
        val adapter = InnerViewpagerAdapter(data)
        holder.recyclerView.adapter = adapter
        holder.recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}

class OuterViewpagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val recyclerView = itemView as RecyclerView
}

class InnerViewpagerAdapter(val data: Array<String>) : RecyclerView.Adapter<InnerViewpagerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewpagerViewHolder {
        val textView = TextView(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        textView.layoutParams = layoutParams
        return InnerViewpagerViewHolder(textView)
    }

    override fun onBindViewHolder(holder: InnerViewpagerViewHolder, position: Int) {
        holder.textView.text = data[position]
    }

    override fun getItemCount(): Int {
        return data.size
    }

}

class InnerViewpagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView = itemView as TextView
}