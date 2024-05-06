package com.wj.basecomponent.util.fw.basictype;

import java.io.Serializable;

/**
 * @author W.Jin H
 * @Description: 为适配FW通讯所编写的基本数据类型
 * @date 2021/3/10
 */
public interface FWBasicType<T> extends Serializable {
        
    /**
     * 获取value
     */
    T getValue();

    void setValue(T t);

    byte[] getBytesBigOrder();

    byte[] getBytesLittleOrder();

    T getValueBigOrder(byte[] bytes);

    T getValueLittleOrder(byte[] bytes);

}
