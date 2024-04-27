package com.wj.basecomponent.util.fw.basictype;

import com.wj.basecomponent.util.fw.annotations.Size;

import java.nio.ByteOrder;


/**
 * 为适配FW通讯所编写的无符号短整型。
 * 因为Java和kotlin没有UnsignedLong，且Long类型为8个字节，而FW的UnsignedLong为4个字节，且无符号，不能使用Java的integer来表达。
 * 注意：FWUnsignedLong不会检查你的value是否超出UnsignedLong的取值范围，如果你传入负数或者一个大于4字节的数来构造FWUnsignedLong，FWUnsignedLong不会抛出异常。
 * FWUnsignedLong只是确保能正确解析FW接口传过来的UnsignedLong数值，且保证将正确的UnsignedLong解析为正确的字节数组。
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size(4)
public class FWUnsignedLong implements FWBasicType<Long> {

    @Size(4)
    public Long value;

    public FWUnsignedLong() {
    }

    public FWUnsignedLong(Long value) {
        this.value = value;
    }

    @Override
    public Long getValue() {
        return value;
    }

    @Override
    public void setValue(Long aLong) {
        this.value = aLong;
    }


    public FWUnsignedLong(byte[] bytes, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            this.value = getValueBigOrder(bytes);
        } else {
            this.value = getValueLittleOrder(bytes);
        }
    }

    @Override
    public Long getValueBigOrder(@Size(4) byte[] bytes) {
        return (long) (0xFF00000000000000L & (0x00 << 56) | 0xFF000000000000L & (0x00 << 48) | 0xFF0000000000L & (0x00 << 40) | 0xFF00000000L & (0x00 << 32) | (0xFF000000L & (bytes[0]) << 24) | (0xFF0000L & (bytes[1]) << 16) | (0xFF00L & (bytes[2]) << 8) | (0xFF & bytes[3]) << 0);
    }

    @Override
    public Long getValueLittleOrder(@Size(4) byte[] bytes) {
        return (long) (0xFF00000000000000L & (0x00 << 56) | 0xFF000000000000L & (0x00 << 48) | 0xFF0000000000L & (0x00 << 40) | 0xFF00000000L & (0x00 << 32) | (0xFF000000L & (bytes[3]) << 24) | (0xFF0000L & (bytes[2]) << 16) | (0xFF00L & (bytes[1]) << 8) | (0xFF & bytes[0]) << 0);
    }

    @Override
    public byte[] getBytesBigOrder() {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) ((value) & 0xFF);
        return bytes;
    }

    @Override
    public byte[] getBytesLittleOrder() {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) ((value >> 24) & 0xFF);
        bytes[2] = (byte) ((value >> 16) & 0xFF);
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        bytes[0] = (byte) ((value) & 0xFF);
        return bytes;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
