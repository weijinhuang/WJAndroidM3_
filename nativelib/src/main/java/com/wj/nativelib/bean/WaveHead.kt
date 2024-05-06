package com.wj.nativelib.bean

import com.wj.basecomponent.util.fw.annotations.FWType
import com.wj.basecomponent.util.fw.basictype.FWString
import com.wj.basecomponent.util.fw.basictype.FWUnsignedInt
import com.wj.basecomponent.util.fw.basictype.FWUnsignedShort

class WaveHead(

    @FWType(fieldOrder = 1, size = 4)
    val riffChunkId: FWString = FWString("RIFF"),


    @FWType(fieldOrder = 2)
    val riffChunkDataSize: FWUnsignedInt,

    @FWType(fieldOrder = 3, size = 4)
    val format: FWString = FWString("WAVE"),

    @FWType(fieldOrder = 4, size = 4)
    val fmtChunkId: FWString = FWString("fmt", 4),

// fmt chunk的data大小：存储PCM数据时，是16

    @FWType(fieldOrder = 5)
    val fmtChunkDataSize: FWUnsignedInt = FWUnsignedInt(16),
    // 音频编码，1表示PCM，3表示Floating Point

    @FWType(fieldOrder = 6)
    val audioFormat: FWUnsignedShort = FWUnsignedShort(1),

    @FWType(fieldOrder = 7)
    val numChannels: FWUnsignedShort,

    @FWType(fieldOrder = 8)
    val simpleRate: FWUnsignedInt,

    @FWType(fieldOrder = 9)
    val byteRate: FWUnsignedInt,

    @FWType(fieldOrder = 10)
    val bitsPerSample: FWUnsignedShort,

    @FWType(fieldOrder = 11, size = 4)
    val dateChunkId: FWString = FWString("data"),

    @FWType(fieldOrder = 12)
    val dataChunkDataSize: FWUnsignedInt

) {

}