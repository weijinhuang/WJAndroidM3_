package com.wj.androidm3.business.ui.main.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.wj.androidm3.R
import com.wj.basecomponent.ui.BaseActivity

class ViewPager2NestedActivity2 : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_viewpager2_nested)
        val viewPager2 = findViewById<ViewPager2>(R.id.vp_outer)
        val dataArray = Array(2) { it1 -> Array(4) { it2 -> "$it1: $it2" } }
        val adapter = OuterViewpagerAdapter2(this, dataArray, object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPager2.isUserInputEnabled = position == 3
            }
        })
        viewPager2?.let { vp ->
            vp.adapter = adapter
        }
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_viewpager2_nested
    }


}

class OuterViewpagerAdapter2(val context: Context, val dataList: Array<Array<String>>, val onPageChangeCallback: ViewPager2.OnPageChangeCallback) : RecyclerView.Adapter<OuterViewpagerViewHolder2>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewpagerViewHolder2 {
        val recyclerView = ViewPager2(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recyclerView.layoutParams = layoutParams
        return OuterViewpagerViewHolder2(recyclerView)
    }

    override fun onBindViewHolder(holder: OuterViewpagerViewHolder2, position: Int) {
        val data = dataList[position]
        val adapter = InnerViewpagerAdapter2(data)
        holder.viewPager2.adapter = adapter
        holder.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
//        holder.viewPager2.registerOnPageChangeCallback( object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                if (position == 0 || position == 3) {
//                    holder.viewPager2.isUserInputEnabled = false
//                }
//            }
//        })
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}

class OuterViewpagerViewHolder2(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val viewPager2 = itemView as ViewPager2
}

class InnerViewpagerAdapter2(val data: Array<String>) : RecyclerView.Adapter<InnerViewpagerViewHolder2>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewpagerViewHolder2 {
        val textView = TextView(parent.context)
        textView.gravity = Gravity.CENTER
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        textView.layoutParams = layoutParams
        return InnerViewpagerViewHolder2(textView)
    }

    override fun onBindViewHolder(holder: InnerViewpagerViewHolder2, position: Int) {
        holder.textView.text = data[position]
    }

    override fun getItemCount(): Int {
        return data.size
    }

}

class InnerViewpagerViewHolder2(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView = itemView as TextView
}