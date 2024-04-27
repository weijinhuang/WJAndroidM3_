package com.wj.nativelib

import com.wj.basecomponent.util.BufferConverter
import com.wj.basecomponent.util.fw.basictype.FWUnsignedInt
import com.wj.basecomponent.util.fw.basictype.FWUnsignedShort
import com.wj.nativelib.bean.WaveHead
import com.wj.nativelib.bean.WaveHeadJava
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//        val wavHead = WaveHeadJava().apply {
//            numChannels = FWUnsignedShort(2)
//            simpleRate = FWUnsignedInt(8_000)
//            bitsPerSample = FWUnsignedShort(16)
//            blockAlign = FWUnsignedShort(bitsPerSample.value * numChannels.value / 8)
//            byteRate = FWUnsignedInt(8_000L * blockAlign.value)
//            dataChunkDataSize = FWUnsignedInt(10244 - 44)
//            riffChunkDataSize = FWUnsignedInt(dataChunkDataSize.value + 44 - 8)
//
//        }
//        val buffer = BufferConverter.getBuffer(wavHead)
//        println(buffer.size)
//        println(buffer.contentToString())
        val byteArray = byteArrayOf(82, 73, 70, 70, 36, 12, 26, 0, 87, 65, 86, 69, 102, 109, 116, 0, 16, 0, 0, 0, 1, 0, 2, 0, 68, -84, 0, 0, 16, -79, 2, 0, 4, 0, 16, 0, 100, 97, 116, 97, 0, 12, 26, 0)
        val waveHeadJava = BufferConverter.getObject(byteArray, WaveHeadJava::class.java)
        println(waveHeadJava)
    }
}