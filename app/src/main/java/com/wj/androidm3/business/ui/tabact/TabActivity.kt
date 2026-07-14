package com.wj.androidm3.business.ui.tabact

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.tabact.ui.main.SectionsPagerAdapter
import com.wj.androidm3.databinding.ActivityTabBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel

class TabActivity : BaseMVVMActivity<BaseViewModel, ActivityTabBinding>() {

    private val binding: ActivityTabBinding
        get() = requireNotNull(mViewBinding)

    override fun getLayoutId(): Int {
        return R.layout.activity_tab
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = binding.fab

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}
