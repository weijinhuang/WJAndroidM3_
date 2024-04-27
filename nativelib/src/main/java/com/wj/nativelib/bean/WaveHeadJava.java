package com.wj.nativelib.bean;

import com.wj.basecomponent.util.fw.annotations.FWType;
import com.wj.basecomponent.util.fw.basictype.FWString;
import com.wj.basecomponent.util.fw.basictype.FWUnsignedInt;
import com.wj.basecomponent.util.fw.basictype.FWUnsignedShort;

public class WaveHeadJava {

    @FWType(fieldOrder = 1, size = 4)
    public FWString riffChunkId = new FWString("RIFF");


    @FWType(fieldOrder = 2)
    public FWUnsignedInt riffChunkDataSize;

    @FWType(fieldOrder = 3, size = 4)
    public FWString format = new FWString("WAVE");

    @FWType(fieldOrder = 4, size = 4)
    public FWString fmtChunkId = new FWString("fmt ", 4);

// fmt chunk的data大小：存储PCM数据时，是16

    @FWType(fieldOrder = 5)
    public FWUnsignedInt fmtChunkDataSize = new FWUnsignedInt(16L);
    // 音频编码，1表示PCM，3表示Floating Point

    @FWType(fieldOrder = 6)
    public FWUnsignedShort audioFormat = new FWUnsignedShort(1);

    @FWType(fieldOrder = 7)
    public FWUnsignedShort numChannels;

    @FWType(fieldOrder = 8)
    public FWUnsignedInt simpleRate;

    @FWType(fieldOrder = 9)
    public FWUnsignedInt byteRate;

    @FWType(fieldOrder = 10)
    public FWUnsignedShort blockAlign;

    @FWType(fieldOrder = 11)
    public FWUnsignedShort bitsPerSample;

    @FWType(fieldOrder = 12, size = 4)
    public FWString dateChunkId = new FWString("data");

    @FWType(fieldOrder = 13)
    public FWUnsignedInt dataChunkDataSize;

    @Override
    public String toString() {
        return "WaveHeadJava{" +
                "riffChunkId=" + riffChunkId +
                ", riffChunkDataSize=" + riffChunkDataSize +
                ", format=" + format +
                ", fmtChunkId=" + fmtChunkId +
                ", fmtChunkDataSize=" + fmtChunkDataSize +
                ", audioFormat=" + audioFormat +
                ", numChannels=" + numChannels +
                ", simpleRate=" + simpleRate +
                ", byteRate=" + byteRate +
                ", blockAlign=" + blockAlign +
                ", bitsPerSample=" + bitsPerSample +
                ", dateChunkId=" + dateChunkId +
                ", dataChunkDataSize=" + dataChunkDataSize +
                '}';
    }
}
