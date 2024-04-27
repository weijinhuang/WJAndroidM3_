package com.wj.basecomponent.util.fw.basictype;

import com.wj.basecomponent.util.fw.Packet;
import com.wj.basecomponent.util.fw.annotations.Size;

/**
 * 为适配FW通讯所编写的类型
 * 等于java的int类型
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size( 4)
public class FWInt implements FWBasicType<Integer> {

    @Size(4)
    public int value;

    public FWInt() {
    }

    public FWInt(int value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Integer integer) {
        this.value = integer;
    }

    @Override
    public byte[] getBytesBigOrder() {
        return Packet.intToByteArray_Big(value);
    }

    @Override
    public byte[] getBytesLittleOrder() {
        return Packet.intToByteArray_Little(value);
    }

    @Override
    public Integer getValueBigOrder(byte[] bytes) {
        return Packet.byteArrayToInt_Big(bytes);
    }

    @Override
    public Integer getValueLittleOrder(byte[] bytes) {

        return Packet.byteArrayToInt_Little(bytes);
    }

    @Override
    public String toString() {
        return  "" + value  ;
    }
}
