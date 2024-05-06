package com.wj.androidm3.business.ui.main.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.wj.androidm3.R
import com.wj.basecomponent.ui.BaseActivity
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.view.viewpager.LongSeViewPager

class ViewPager2NestedActivity3 : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_viewpager2_nested)
        val viewPager2 = findViewById<LongSeViewPager>(R.id.vp_outer)
        val dataArray = Array(2) { it1 -> Array(4) { it2 -> "$it1: $it2" } }
        val adapter = TestLongSeViewPage2FragmentAdapter()
        viewPager2?.let { viewpager2 ->
            viewpager2.adapter = adapter
            viewpager2.spanCount = 2
//            viewpager2.addItemDecoration(GridItemDecoration2(2f))
            viewpager2.orientation = LongSeViewPager.ORIENTATION_HORIZONTAL
            viewpager2.registerOnPageChangeCallback(object : LongSeViewPager.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                    LogUtils.d("super.onPageScrolled($position, $positionOffset, $positionOffsetPixels)")
                }

                override fun onPageSelected(position: Int) {
                    WJLog.d("onPageSelected($position: Int) ")
                }

                override fun onPageScrollStateChanged(state: Int) {
//                    LogUtils.d("onPageScrollStateChanged($state: Int)")
                }
            })
//            changeItem.setOnClickListener {
////                viewpager2.currentItem = currentItem++
////                if (currentItem == 8) {
////                    currentItem = 0
////                }
//                viewpager2.spanCount = 2
//            }
        }
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_viewpager2_nested
    }


    inner class TestLongSeViewPage2FragmentAdapter : RecyclerView.Adapter<TestLongSeViewPage2FragmentViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestLongSeViewPage2FragmentViewHolder {
            val view = TextView(parent.context)
            val layoutParam = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            view.layoutParams = layoutParam
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            view.setTextColor(Color.WHITE)
            view.setBackgroundColor(Color.BLACK)
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_longse_viewpage2, null, false)
            view.layoutParams = layoutParam
            return TestLongSeViewPage2FragmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestLongSeViewPage2FragmentViewHolder, position: Int) {
//            holder.playerView.setStatus(LongSePlayState.BUFFERING)
            holder.playerView.text = "position:$position"
            val lp = holder.itemView.layoutParams
            lp.width = 540
            holder.itemView.setOnClickListener { WJLog.d("选中:$position") }
        }

        override fun getItemCount(): Int {
            return 8
        }

    }

    inner class TestLongSeViewPage2FragmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //        val playerView: BaseLongSePlayerView = itemView.findViewById<BaseLongSePlayerView>(R.id.longse_player_view)
        val playerView = itemView as TextView
    }
}



class OuterViewpagerAdapter3(val context: Context, val dataList: Array<Array<String>>, val onPageChangeCallback: ViewPager2.OnPageChangeCallback) : RecyclerView.Adapter<OuterViewpagerViewHolder3>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewpagerViewHolder3 {
        val recyclerView = ViewPager2(parent.context)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recyclerView.layoutParams = layoutParams
        return OuterViewpagerViewHolder3(recyclerView)
    }

    override fun onBindViewHolder(holder: OuterViewpagerViewHolder3, position: Int) {
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

class OuterViewpagerViewHolder3(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val viewPager2 = itemView as ViewPager2
}
