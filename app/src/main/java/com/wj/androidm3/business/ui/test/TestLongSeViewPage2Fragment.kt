package com.wj.androidm3.business.ui.test

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentTestLongseViewpage2Binding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.getScreenHeight
import com.wj.basecomponent.util.getScreenWidth
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.view.GridItemDecoration2
import com.wj.basecomponent.view.LongSeViewPage2
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *@Create by H.W.J 2023/6/26/026
 */
class TestLongSeViewPage2Fragment : BaseMVVMFragment<BaseViewModel, FragmentTestLongseViewpage2Binding>() {


    private var gridItemDecoration2: GridItemDecoration2? = null

    override fun getLayoutId() = R.layout.fragment_test_longse_viewpage2

    override fun firstCreateView() {
        mViewBinding?.run {
            resizePlayerView(mViewBinding?.viewpager2)
//            resizePlayerView(mViewBinding?.viewPager)
            val adapter = TestLongSeViewPage2FragmentAdapter()
            viewpager2.adapter = adapter
            adapter.setOnItemDoubleClickListener { position ->
                WJLog.d("OnItemDoubleClick：$position")
                if (viewpager2.spanCount == 2) {
                    WJLog.d("单路模式")
                    adapter.setSpanCount(1)
                    viewpager2.spanCount = 1


                } else {
                    WJLog.d("多路模式")
                    adapter.setSpanCount(2)
                    viewpager2.spanCount = 2
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(150)
                    viewpager2.setCurrentItem(position, false)
                }
            }
            adapter.setOnItemClickListener {
                gridItemDecoration2?.setSelectedPos(it)
                viewpager2.updateSelected(it)
            }
            viewpager2.orientation = LongSeViewPage2.ORIENTATION_HORIZONTAL
            viewpager2.registerOnPageChangeCallback(object : LongSeViewPage2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                    WJLog.d("super.onPageScrolled($position, $positionOffset, $positionOffsetPixels)")
                }

                override fun onPageSelected(position: Int) {
                    WJLog.d("----onPageSelected($position: Int) ")
                }

                override fun onPageScrollStateChanged(state: Int) {
//                    WJLog.d("onPageScrollStateChanged($state: Int)")
                }
            })
            gridItemDecoration2 = GridItemDecoration2(1f).apply {
                viewpager2.addItemDecoration(this)
            }

            changeItem.setOnClickListener {

                val currentOrientation = requireActivity().requestedOrientation
                if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
//                viewpager2.currentItem = currentItem++

            }
            val dataList =
                listOf<List<String>>(
                    listOf("1", "2", "3", "4"),
                    listOf("5", "6", "7", "8"),
                    listOf("9", "10", "11", "12"),
                    listOf("13", "14", "15", "16")
                )




