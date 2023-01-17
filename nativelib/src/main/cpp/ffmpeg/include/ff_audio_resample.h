//
// Created by HWJ on 2022/11/22.
//

#ifndef WJANDROIDM3_FF_AUDIO_RESAMPLE_H
#define WJANDROIDM3_FF_AUDIO_RESAMPLE_H

#ifdef __cplusplus
extern "C"{
#endif

#include "libavformat/avformat.h"

#include "libavformat/avio.h"

#include "libavcodec/avcodec.h"

#include "libswresample/swresample.h"

#include "libavutil/audio_fifo.h"
#include "wj_log.h"
#ifdef __cplusplus
};

#endif


struct AudioResample {
    int64_t pts = 0;
    AVPacket inPacket;
    AVPacket outPacket;
    AVFrame *inFrame;
    AVFrame *outFrame;
    SwrContext *resampleCtx;
    AVAudioFifo *fifo;
    AVFormatContext *inFormatCtx;
    AVFormatContext *outFormatCtx;
    AVCodecContext *inCodecCtx;
    AVCodecContext *outCodecCtx;
};

class FFAudioResample {
private:
    AudioResample *resample;

    int openInput(const char *inputPath);

    int openOutput(const char *outputPath,int sample_rate);

    int decodeAudioFrame(AVFrame *frame, int *data_present, int *finished);

    int decodeAndCovert(int *finished);

    int encodeAudioFrame(AVFrame *frame, int *data_present);

    int encodeAndWrite();

public:
    FFAudioResample();

    ~FFAudioResample();

    int resampling(const char *inputPath, const char *outputPath, int sampleRate);
};

#endif //WJANDROIDM3_FF_AUDIO_RESAMPLE_H
