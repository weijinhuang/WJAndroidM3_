package com.wj.androidm3.business.ui.recyclerview

import androidx.recyclerview.widget.LinearLayoutManager
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentRecyclerviewPractiseBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import java.text.SimpleDateFormat
import java.util.Locale

class RecyclerViewPractiseFragment : BaseMVVMFragment<RecyclerViewPractiseViewModel, FragmentRecyclerviewPractiseBinding>() {

    val mSimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val mAdapter: RecyclerViewPractiseAdapter2 = RecyclerViewPractiseAdapter2()


    override fun firstCreateView() {
        mViewBinding?.run {

            recyclerView.adapter = mAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            mAdapter.submitList(mViewModel.mOriginalList.toMutableList())

            addData.setOnClickListener {
                val newList = mViewModel.mOriginalList
                newList.add(RecyclerViewPractiseItem(mViewModel.mCurrentIndex, "name:${mViewModel.mCurrentIndex}"))
                mViewModel.mCurrentIndex++
                mAdapter.submitList(newList.toMutableList())
            }

            removeData.setOnClickListener {
                val newList = mViewModel.mOriginalList
                newList.removeFirst()
                newList.removeLast()
                mAdapter.submitList(newList.toMutableList())
            }

            modifyData.setOnClickListener {
                val newList = mViewModel.mOriginalList
                newList.first().name = mSimpleDateFormat.format(System.currentTimeMillis())
                newList.first().areContentsTheSame = false
                newList.last().name = mSimpleDateFormat.format(System.currentTimeMillis() + 10000)
                newList.last().areContentsTheSame = false
                mAdapter.submitList(newList.toMutableList())
            }
            removeAndModify.setOnClickListener {
                val newList = mViewModel.mOriginalList
//                val first = newList.first()
//                first.name = mSimpleDateFormat.format(System.currentTimeMillis())
//                first.areContentsTheSame = false
                val last = newList.last()
                last.name = mSimpleDateFormat.format(System.currentTimeMillis() + 10000)
                last.areContentsTheSame = false
                newList.removeFirst()
                mAdapter.submitList(newList.toMutableList())
            }
            swap.setOnClickListener {
                val newList = mViewModel.mOriginalList
                val first = newList.first()
                val last = newList.last()
                newList[newList.size - 1] = first
                newList[0] = last
                mAdapter.submitList(newList.toMutableList())
            }
        }
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_recyclerview_practise
    }
}