            viewPager.adapter = TestViewPagerAdapter(requireContext(), dataList)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resizePlayerView(mViewBinding?.viewpager2)
//        resizePlayerView(mViewBinding?.viewPager)
    }


    private fun resizePlayerView(view: View?) {
        view?.let {
            val originalLayoutParams = it.layoutParams
            var widthRatio = 16
            var heightRatio = 9
            val screenWidth = getScreenWidth()
            val screenHeight = getScreenHeight()
            if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                //横屏
                if (screenWidth.toFloat() / screenHeight.toFloat() > widthRatio.toFloat() / heightRatio.toFloat()) {
                    originalLayoutParams.height = screenHeight
                    originalLayoutParams.width = screenHeight * widthRatio / heightRatio
                } else {
                    originalLayoutParams.width = screenWidth
                    originalLayoutParams.height = screenWidth * heightRatio / widthRatio
                }
                it.layoutParams = originalLayoutParams
                if (originalLayoutParams is ConstraintLayout.LayoutParams) {
                    originalLayoutParams.bottomToBottom = 0
                }
//                mViewBinding?.playViewContainer?.enableFullScreenMode(widthRatio, heightRatio)
            } else {
                if (originalLayoutParams is ConstraintLayout.LayoutParams) {
                    originalLayoutParams.bottomToBottom = -1
                    originalLayoutParams.topToTop = 0
                }
                originalLayoutParams.width = screenWidth
                originalLayoutParams.height = screenWidth * heightRatio / widthRatio
//                mViewBinding?.playViewContainer?.disableFullScreenMode(widthRatio, heightRatio)
            }

            WJLog.d("重置播放器尺寸 -> ${originalLayoutParams.width}:${originalLayoutParams.height}")
        }
    }


    /**---------------------------------------------------------------------------------------------------------------*/
    /**---------------------------------------------------------------------------------------------------------------*/
    inner class TestLongSeViewPage2FragmentAdapter : RecyclerView.Adapter<TestLongSeViewPage2FragmentViewHolder>() {

        private var mLastClickTime = 0L
        private var mLastClickPosition = -1

        private var mSpanCount = 2

        private var mOnItemDoubleClick: ((position: Int) -> Unit)? = null

        private var mOnItemClick: ((position: Int) -> Unit)? = null


        fun setOnItemDoubleClickListener(onClick: (position: Int) -> Unit) {
            mOnItemDoubleClick = onClick
        }

        fun setOnItemClickListener(onClick: (position: Int) -> Unit) {
            mOnItemClick = onClick
        }

        fun setSpanCount(spanCount: Int) {
            mSpanCount = spanCount
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestLongSeViewPage2FragmentViewHolder {
            val view = TextView(parent.context)

//            val layoutParam = ViewGroup.LayoutParams(getScreenWidth() / 2, ViewGroup.LayoutParams.MATCH_PARENT)
            val width = if (mSpanCount == 1) ViewGroup.LayoutParams.MATCH_PARENT else getScreenWidth() / 2
            WJLog.d("width:$width")
            val layoutParam = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT)
            view.layoutParams = layoutParam
            view.setTextSize(COMPLEX_UNIT_SP, 18f)
            view.setTextColor(Color.WHITE)
            view.setBackgroundColor(Color.BLACK)
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_longse_viewpage2, null, false)
            view.layoutParams = layoutParam
            return TestLongSeViewPage2FragmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestLongSeViewPage2FragmentViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNullOrEmpty()) {
                onBindViewHolder(holder, position)
            }
        }

        override fun onBindViewHolder(holder: TestLongSeViewPage2FragmentViewHolder, position: Int) {
//            holder.playerView.setStatus(LongSePlayState.BUFFERING)
            WJLog.d("onBindViewHolder($holder: position, $position: Int)")
            val adapterPosition = holder.adapterPosition
            if (mSpanCount == 1) {
                val lp = holder.playerView.layoutParams
                if (lp.width != -1) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                holder.playerView.layoutParams = lp
            } else if (mSpanCount == 2) {
                val lp = holder.playerView.layoutParams
                if (lp.width == -1) {
                    lp.width = getScreenWidth() / 2
                }
                holder.playerView.layoutParams = lp
            }
            holder.playerView.text = "adapterPosition:$adapterPosition"
            holder.playerView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (mLastClickPosition == adapterPosition) {
                    if (currentTime - mLastClickTime < 250) {
                        mOnItemDoubleClick?.invoke(adapterPosition)
                    }
                }
                mOnItemClick?.invoke(adapterPosition)
                mLastClickPosition = adapterPosition
                mLastClickTime = currentTime

            }
        }

        override fun getItemCount(): Int {
            return 16
        }

    }


    //**********************************************************************************************************************************************/

    inner class TestLongSeViewPage2FragmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerView = itemView as TextView
    }

    private var mOnItemDoubleClick: ((position: Int) -> Unit)? = null

    private var mOnItemClick: ((position: Int) -> Unit)? = null


    fun setOnItemDoubleClickListener(onClick: (position: Int) -> Unit) {
        mOnItemDoubleClick = onClick
    }

    fun setOnItemClickListener(onClick: (position: Int) -> Unit) {
        mOnItemClick = onClick
    }

    inner class TestViewPagerAdapter(val context: Context, val data: List<List<String>>) : RecyclerView.Adapter<TestViewPagerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewPagerViewHolder {
            val view = LongSeViewPage2(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return TestViewPagerViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewPagerViewHolder, position: Int) {
            holder.longSeViewPage2.orientation = LongSeViewPage2.ORIENTATION_HORIZONTAL
            holder.longSeViewPage2.adapter = PlayerAdapter(data[position]).apply {

                setOnItemDoubleClickListener {
                    val newSpanCount = if (holder.longSeViewPage2.spanCount == 1) 2 else 1
                    setSingleMode(newSpanCount == 1)
                    holder.longSeViewPage2.spanCount = newSpanCount

                }
            }
//            holder.recyclerView.seton
//            holder.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                        val layoutManager = recyclerView.layoutManager as GridLayoutManager
//                        val findFirstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
//                        val view = layoutManager.findViewByPosition(findFirstVisibleItemPosition)
//                        WJLog.d("FirstVisibleItemPosition:$findFirstVisibleItemPosition Left:${view?.left}")
//                    }
//                }
//            })
//            holder.recyclerView.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false).apply {
//                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                    override fun getSpanSize(position: Int): Int {
//                        return 2
//                    }
//                }
//            }
        }

        override fun getItemCount() = 4
    }

    inner class TestViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val longSeViewPage2 = itemView as LongSeViewPage2
    }

    inner class PlayerAdapter(val data: List<String>) : RecyclerView.Adapter<PlayerHolder>() {
        private var mSingleMode = true

        private var mLastClickTime = 0L
        private var mLastClickPosition = -1
        private val mScreenWidth = getScreenWidth()

        private var mOnItemDoubleClick: ((position: Int) -> Unit)? = null

        private var mOnItemClick: ((position: Int) -> Unit)? = null

        fun setSingleMode(singleMode: Boolean) {
            mSingleMode = singleMode
        }

        fun setOnItemDoubleClickListener(onClick: (position: Int) -> Unit) {
            mOnItemDoubleClick = onClick
        }

        fun setOnItemClickListener(onClick: (position: Int) -> Unit) {
            mOnItemClick = onClick
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            view.setTextColor(Color.YELLOW)
            return PlayerHolder(view)
        }

        override fun onBindViewHolder(holder: PlayerHolder, position: Int) {
            val adapterPosition = holder.adapterPosition
            holder.textView.text = data[adapterPosition]
            if (mSingleMode) {
                val lp = holder.textView.layoutParams
                lp.width = mScreenWidth
                holder.textView.layoutParams = lp
            } else {
                val lp = holder.textView.layoutParams
                lp.width = getScreenWidth() / 2

                holder.textView.layoutParams = lp
            }
            holder.textView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (mLastClickPosition == adapterPosition) {
                    if (currentTime - mLastClickTime < 250) {
                        mOnItemDoubleClick?.invoke(adapterPosition)
                    }
                }
                mOnItemClick?.invoke(adapterPosition)
                mLastClickPosition = adapterPosition
                mLastClickTime = currentTime
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

    }

    inner class PlayerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView as TextView
    }

}