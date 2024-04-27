package com.wj.basecomponent.util.fw.annotations


/**
 * 为了适配FW通讯所编写的注解
 * 表示这里是一个对象
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class FWObject