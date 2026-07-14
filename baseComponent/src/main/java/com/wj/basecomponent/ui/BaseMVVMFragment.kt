package com.wj.basecomponent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.wj.basecomponent.vm.BaseViewModel
import com.wj.basecomponent.ui.constraint.BaseMVVM
import java.lang.reflect.ParameterizedType

abstract class BaseMVVMFragment<VM : BaseViewModel, VDB : ViewBinding> : BaseFragment(), BaseMVVM<VM> {

    var mViewBinding: VDB? = null

    val mViewModel: VM by lazy { createViewModel(false) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null || !enableCacheView()) {
            mViewBinding = createViewBinding(inflater, container)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.mErrorMD.observe(this){errorMsg->
            errorMsg?.let {
                Toast.makeText(requireContext(),it,Toast.LENGTH_SHORT).show()
                mViewModel.mErrorMD.postValue(null)
            }
        }
    }

    fun bindView(inflater: LayoutInflater, container: ViewGroup?) {
        mViewBinding = createViewBinding(inflater, container)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VDB {
        val parameterizedType = javaClass.genericSuperclass as ParameterizedType
        val bindingClazz = parameterizedType.actualTypeArguments[1] as Class<VDB>
        val inflateMethod = bindingClazz.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.javaPrimitiveType
        )
        return inflateMethod.invoke(null, inflater, container, false) as VDB
    }


    override fun createViewModel(attachActivity: Boolean): VM {
        val parameterizedType = javaClass.genericSuperclass as ParameterizedType
        val vmClazz = parameterizedType.actualTypeArguments[0] as Class<VM>
        return ViewModelProvider(if (attachActivity) requireActivity() else this)[vmClazz]
    }

}
