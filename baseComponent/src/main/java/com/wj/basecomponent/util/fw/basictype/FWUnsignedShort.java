package com.wj.basecomponent.util.fw.basictype;


import com.wj.basecomponent.util.fw.Packet;
import com.wj.basecomponent.util.fw.annotations.Size;
import com.wj.basecomponent.util.fw.annotations._UnsignedShort;

/**
 * 为适配FW通讯所编写的无符号短整型。
 * 因为Java和kotlin没有UnsignedShort，且Java和Kotlin的short取值范围比C/C++UnsignedShort小一半，所以内部使用Integer来实现无符号短整型
 * 注意：FWUnsignedShort不会检查你的value是否超出无符号短整型的取值范围，如果你传入负数或者一个大于2字节的数来构造FWUnsignedShort，FWUnsignedShort不会抛出异常。
 * FWUnsignedShort只是确保能正确解析FW接口传过来的UnsignedShort数值，且保证将正确的UnsignedShort解析为正确的字节数组。
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size(2)
public class FWUnsignedShort implements FWBasicType<Integer> {

    @Size(1)
    public int value;

    public FWUnsignedShort() {
    }

    public FWUnsignedShort(@_UnsignedShort int value) {
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
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value >> 8) & 0xff);
        bytes[1] = (byte) ((value) & 0xff);
        return bytes;
    }

    @Override
    public byte[] getBytesLittleOrder() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value) & 0xff);
        bytes[1] = (byte) ((value >> 8) & 0xff);
        return bytes;
    }

    @Override
    public Integer getValueBigOrder(byte[] bytes) {
        byte[] intBytes = new byte[4];
        intBytes[0] = 0;
        intBytes[1] = 0;
        intBytes[2] = bytes[0];
        intBytes[3] = bytes[1];
        return Packet.byteArrayToInt_Big(intBytes);
    }

    @Override
    public Integer getValueLittleOrder(byte[] bytes) {
        byte[] intBytes = new byte[4];
        intBytes[0] = bytes[0];
        intBytes[1] = bytes[1];
        intBytes[2] = 0;
        intBytes[3] = 0;
        return Packet.byteArrayToInt_Little(intBytes);
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
