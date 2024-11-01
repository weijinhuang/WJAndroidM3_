package com.wj.androidm3.business.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.anim.AnimationViewModel
import com.wj.androidm3.databinding.ActivityTestViewBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.util.log.WJLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class TestViewActivity : BaseMVVMActivity<AnimationViewModel, ActivityTestViewBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.activity_test_view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding?.addProgress?.setOnClickListener {
            val oldProgress = mViewBinding?.progressBar?.progress ?: 0
            mViewBinding?.progressBar?.setProgress(oldProgress + 10)


        }
        mViewBinding?.startAnim?.setOnClickListener {
            startAnim()
        }
    }

    var animJob: Job? = null
    var mStartAnim = false
    private fun startAnim() {
        mStartAnim = true
        animJob = lifecycleScope.launch {

            WJLog.d("开始动画")
            mViewBinding?.run {
                val views = listOf<View>(line1, line2, line3, line4, line5, line6, line7)
                for (i in 0..6) {
                    val line = views[i]
                    line.visibility = View.VISIBLE
                    val animator1: ObjectAnimator = ObjectAnimator.ofFloat<View>(line, View.SCALE_Y, 1f, 5f, 1f)
                    animator1.duration = 700
                    animator1.addListener(onEnd = {
                        if (i == 6) {
                            views.forEach { it.visibility = View.GONE }
                            WJLog.d("所有动画结束")
                            if (mStartAnim) {
                                startAnim()
                            }
                        }
                    })
                    animator1.start()
                    delay(200)
                    WJLog.d("开始Line ${i + 1}")
                }

            }


        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mStartAnim = false
        animJob?.cancel()
    }
}