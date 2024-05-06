package com.wj.basecomponent.util.fw.annotations

import androidx.annotation.IntRange

/**
 * 为了适配FW通讯所编写的类型无符号整型，取值范围0-0xFFFFFFFF
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD,AnnotationTarget.PROPERTY)
@IntRange(from = 0, to = 0xFFFFFFFF)
annotation class _UnsignedInt(val min:Int = 0, val max:Long = 0xFFFFFFFF)