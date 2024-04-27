package com.wj.basecomponent.util.fw.basictype;



import com.wj.basecomponent.util.log.WJLog;

import java.io.UnsupportedEncodingException;


/**
 * 为适配FW通讯所编写的String类型，用来解析文档里面的char[]类型
 * 注意，如果用来反序列化char[]为字符串，必须使用FWType声明size大小
 *
 * @author H.W.J
 * @date 2021/3/10
 */
public class FWString implements FWBasicType<String> {

    public byte[] bytes;

    public String value;


    public FWString() {
    }

    public FWString(String value) {
        this.value = value;
        bytes = value.getBytes();
    }

    /**
     * 构造函数，拼接不方便直接和String写在一起的字符，比如'\u0000'
     *
     * @param value
     * @param c
     */
    public FWString(String value, char c) {
        this.value = value;
        byte[] tempBytes = value.getBytes();
        bytes = new byte[tempBytes.length + 1];
        System.arraycopy(tempBytes, 0, bytes, 0, tempBytes.length);
        bytes[bytes.length - 1] = (byte) c;
    }

    /**
     * 构造函数，指定这个String最小长度，有些接口要求char[]必须为X长度，但我们的值不够这个长度，可以使用这个构造函数，会自动在字节数组后面填充空字符
     *
     * @param value
     * @param minLen
     */
    public FWString(String value, int minLen) {
        this.value = value;
        try{
            byte[] tempBytes = value.getBytes("utf-8");
            if (minLen <= 0) {
                bytes = tempBytes;
            } else {
                bytes = new byte[minLen];
                System.arraycopy(tempBytes, 0, bytes, 0, tempBytes.length);
            }
        }catch(Exception e){
            WJLog.Companion.e(e.getMessage());
        }
    }

    public FWString(byte[] bytes) throws UnsupportedEncodingException {
        this.bytes = bytes;
        value = getStringValue(bytes);
    }

    private static int searchByte(byte[] data, byte value) {
        int size = data.length;
        for (int i = 0; i < size; ++i) {
            if (data[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static String getStringValue(byte[] data) throws UnsupportedEncodingException {
        int offset = searchByte(data, (byte) 0);

        String str = null;
        if (offset < 0) {
            return new String(data, 0, data.length, "utf-8");
        }
        return new String(data, 0, offset, "utf-8");
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String s) {
        this.value = s;
        bytes = value.getBytes();
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
    public String getValueBigOrder(byte[] bytes) {
        try {
            return getStringValue(bytes);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public String getValueLittleOrder(byte[] bytes) {
        try {
            return getStringValue(bytes);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
