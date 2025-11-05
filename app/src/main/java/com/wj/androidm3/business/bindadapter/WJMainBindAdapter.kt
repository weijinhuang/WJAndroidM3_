package com.wj.androidm3.business.bindadapter

import android.widget.TextView
import androidx.databinding.BindingAdapter

/**
 *@Create by H.W.J 2024/12/20/020
 */
class WJMainBindAdapter {
}

@BindingAdapter("bindAge","bindName")
fun person(textView: TextView, age: String, name: String) {
    textView.text = "name:$name age:$age"
}