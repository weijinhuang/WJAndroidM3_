package com.wj.androidm3.business.ui.main.viewpage2

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 *@Create by H.W.J 2022/11/1/001
 */
class ViewPager2TestFragmentAdapter(val channels: List<String>, fragment: Fragment) : FragmentStateAdapter(fragment) {
    val mFragments: List<ViewPage2TestChildFragment>

    init {
        mFragments = ArrayList(channels.size)
        for (i in channels.indices) {
            mFragments.add(ViewPage2TestChildFragment(channels.get(i)))
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }

}