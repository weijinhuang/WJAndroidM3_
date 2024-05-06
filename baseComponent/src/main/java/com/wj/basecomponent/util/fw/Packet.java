package com.wj.basecomponent.util.fw;

import java.nio.ByteOrder;

/**
 * 文 件 名: AVAPIs_Client
 * 版    权:  Bosma Technologies Co., Ltd. YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  likq
 * 修改时间:  2018/3/28 17:59
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */
public class Packet {
    public Packet() {
    }

    public static final short byteArrayToShort_Little(byte[] byt, int nBeginPos) {
        return (short) (255 & byt[nBeginPos] | (255 & byt[nBeginPos + 1]) << 8);
    }

    public static final int byteArrayToInt_Little(byte[] byt, int nBeginPos) {
        return 255 & byt[nBeginPos] |
                (255 & byt[nBeginPos + 1]) << 8 |
                (255 & byt[nBeginPos + 2]) << 16 |
                (255 & byt[nBeginPos + 3]) << 24;
    }


    public static final int byteArrayToInt_Little2(byte[] byt, int nBeginPos) {
        return byt[nBeginPos + 3] & 0xFF |
                (byt[nBeginPos + 2] & 0xFF) << 8 |
                (byt[nBeginPos + 1] & 0xFF) << 16 |
                (byt[nBeginPos] & 0xFF) << 24;
    }

    public static final long byteArrayToLong_Little2(byte[] byt, int nBeginPos) {
        long l = 0L;

        for (int i = 0; i < 4; ++i) {
            l |= (255L & (long) byt[nBeginPos + i]) << 8 * i;
        }

        return l;
    }

    public static final int byteArrayToInt_Little(byte[] byt) {
        return byt.length == 1 ? 255 & byt[0] : (byt.length == 2 ? 255 & byt[0] | (255 & byt[1]) << 8 : (byt.length == 4 ? 255 & byt[0] | (255 & byt[1]) << 8 | (255 & byt[2]) << 16 | (255 & byt[3]) << 24 : 0));
    }

    public static final long byteArrayToLong_Little(byte[] byt, int nBeginPos) {
        return (long) (255 & byt[nBeginPos] | (255 & byt[nBeginPos + 1]) << 8 | (255 & byt[nBeginPos + 2]) << 16 | (255 & byt[nBeginPos + 3]) << 24 | (255 & byt[nBeginPos + 4]) << 32 | (255 & byt[nBeginPos + 5]) << 40 | (255 & byt[nBeginPos + 6]) << 48 | (255 & byt[nBeginPos + 7]) << 56);
    }

    public static final int byteArrayToInt_Big(byte[] byt) {
        return byt.length == 1 ? 255 & byt[0] : (byt.length == 2 ? (255 & byt[0]) << 8 | 255 & byt[1] : (byt.length == 4 ? (255 & byt[0]) << 24 | (255 & byt[1]) << 16 | (255 & byt[2]) << 8 | 255 & byt[3] : 0));
    }

    public static final byte[] longToByteArray_Little(long value) {
        return new byte[]{(byte) ((int) value), (byte) ((int) (value >>> 8)), (byte) ((int) (value >>> 16)), (byte) ((int) (value >>> 24)), (byte) ((int) (value >>> 32)), (byte) ((int) (value >>> 40)), (byte) ((int) (value >>> 48)), (byte) ((int) (value >>> 56))};
    }

    public static final byte[] intToByteArray_Little(int value) {
        return new byte[]{(byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24)};
    }

    public static final byte[] intToByteArray_Big(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static final byte[] shortToByteArray_Little(short value) {
        return new byte[]{(byte) value, (byte) (value >>> 8)};
    }

    public static final byte[] shortToByteArray_Big(short value) {
        return new byte[]{(byte) (value >>> 8), (byte) value};
    }

    public static final short[] byteArray2shortArray_Little(byte[] b, int length) {
        short[] buf = new short[length / 2];

        for (int i = 0; i < length / 2; ++i) {
            buf[i] = byteArrayToShort_Little(b, i * 2);
        }

        return buf;
    }

    public static final byte[] shortArray2byteArray_Little(short[] s, int length) {
        byte[] buf = new byte[length * 2];

        for (int i = 0; i < length; ++i) {
            short s0 = s[i];
            byte[] b = shortToByteArray_Little(s0);
            buf[i * 2 + 0] = b[0];
            buf[i * 2 + 1] = b[1];
        }

        return buf;
    }

    public static final long bytes2Long(byte[] data, int length) {
        byte[] bData = reverse(data, length);
        int mask = 255;
        long n = 0L;

        for (int i = 0; i < length; ++i) {
            n <<= 8;
            int temp = bData[i] & mask;
            n |= (long) temp;
        }

        return n;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] reverse(byte[] data, int length) {
        int nSize = length;

        for (int i = 0; i < nSize / 2; ++i) {
            byte temp = data[i];
            data[i] = data[nSize - 1 - i];
            data[nSize - 1 - i] = temp;
        }

        return data;
    }


    /**
     * 从byte数组的index处的连续4个字节获得一个float
     * @param arr
     * @param index
     * @return
     */
    public static float byteToFloat(byte[] arr, int index) {
        return Float.intBitsToFloat(getInt(arr, index));
    }


    /**
     * 从byte数组的index处的连续4个字节获得一个int
     * @param arr
     * @param index
     * @return
     */
    public static int getInt(byte[] arr, int index) {
        return 	(0xff000000 	& (arr[index+0] << 24))  |
                (0x00ff0000 	& (arr[index+1] << 16))  |
                (0x0000ff00 	& (arr[index+2] << 8))   |
                (0x000000ff 	&  arr[index+3]);
    }

    public static boolean testCPU() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return true;
        }
        return false;
    }

    public static short getShort(byte[] buf) {
        return getShort(buf, testCPU());
    }

    public static short getShort(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }
        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }
        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r = (short) (r << 8);
                r = (short) (r | buf[i] & 0xFF);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r = (short) (r << 8);
                r = (short) (r | buf[i] & 0xFF);
            }
        }
        return r;
    }

    public static short[] Bytes2Shorts(byte[] buf) {
        byte bLength = 2;
        short[] s = new short[buf.length / bLength];
        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];
            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[(iLoop * bLength + jLoop)];
            }
            s[iLoop] = getShort(temp);
        }
        return s;
    }


}
