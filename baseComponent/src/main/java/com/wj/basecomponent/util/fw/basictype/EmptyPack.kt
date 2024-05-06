package com.wj.basecomponent.util.fw.basictype

import android.os.Parcel
import android.os.Parcelable


/**
 * 空包
 */
public class EmptyPack : Parcelable , FWBasicType<ByteArray> {

    var length: Int = 0

    constructor(parcel: Parcel) : this() {
        length = parcel.readInt()
    }

    constructor()

    constructor(len: Int) {
        length = len
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(length)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EmptyPack> {
        override fun createFromParcel(parcel: Parcel): EmptyPack {
            return EmptyPack(parcel)
        }

        override fun newArray(size: Int): Array<EmptyPack?> {
            return arrayOfNulls(size)
        }
    }

    override fun getValue(): ByteArray {
        return ByteArray(length)
    }

    override fun setValue(t: ByteArray?) {

    }

    override fun getBytesBigOrder(): ByteArray {
        return ByteArray(length)
    }

    override fun getBytesLittleOrder(): ByteArray {
        return ByteArray(length)
    }

    override fun getValueBigOrder(bytes: ByteArray?): ByteArray {
        return ByteArray(length)
    }

    override fun getValueLittleOrder(bytes: ByteArray?): ByteArray {
        return ByteArray(length)
    }

    override fun toString(): String {
        return "EmptyPack(length=$length)"
    }


}