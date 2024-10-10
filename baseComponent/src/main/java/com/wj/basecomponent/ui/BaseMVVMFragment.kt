package com.wj.basecomponent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.wj.basecomponent.vm.BaseViewModel
import com.wj.basecomponent.ui.constraint.BaseMVVM
import java.lang.reflect.ParameterizedType

abstract class BaseMVVMFragment<VM : BaseViewModel, VDB : ViewDataBinding> : BaseFragment(), BaseMVVM<VM> {

    var mViewBinding: VDB? = null

    val mViewModel: VM by lazy { createViewModel(false) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null || !enableCacheView()) {
            mViewBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
            mRootView = mViewBinding?.root
            firstCreateView()
        }
        mRootView?.let { rootView ->
            val p = rootView.parent
            if (p is ViewGroup) {
                p.removeAllViews()
            }
        }
        return mRootView
    }



    fun bindView(inflater: LayoutInflater, container: ViewGroup?) {
        mViewBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
    }


    override fun createViewModel(attachActivity: Boolean): VM {
        val parameterizedType = javaClass.genericSuperclass as ParameterizedType
        val vmClazz = parameterizedType.actualTypeArguments[0] as Class<VM>
        return ViewModelProvider(if (attachActivity) requireActivity() else this)[vmClazz]
    }

}