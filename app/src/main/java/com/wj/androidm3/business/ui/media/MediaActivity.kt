package com.wj.androidm3.business.ui.media

import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityMediaBinding
import com.wj.basecomponent.ui.BaseMVVMActivity

class MediaActivity : BaseMVVMActivity<MediaViewModel, ActivityMediaBinding>() {


    override fun getLayoutId(): Int {
        return R.layout.activity_media
    }
}