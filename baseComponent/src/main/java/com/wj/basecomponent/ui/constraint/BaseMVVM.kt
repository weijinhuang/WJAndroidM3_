package com.wj.basecomponent.ui.constraint

import com.wj.basecomponent.vm.BaseViewModel

interface BaseMVVM<VM : BaseViewModel> {

    /**
     * @param attachActivity 生命周期是否跟着activity
     */
    fun createViewModel(attachActivity: Boolean): VM
}