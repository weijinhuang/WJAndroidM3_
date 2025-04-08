package com.wj.androidm3.business.ui.playerview

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentNewHsMultipleplayerviewTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import java.util.Collections

/**
 *@Create by H.W.J 2025/3/31/031
 */
class NewHsMultiplePlayerViewTestFragment : BaseMVVMFragment<BaseViewModel, FragmentNewHsMultipleplayerviewTestBinding>() {

    private lateinit var adapter: TestMultiplePlayerViewAdapter
    private val dataList = mutableListOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
    )
    private var currentLayoutMode = 8 // 0:A, 1:B, 2:C, 3:D
    override fun getLayoutId(): Int {
        return R.layout.fragment_new_hs_multipleplayerview_test
    }

    private var mCurrentFirstPosition = 0
    override fun firstCreateView() {
        mViewBinding?.run {
            adapter = TestMultiplePlayerViewAdapter(dataList)
            recyclerView.adapter = adapter

            setupPagerSnapHelper()

            button1.setOnClickListener { setLayoutMode(1) }
            button4.setOnClickListener { setLayoutMode(4) }
            button8.setOnClickListener { setLayoutMode(8) }
            button9.setOnClickListener { setLayoutMode(9) }
            button16.setOnClickListener { setLayoutMode(16) }
            setLayoutMode(currentLayoutMode)
            setupItemTouchHelper()
            nextPage.setOnClickListener {
                when(currentLayoutMode){
                    4->{
                        mCurrentFirstPosition+=4
                        if(mCurrentFirstPosition > 15){
                            mCurrentFirstPosition = 0
                        }
                        recyclerView.scrollToPosition(mCurrentFirstPosition)
                    }
                    8->{
                        mCurrentFirstPosition+=8
                        if(mCurrentFirstPosition > 15){
                            mCurrentFirstPosition = 0
                        }
                        recyclerView.scrollToPosition(mCurrentFirstPosition)
                    }
                    9->{
                        mCurrentFirstPosition+=9
                        if(mCurrentFirstPosition > 15){
                            mCurrentFirstPosition = 0
                        }
                        recyclerView.scrollToPosition(mCurrentFirstPosition)
                    }
                    16->{

                    }
                    else ->{
                        mCurrentFirstPosition+=1
                        if(mCurrentFirstPosition > 15){
                            mCurrentFirstPosition = 0
                        }
                        recyclerView.scrollToPosition(mCurrentFirstPosition)
                    }
                }
                WJLog.d("scroll to $mCurrentFirstPosition")
            }
        }

    }

    var snapHelper: PagerSnapHelper? = null
    private fun setupPagerSnapHelper() {
//        snapHelper = PagerSnapHelperImpl2(currentLayoutMode)
        snapHelper = PagerSnapHelper()
        snapHelper?.attachToRecyclerView(mViewBinding?.recyclerView)
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                Collections.swap(dataList, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }
        })
        itemTouchHelper.attachToRecyclerView(mViewBinding?.recyclerView)
    }

    private fun setLayoutMode(mode: Int) {
        currentLayoutMode = mode
//        snapHelper?.spanCount = mode
        adapter.setCurrentLayoutMode(mode)
        mViewBinding?.run {
            val gridLayoutManager = GridLayoutManager(requireContext(), 144, GridLayoutManager.HORIZONTAL, false)
//            val spannedGridLayoutManager =  SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.HORIZONTAL, 144)
//            spannedGridLayoutManager.spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup{position->
//                 when (currentLayoutMode) {
//
//                    4 -> SpanSize(72,72)
//                    8 -> {
//                        if (position % 8 == 0) {
//                            SpanSize(108,108)
//                        } else {
//                            SpanSize(36,36)
//                        }
//                    }
//
//                    9 -> SpanSize(48,48)
//                    16 -> SpanSize(36,36)
//                    else -> SpanSize(144,144)
//                }
//            }
            val spannedGridLayoutManager = SpannedGridLayoutManager({ position ->
                when (currentLayoutMode) {

                    4 -> SpannedGridLayoutManager.SpanInfo(6, 6)
                    8 -> {
                        if (position % 8 == 0) {
                            SpannedGridLayoutManager.SpanInfo(9, 9)
                        } else {
                            SpannedGridLayoutManager.SpanInfo(3, 3)
                        }
                    }

                    9 -> SpannedGridLayoutManager.SpanInfo(4, 4)
                    16 -> SpannedGridLayoutManager.SpanInfo(1, 1)
                    else -> SpannedGridLayoutManager.SpanInfo(12, 12)
                }
            }, 12, 16f / 9f)

            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    var spanSize = 12
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
                        16 -> spanSize = 3
                        else -> spanSize = 12
                    }
//                    LogUtils.d("SpanSize:$spanSize")
                    return spanSize
                }
            }
            recyclerView.layoutManager = spannedGridLayoutManager
//            adapter.notifyDataSetChanged()
        }

    }
}