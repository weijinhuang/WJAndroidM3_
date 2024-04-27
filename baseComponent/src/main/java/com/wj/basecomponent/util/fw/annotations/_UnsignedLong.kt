package com.wj.basecomponent.util.fw.annotations

import androidx.annotation.IntRange

/**
 * 为了适配FW通讯所编写的类型无符号长整型，取值范围0-65535
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@IntRange(from = 0, to = 0xFFFFFFFF)
annotation class _UnsignedLong(val min: Long = 0, val max: Long = 0xFFFFFFFF)