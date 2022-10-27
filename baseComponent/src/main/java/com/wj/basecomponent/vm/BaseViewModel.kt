package com.wj.basecomponent.vm

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel(), Observable {
    private val mPropertyChangeRegistry: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    fun launch(codeBlock: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(block = codeBlock)

    fun launchBackground(codeBlock: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(context = Dispatchers.IO, block = codeBlock)

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        mPropertyChangeRegistry.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        mPropertyChangeRegistry.remove(callback)
    }

    fun notifyChange() {
        mPropertyChangeRegistry.notifyCallbacks(this, 0, null)
    }

    fun notifyPropertyChanged(fieldId: Int) {
        mPropertyChangeRegistry.notifyCallbacks(this, fieldId, null)
    }
}