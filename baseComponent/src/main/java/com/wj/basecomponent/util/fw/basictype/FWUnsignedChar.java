package com.wj.basecomponent.util.fw.basictype;

import android.os.Parcel;
import android.os.Parcelable;

import com.wj.basecomponent.util.fw.Packet;
import com.wj.basecomponent.util.fw.annotations.Size;
import com.wj.basecomponent.util.fw.annotations._UnsignedChar;


/**
 * 为适配FW通讯所编写的无符号char类型，使用short表达。
 * 注意：
 * 这里的char仅表示数值类型，不表示字符类型。
 * 如 FWUnsignedChar(1) 表示为数值1，而不是字符'1'。（字符'1'的十进制数值为49）
 *
 * @author H.W.J
 * @date 2021/3/10
 */
@Size(1)
public class FWUnsignedChar implements FWBasicType<Short>, Parcelable {

    @Size(1)
    public Short value;

    public byte[] bytes;

    public FWUnsignedChar() {
        this((short)0);
    }

    public FWUnsignedChar(@_UnsignedChar Short value) {
        this.value = value;
        bytes = new byte[]{(byte) (value & 0x00ff)};
    }

    public FWUnsignedChar(byte[] bytes) {
        this.bytes = bytes;
        value = Packet.byteArrayToShort_Little(new byte[]{bytes[0], 0x00}, 0);
    }


    public Short getValue() {
        return value;
    }

    @Override
    public void setValue(Short aShort) {
        this.value = aShort;
        bytes = new byte[]{(byte) (value & 0x00ff)};
    }

    @Override
    public byte[] getBytesBigOrder() {
        return bytes;
    }

    @Override
    public byte[] getBytesLittleOrder() {
        return bytes;
    }

    @Override
    public Short getValueBigOrder(byte[] bytes) {
        return Packet.byteArrayToShort_Little(new byte[]{bytes[0], 0x00}, 0);
    }

    @Override
    public Short getValueLittleOrder(byte[] bytes) {
        return Packet.byteArrayToShort_Little(new byte[]{bytes[0], 0x00}, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.value);
        dest.writeByteArray(this.bytes);
    }

    public void readFromParcel(Parcel source) {
        this.value = (Short) source.readValue(Short.class.getClassLoader());
        this.bytes = source.createByteArray();
    }

    protected FWUnsignedChar(Parcel in) {
        this.value = (Short) in.readValue(Short.class.getClassLoader());
        this.bytes = in.createByteArray();
    }

    public static final Creator< FWUnsignedChar> CREATOR = new Creator< FWUnsignedChar>() {
        @Override
        public  FWUnsignedChar createFromParcel(Parcel source) {
            return new  FWUnsignedChar(source);
        }

        @Override
        public  FWUnsignedChar[] newArray(int size) {
            return new  FWUnsignedChar[size];
        }
    };

    @Override
    public String toString() {
        return value+"";
    }
}
