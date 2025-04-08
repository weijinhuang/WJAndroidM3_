package com.wj.androidm3.business.ui.playerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.R
import com.wj.basecomponent.util.getScreenWidth
import com.wj.basecomponent.util.log.WJLog

/**
 *@Create by H.W.J 2025/3/31/031
 */
class TestMultiplePlayerViewAdapter(private val dataList: MutableList<String>) : RecyclerView.Adapter<TestMultiplePlayerViewAdapter.MyViewHolder>() {

    private var currentLayoutMode = 1

    fun setCurrentLayoutMode(layoutMode: Int) {
        currentLayoutMode = layoutMode
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.test_multiple_item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = dataList[position]
        val bindingPos = holder.adapterPosition
        val screenSize = getScreenWidth()
        val lp = holder.textView.layoutParams
        val itemViewType : Int = getItemViewType(bindingPos)
//        lp.width = screenSize / 12 * itemViewType
//        lp.height = lp.width * 9 / 16
//        holder.textView.layoutParams = lp
        WJLog.d("onBindViewHolder :$position itemViewType：$itemViewType   width:${lp.width} height:${lp.height}")

    }

    override fun getItemCount(): Int = dataList.size

    override fun getItemViewType(position: Int): Int {
        var spanSize: Int
        when (currentLayoutMode) {
            4 -> spanSize = 6
            8 -> {
                if (position % 8 == 0) {
                    spanSize = 9
                } else {
                    spanSize = 3
                }
            }

            9 -> spanSize = 4
            16 -> spanSize = 1
            else -> spanSize = 12
        }
        return spanSize
    }
}