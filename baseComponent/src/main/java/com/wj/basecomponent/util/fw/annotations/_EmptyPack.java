package com.wj.basecomponent.util.fw.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author H.W.J
 * @date 2021/5/26
 *
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.TYPE_PARAMETER})
public @interface _EmptyPack {
    int value();
}
