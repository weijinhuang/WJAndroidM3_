package com.wj.basecomponent.util.fw.annotations

/**
 * 表示这是一个FW字段类型
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY
)
annotation class FieldOrder(val order: Int = 0)