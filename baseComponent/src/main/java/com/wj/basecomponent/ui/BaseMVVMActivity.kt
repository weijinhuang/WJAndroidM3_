package com.wj.basecomponent.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wj.basecomponent.ui.constraint.BaseMVVM
import com.wj.basecomponent.vm.BaseViewModel
import java.lang.reflect.ParameterizedType

abstract class BaseMVVMActivity<VM : BaseViewModel, VDB : ViewBinding> : BaseActivity(), BaseMVVM<VM> {

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
        val binding = createViewBinding()
        mViewBinding = binding
        setContentView(binding.root)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createViewBinding(): VDB {
        val parameterizedType = javaClass.genericSuperclass as ParameterizedType
        val bindingClazz = parameterizedType.actualTypeArguments[1] as Class<VDB>
        val inflateMethod = bindingClazz.getMethod("inflate", LayoutInflater::class.java)
        return inflateMethod.invoke(null, layoutInflater) as VDB
    }
}
