package com.wj.androidm3.business.ui.anim

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wj.androidm3.R

class AnimationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, AnimationFragment.newInstance())
                .commitNow()
        }
    }
}