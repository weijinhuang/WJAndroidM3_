//
// Created by HWJ on 2022/11/17.
//

#ifndef WJANDROIDM3_WJ_AUDIO_PLAYER_H
#define WJANDROIDM3_WJ_AUDIO_PLAYER_H


#ifdef __cplusplus
extern "C" {
#endif
#include <jni.h>
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#ifdef __cplusplus
}
#endif


class WJAudioPlayer {
private:
    AVFormatContext *formatContext;
    AVCodecContext *codecContext;

    int audio_index = -1;

    int out_sample_rate;
    enum AVSampleFormat out_sample_fmt;
    int out_ch_layout;
    int out_channel;
    SwrContext *swrContext;

    AVPacket *packet;
    AVFrame *frame;
    uint8_t *out_buffer;
public:

    int getSimpleRate() const;

    int getChannel() const;

    int open(const char *filePath);

    int decodeAudio();

    uint8_t *getDecodeFrame() const;

    void close();
};

#endif //WJANDROIDM3_WJ_AUDIO_PLAYER_H
