package com.wj.androidm3.business.ui.media.audio;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.wj.basecomponent.util.log.WJLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder {

    private static final String TAG = "AudioEncoder";
    private static final int SAMPLE_RATE = 44100; // 采样率
    private static final int CHANNEL_COUNT = 1; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // PCM 16位
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private Thread encodingThread;
    private volatile boolean isEncoding = false;
    private int trackIndex = -1;
    private boolean hasError = false;

    private boolean checkPermission(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return false;
        }
        return true;
    }

    public void startEncoding(Activity activity) {

        boolean hasPermission = checkPermission(activity);
        if (!hasPermission) {
            return;
        }

        try {
            // 初始化 AudioRecord
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT, BUFFER_SIZE);
            initMediaCodec();

            // 初始化 MediaMuxer
            File outputFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "encoded_audio.aac");
            if (outputFile.exists()) {
                outputFile.delete();
            }
            WJLog.Companion.d("创建录音文件：" + outputFile.getAbsolutePath());
            mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            isEncoding = true;
            encodingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    encodeAudio();
                }
            });
            encodingThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting encoding: " + e.getMessage());
            hasError = true;
        }
    }

    private void initMediaCodec() throws IOException {
        // 初始化 MediaCodec
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.KEY_MIME, SAMPLE_RATE, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000); // 比特率
        mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void stopEncoding() {
        isEncoding = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        if (encodingThread != null) {
            try {
                encodingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void encodeAudio() {
        audioRecord.startRecording();
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (isEncoding && !hasError) {
            // 获取可用的输入缓冲区
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();

                // 从 AudioRecord 读取 PCM 数据
                int bytesRead = audioRecord.read(inputBuffer, inputBuffer.capacity());
                if (bytesRead > 0) {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead, System.nanoTime() / 1000, 0);
                }
            }

            // 获取编码后的输出缓冲区
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (trackIndex == -1) {
                        MediaFormat format = mediaCodec.getOutputFormat();
                        trackIndex = mediaMuxer.addTrack(format);
                        mediaMuxer.start();
                    }
                    mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }

        if (trackIndex != -1) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }
}