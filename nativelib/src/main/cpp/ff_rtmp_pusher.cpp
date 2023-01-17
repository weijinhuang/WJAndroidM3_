//
// Created by HWJ on 2022/11/27.
//
#include "ff_rtmp_pusher.h"

int FFRtmpPusher::open(const char *inputPath, const char *outputPath) {
    int ret = 0;
    avformat_network_init();
    ret = avformat_open_input(&inFormatCtx, inputPath, nullptr, nullptr);
    if (ret < 0) {
        LOGE(LOG_TAG, "avformat_open_input error %s", av_err2str(ret));
        return ret;
    }
    avformat_find_stream_info(inFormatCtx, nullptr);
    av_dump_format(inFormatCtx, 0, inputPath, 0);
    //分配输出流到对应AVFormatContext
    ret = avformat_alloc_output_context2(&outFormatCtx, nullptr, "flv", outputPath);
    if (ret < 0) {
        LOGE(LOG_TAG, "avformat_alloc_output_context2 error %s", av_err2str(ret));
        return ret;
    }
    //构造输出流的AVStream
    for (int i = 0; i < inFormatCtx->nb_chapters; ++i) {
        AVStream *in_stream = inFormatCtx->streams[i];
        const AVCodec *codec = avcodec_find_encoder(in_stream->codecpar->codec_id);
        AVStream *out_stream = avformat_new_stream(outFormatCtx, codec);
        avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
        out_stream->codecpar->codec_tag = 0;
        if(AVMEDIA_TYPE_AUDIO == in_stream->codecpar->codec_type && audio_index == -1){
            audio_index = i;
        }
        if(AVMEDIA_TYPE_VIDEO == in_stream->codecpar->codec_type){
            video_index = i;
        }
    }
    if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open2(&outFormatCtx->pb, outputPath, AVIO_FLAG_WRITE, nullptr, nullptr);
        if (ret < 0) {
            LOGE(LOG_TAG, "avio_open2 error %s", av_err2str(ret));
            return ret;
        }
    }
    //写媒体头
    ret = avformat_write_header(outFormatCtx, nullptr);
    if (ret < 0) {
        LOGE(LOG_TAG, "avformat_write_header %s", av_err2str(ret));
        return ret;
    }
}

void rescale(AVFormatContext *in_format_ctx, AVFormatContext *out_format_ctx, AVPacket *packet) {
    AVStream *inStream = in_format_ctx->streams[packet->stream_index];
    AVStream *outStream = out_format_ctx->streams[packet->stream_index];
    packet->pts = av_rescale_q(packet->pts, inStream->time_base, outStream->time_base);
    packet->dts = av_rescale_q(packet->pts, inStream->time_base, outStream->time_base);
    packet->duration = av_rescale_q(packet->duration, inStream->time_base, outStream->time_base);
    packet->pos = -1;

}

int FFRtmpPusher::push() {
    int ret = 0;

    int frameCount = 0;
    int64_t startTime = av_gettime();
    while (true) {
        //读取音视频帧
        ret = av_read_frame(inFormatCtx, &packet);
        if (ret < 0) {
            LOGE(LOG_TAG, "av_read_frame error:%s", av_err2str(ret));
            return ret;
        }
        if(packet.stream_index != video_index && packet.stream_index!= audio_index){
            continue;
        }
        //根据pts控制推流速度
        AVRational time_base = inFormatCtx->streams[packet.stream_index]->time_base;
        int64_t pts_time = av_rescale_q(packet.pts, time_base, AV_TIME_BASE_Q);
        int64_t cur_time = av_gettime() - startTime;
        if (pts_time > cur_time) {
            av_usleep(pts_time - cur_time);
        }
        rescale(inFormatCtx, outFormatCtx, &packet);
        ret = av_interleaved_write_frame(outFormatCtx, &packet);
        if (ret < 0) {
            LOGE(LOG_TAG, "av_interleaved_write_frame ERROR %S", av_err2str(ret));
            break;
        }
        if (frameCount % 10 == 0) {
            LOGI(LOG_TAG, "write frame = %d", frameCount);
        }
        av_packet_unref(&packet);
    }
    return ret;
}

void FFRtmpPusher::close() {
    if (inFormatCtx) {
        avformat_close_input(&inFormatCtx);
        inFormatCtx = nullptr;
    }
    if (outFormatCtx) {
        av_write_trailer(outFormatCtx);
        avformat_close_input(&outFormatCtx);
        outFormatCtx = nullptr;
    }

}