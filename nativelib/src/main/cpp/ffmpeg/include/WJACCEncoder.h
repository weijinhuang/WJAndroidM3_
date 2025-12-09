//
// Created by HWJ on 2023/3/7.
//

#ifndef WJANDROIDM3_WJACCENCODER_H
#define WJANDROIDM3_WJACCENCODER_H
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "wj_log.h"
#include "libavutil/opt.h"
};

class WJACCEncoder {
private:
    int index = 0;
    int bufferSize = 0;

    AVFormatContext *pFormatCtx = nullptr;
    AVOutputFormat *fmt = nullptr;
    AVStream *audioStream = nullptr;
    AVCodecContext *pCodecCtx = nullptr;
    AVCodec *pCodec = nullptr;

    uint8_t *audioBuffer = nullptr;
    AVFrame *audioFrame = nullptr;
    AVPacket audioPacket;

    SwrContext *swr = nullptr;

    AVSampleFormat in_sample_fmt = AV_SAMPLE_FMT_S16;
    int in_channels = 2;
    int in_sample_rate = 44100;
    int64_t in_channel_layout = AV_CH_LAYOUT_STEREO;

    int EncodeFrame(AVCodecContext *pCodecCtx, AVFrame *pFrame);

public:
    int EncodeStart(const char *aacPath, int sampleRate, int channelCount, int sampleFormat);
    int EncodeBuffer(const unsigned char *pcmBuffer, int length);
    int EncodeStop();
};

#endif //WJANDROIDM3_WJACCENCODER_H
