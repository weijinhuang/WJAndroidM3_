//
// Created by HWJ on 2022/11/27.
//

#ifndef WJANDROIDM3_FF_RTMP_PUSHER_H
#define WJANDROIDM3_FF_RTMP_PUSHER_H

#include "wj_log.h"

#ifdef __cplusplus
extern "C" {
#endif
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include <libavutil/time.h>
#ifdef __cplusplus
}
#endif

class FFRtmpPusher {

private:
    AVFormatContext *inFormatCtx;
    AVFormatContext *outFormatCtx;

    int video_index = -1;
    int audio_index = -1;

    AVPacket packet;

public:
    int open(const char *inputPath, const char *outputPath);

    int push();

    void close();
};

#endif //WJANDROIDM3_FF_RTMP_PUSHER_H

