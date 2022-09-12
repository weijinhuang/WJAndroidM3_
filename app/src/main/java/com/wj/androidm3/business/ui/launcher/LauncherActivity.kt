package com.wj.androidm3.business.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityLauncherBinding
import com.wj.androidm3.business.ui.main.MainActivity
import com.wj.basecomponent.ui.BaseMVVMActivity

class LauncherActivity : BaseMVVMActivity<LauncherViewModel, ActivityLauncherBinding>() {

    private lateinit var mImageViewAnimation: ScaleAnimation

    override fun getLayoutId(): Int {
        return R.layout.activity_launcher
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding?.viewModel = mViewModel
        startBackgroundAnimation()

    }

    private fun startBackgroundAnimation() {
        mImageViewAnimation = ScaleAnimation(
            1f,
            1.2f,
            1f,
            1.2f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        mImageViewAnimation.repeatCount = 0
        mImageViewAnimation.duration = 500
        mImageViewAnimation.repeatMode = Animation.REVERSE
        mImageViewAnimation.setAnimationListener(object :Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        mViewBinding?.backgroundImage?.animation = mImageViewAnimation
        mImageViewAnimation.start()
    }
}