package com.wj.androidm3.business.ui.main.viewpage2

import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.anim.AnimationViewModel
import com.wj.androidm3.databinding.FragmentViewpage2TestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog

/**
 *@Create by H.W.J 2022/11/1/001
 */
class ViewPager2TestFragment : BaseMVVMFragment<AnimationViewModel, FragmentViewpage2TestBinding>() {
    private var mTextView: TextView? = null


    override fun firstCreateView() {
        mViewBinding?.run {
            timePicker.setIs24HourView(true)
            numberPicker.maxValue = 24
            numberPicker.minValue = 0
            stringPicker.postDelayed({
                val dataSet = ArrayList<String>()
                dataSet.add("")
                for (i in 0..24) {
                    dataSet.add("$i:00")
                }
                dataSet.add("")
                stringPicker.setDataSet(dataSet)
            }, 500)

            val adapter = ViewPager2TestFragmentAdapter(listOf("a", "b", "c"), this@ViewPager2TestFragment)
            viewPager.adapter = adapter
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    WJLog.i("onPageSelected:$position")
                    mTextView = adapter.mFragments.get(position).getTextView()

                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    WJLog.i("onPageScrolled ->position:$position positionOffset:$positionOffset positionOffsetPixels:$positionOffsetPixels")
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    when (state) {
                        ViewPager2.SCROLL_STATE_IDLE -> WJLog.i("onPageScrollStateChanged -> SCROLL_STATE_IDLE")
                        ViewPager2.SCROLL_STATE_DRAGGING -> WJLog.i("onPageScrollStateChanged -> SCROLL_STATE_DRAGGING")
                        ViewPager2.SCROLL_STATE_SETTLING -> WJLog.i("onPageScrollStateChanged -> SCROLL_STATE_SETTLING")
                    }
                }
            })
            button.setOnClickListener {
                mTextView?.text = "${System.currentTimeMillis()}"
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_viewpage2_test
    }
}