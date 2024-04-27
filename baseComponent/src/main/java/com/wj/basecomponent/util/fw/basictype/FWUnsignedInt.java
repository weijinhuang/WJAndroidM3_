package com.wj.basecomponent.util.fw.basictype;

import android.os.Parcel;
import android.os.Parcelable;


import com.wj.basecomponent.util.fw.annotations.Size;

import java.nio.ByteOrder;

/**
 * 为适配FW通讯所编写的无符号整型。
 * 因为Java和kotlin没有UnsignedInt，且Int的取值范围比UnsignedInt少一半，故内部使用Long类型来实现UnsignedInt。
 * 注意：FWUnsignedInt不会检查你的value是否超出UnsignedInt的取值范围，如果你传入负数或者一个大于4字节的数来构造FWUnsignedInt，FWUnsignedInt不会抛出异常。
 * FWUnsignedInt只是确保能正确解析FW接口传过来的UnsignedInt数值，且保证将正确的UnsignedInt解析为正确的字节数组。
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size(4)
public class FWUnsignedInt implements FWBasicType<Long>, Parcelable {

    @Size(4)
    public Long value;

    public FWUnsignedInt() {
    }

    public FWUnsignedInt(Long value) {
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

    public FWUnsignedInt(byte[] bytes, ByteOrder byteOrder) {
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
        bytes[0] = (byte) ((value >> 24) & 0xff);
        bytes[1] = (byte) ((value >> 16) & 0xff);
        bytes[2] = (byte) ((value >> 8) & 0xff);
        bytes[3] = (byte) ((value) & 0xff);
        return bytes;
    }

    @Override
    public byte[] getBytesLittleOrder() {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) ((value >> 24) & 0xff);
        bytes[2] = (byte) ((value >> 16) & 0xff);
        bytes[1] = (byte) ((value >> 8) & 0xff);
        bytes[0] = (byte) ((value) & 0xff);
        return bytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.value);
    }

    public void readFromParcel(Parcel source) {
        this.value = (Long) source.readValue(Long.class.getClassLoader());
    }

    protected FWUnsignedInt(Parcel in) {
        this.value = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Creator<FWUnsignedInt> CREATOR = new Creator<FWUnsignedInt>() {
        @Override
        public FWUnsignedInt createFromParcel(Parcel source) {
            return new FWUnsignedInt(source);
        }

        @Override
        public FWUnsignedInt[] newArray(int size) {
            return new FWUnsignedInt[size];
        }
    };

    @Override
    public String toString() {
        return  "" + value ;
    }
}
