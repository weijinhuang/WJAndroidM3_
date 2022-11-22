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
}
