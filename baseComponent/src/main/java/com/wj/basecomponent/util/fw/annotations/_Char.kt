package com.wj.basecomponent.util.fw.annotations

import androidx.annotation.IntRange

/**
 * 为了适配FW通讯所编写的类型，无符号char，1个字节
 * 取值范围~255
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD,AnnotationTarget.PROPERTY)
@IntRange(from = -128, to = 127)
annotation class _Char(val min:Int = -128, val max:Int = 127)