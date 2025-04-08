package com.wj.nativelib;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.provider.MediaStore;

public class WJMediaJNIHepler {

    static {
        System.loadLibrary("nativelib");
    }

    private AudioTrack audioTrack;

    public native void playAudio(String filePath);


    public native void audioResample(String inputPath, String outputPath, int sampleRate);

    public AudioTrack createAudioTrack(int sampleRate, int channel) {
        int channelConfig = channel == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channel, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
        return audioTrack;
    }

    public void releaseAudioTrack() {
        if (audioTrack != null) {
            audioTrack.release();
        }
    }

    private native int push(String inputPath, String outputPath);

    public int pushStream(String inputPath, String outputPath) {
        return push(inputPath, outputPath);
    }


    public native int jinTest();

    /**
     *
     * @param srcPCMFilePath
     * @param srcSampleRate
     * @param srcChannelCount
     * @param srcSampleFormat
     * @param dstPCMFile
     * @param dstSampleRate
     * @param dstChannelCount  1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO
     * @param dstSampleFormat 1->AV_SAMPLE_FMT_FLT else-> AV_SAMPLE_FMT_S16
     */
    public native void WJAudioResample(String srcPCMFilePath, int srcSampleRate, int srcChannelCount, int srcSampleFormat,
                                       String dstPCMFile, int dstSampleRate, int dstChannelCount, int dstSampleFormat);
}
