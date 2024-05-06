package com.wj.basecomponent.util.fw.annotations;

import androidx.annotation.StringDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author H.W.J
 * @Description: 字节顺序, 默认小端序
 * @date 2021/3/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.PARAMETER})
@StringDef({FWByteOrder.BIG, FWByteOrder.LITTLE})
public @interface FWByteOrder {

    String order() default LITTLE;

    String BIG = "BIG_ENDIAN";

    String LITTLE = "LITTLE_ENDIAN";
}
