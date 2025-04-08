//
// Created by HWJ on 2025/1/9.
//

#ifndef WJANDROIDM3_WJAUDIORESAMPLE_H
#define WJANDROIDM3_WJAUDIORESAMPLE_H

extern "C" {
#include "libswresample//swresample.h"
#include "libavutil/avutil.h"
#include "libavutil/opt.h"
#include "wj_log.h"
}

#define ERROR_BUF(ret) \
char errbuf[1024];     \
av_strerror(ret, errbuf, sizeof (errbuf));\

class WJAudioReSample {

public:
    WJAudioReSample();

    ~WJAudioReSample();

    int startResample(const char *inFilePath,int src_sample_rate, int src_channel_count, int src_sample_format, const char *outFilePath,int dst_sample_rate, int dst_channel_count, int dst_sample_format);

    int startResample2(const char *inFilePath, const char *outFilePath);

    int writeFile(const char *dstFile);

    int readFile(const char *dstFile);
};

#endif //WJANDROIDM3_WJAUDIORESAMPLE_H
