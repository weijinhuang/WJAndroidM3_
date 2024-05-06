package com.wj.basecomponent.util.fw.annotations

import androidx.annotation.IntRange

/**
 * 为了适配FW通讯所编写的类型
 * 整型，取值范围0-0xFFFFFFFF
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD,AnnotationTarget.PROPERTY)
@IntRange(from = -2147483648, to = 2147483647)
annotation class _Int(val min:Int = -2147483648, val max:Int= 2147483647)
