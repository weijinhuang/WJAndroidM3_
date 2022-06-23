package com.wj.basecomponent.ui

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.wj.basecomponent.ui.constraint.BaseMVVM
import com.wj.basecomponent.vm.BaseViewModel
import java.lang.reflect.ParameterizedType

abstract class BaseMVVMActivity<VM : BaseViewModel, VDB : ViewDataBinding> : BaseActivity(), BaseMVVM<VM> {

     var mViewBinding: VDB? = null

     val mViewModel: VM by lazy { createViewModel(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView()
    }


    override fun createViewModel(attachActivity: Boolean): VM {
        val parameterizedType = javaClass.genericSuperclass as ParameterizedType
        val vmClazz = parameterizedType.actualTypeArguments[0] as Class<VM>
        return ViewModelProvider(this)[vmClazz]
    }

    private fun bindView() {
        mViewBinding = DataBindingUtil.setContentView(this, getLayoutId())
    }
}