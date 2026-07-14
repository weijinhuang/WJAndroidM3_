package com.wj.androidm3.business.ui.tabact.ui.main

import android.os.Bundle
import android.view.View
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentTabBinding
import com.wj.basecomponent.ui.BaseMVVMFragment

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : BaseMVVMFragment<PageViewModel, FragmentTabBinding>() {

    override fun enableCacheView() = false

    override fun getLayoutId(): Int {
        return R.layout.fragment_tab
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
    }

    override fun firstCreateView() = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textView = mViewBinding?.sectionLabel ?: return
        mViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}
