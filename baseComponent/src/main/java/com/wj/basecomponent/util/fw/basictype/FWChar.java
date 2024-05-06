package com.wj.basecomponent.util.fw.basictype;


import com.wj.basecomponent.util.fw.annotations.Size;

/**
 * 为适配FW通讯所编写的有符号char类型，直接用byte表示。
 * 注意：
 * 这里的char仅表示数值类型，不表示字符类型。
 * 如 FWChar(1) 表示为数值1，而不是字符'1'。（字符'1'的十进制数值为49）
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size(1)
public class FWChar implements FWBasicType<Byte> {

    @Size(1)
    public byte value;

    public FWChar() {
    }

    public FWChar(byte value) {
        this.value = value;
    }

    @Override
    public Byte getValue() {
        return value;
    }

    @Override
    public void setValue(Byte aByte) {
        this.value = aByte;
    }

    @Override
    public byte[] getBytesBigOrder() {
        return new byte[]{value};
    }

    @Override
    public byte[] getBytesLittleOrder() {
        return new byte[]{value};
    }

    @Override
    public Byte getValueBigOrder(byte[] bytes) {
        return bytes[0];
    }

    @Override
    public Byte getValueLittleOrder(byte[] bytes) {
        return bytes[0];
    }

    @Override
    public String toString() {
        return value + "";
    }
}
