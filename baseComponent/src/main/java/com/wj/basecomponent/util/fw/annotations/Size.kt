package com.wj.basecomponent.util.fw.annotations


/**
 * 为了适配FW通讯所编写的注解
 *
 * @Author    H.W.J
 * @Time      2021/3/5
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD,AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)

annotation class Size(val value:Int = 0)