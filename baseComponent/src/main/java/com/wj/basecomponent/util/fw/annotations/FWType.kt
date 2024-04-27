package com.wj.basecomponent.util.fw.annotations

import com.wj.basecomponent.util.fw.annotations.FWByteOrder


/**
 * 表示这是一个FW字段类型
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY
)
annotation class FWType(
    val fieldOrder: Int,//这个字段在文档结构体中的顺序，必填
    val size: Int = Int.MIN_VALUE,//这个字段的大小（字节,String（即char[])必填）
    val isObject: Boolean = false,//这个字段是否是对象（结构体）
    val byteOrder: String = FWByteOrder.LITTLE,//数值类型转换的大小端序，默认小端
    val isArray: Boolean = false
)
