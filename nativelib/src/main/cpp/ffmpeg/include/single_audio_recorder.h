//
// Created by HWJ on 2023/1/31.
//

#ifndef WJANDROIDM3_SINGLE_AUDIO_RECORDER_H
#define WJANDROIDM3_SINGLE_AUDIO_RECORDER_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

#include "thread"
#include "wj_log.h"
#include "ThreadSafeQueue.h"
#include "AudioFrame.h"

using namespace std;
#define DEFAULT_SAMPLE_RATE  44100
#define DEFAULT_CHANNEL_LAYOUT AV_CH_LAYOUT_STEREO


class SingleAudioRecorder {
public:
    SingleAudioRecorder(const char *outUrl, int sampleRate, int channelLayout, int sampleFormat);

    ~SingleAudioRecorder();

    int StartRecord();

    int OnFrame2Encode(AudioFrame *inputFrame);

    int StopRecord();

private:
    static void StartACCEncoderThread(SingleAudioRecorder *context);

    int EncodeFrame(AVFrame *pFrame);

private:
    ThreadSafeQueue<AudioFrame *> m_frameQueue;
    char m_outUrl[1024] = {0};
    int m_frameIndex = 0;
    int m_sampleRate;
    int m_channelLayout;
    int m_sampleFormat;
    AVPacket m_avPacket;
    AVFrame *m_PFrame = nullptr;
    uint8_t *m_pFrameBuffer = nullptr;
    int m_frameBufferSize;
    AVCodec *m_pCodec = nullptr;
    AVStream *m_pStream = nullptr;
    AVCodecContext *m_pCodecCtx = nullptr;
    AVFormatContext *m_pFormatCtx = nullptr;
    SwrContext *m_swrCtx = nullptr;
    thread *m_encodeThread = nullptr;
    volatile int m_exit = 0;
};

#endif //WJANDROIDM3_SINGLE_AUDIO_RECORDER_H
