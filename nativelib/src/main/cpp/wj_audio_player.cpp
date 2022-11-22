//
// Created by HWJ on 2022/11/17.
//

#include "ffmpeg/include/wj_audio_player.h"
#include "wj_log.h"

#define AUDIO_TAG "AudioPlayer"
#define BUFFER_SIZE (48000 * 10)

int WJAudioPlayer::getChannel() const {
    return out_channel;
}

int WJAudioPlayer::getSimpleRate() const {
    return out_sample_rate;
}

uint8_t *WJAudioPlayer::getDecodeFrame() const {
    return out_buffer;
}

int WJAudioPlayer::open(const char *filePath) {
    if (!filePath) {
        return -1;
    }
    int ret = avformat_open_input(&formatContext, filePath, nullptr, nullptr);
    const AVCodec *codec;

    frame = av_frame_alloc();
    packet = av_packet_alloc();
    out_buffer = new uint8_t[BUFFER_SIZE];


    if (ret < 0) {
        LOGE(AUDIO_TAG, "avformat_open_input error %s", av_err2str(ret));
        return ret;
    }
    avformat_find_stream_info(formatContext, nullptr);//查找stream信息
    for (int i = 0; i < formatContext->nb_streams; i++) {
        if (AVMEDIA_TYPE_AUDIO == formatContext->streams[i]->codecpar->codec_type) {
            audio_index = i;
            break;
        }
    }
    //找到codec
    if (audio_index == -1) {
        return -1;
    }
    codec = avcodec_find_decoder(formatContext->streams[audio_index]->codecpar->codec_id);
    codecContext = avcodec_alloc_context3(codec);
    //拷贝数据
    avcodec_parameters_to_context(codecContext, formatContext->streams[audio_index]->codecpar);
    ret = avcodec_open2(codecContext, codec, nullptr);
    if (ret < 0) {
        LOGE(AUDIO_TAG, "avcodec_open2 error=%s", av_err2str(ret));
        return ret;
    }
    //输入：采样率、声道布局、音频格式
    int in_sample_rate = codecContext->sample_rate;
    auto in_sample_fmt = codecContext->sample_fmt;
    int in_ch_layout = codecContext->channel_layout;
    LOGE(AUDIO_TAG, "SAMPLE RATE = %d", in_sample_rate);
    LOGE(AUDIO_TAG, "SAMPLE FMT = %d", in_sample_fmt);
    LOGE(AUDIO_TAG, "CH LAYOUT = %d", in_ch_layout);
    out_sample_rate = in_sample_rate;
    out_sample_fmt = AV_SAMPLE_FMT_S16;
    out_ch_layout = AV_CH_LAYOUT_STEREO;
    out_channel = codecContext->channels;
    swrContext = swr_alloc();
    swr_alloc_set_opts(swrContext, out_ch_layout, out_sample_fmt, out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0, nullptr);
    swr_init(swrContext);
    return 0;
}

int WJAudioPlayer::decodeAudio() {
    int ret;
    //解封装
    ret = av_read_frame(formatContext, packet);
    if (ret < 0) {
        LOGE(AUDIO_TAG, "解封装ERROR:%s", av_err2str(ret));
        return ret;
    }
    if (packet->stream_index != audio_index) {
        return 0;
    }
    ret = avcodec_send_packet(codecContext, packet);

    if (ret < 0) {
        LOGE(AUDIO_TAG, "解封装ERROR:%s", av_err2str(ret));
        return ret;
    }
    ret = avcodec_receive_frame(codecContext, frame);
    if (ret < 0) {
        if (ret == AVERROR(EAGAIN)) {
            return 0;
        } else {
            return ret;
        }
    }
    swr_convert(swrContext, &out_buffer, BUFFER_SIZE, (const uint8_t **) frame->data, frame->nb_samples);
    int buffer_size = av_samples_get_buffer_size(nullptr, out_channel, frame->nb_samples, out_sample_fmt, 1);
    LOGI(AUDIO_TAG, "buffer_size=%d", buffer_size);
    av_frame_unref(frame);
    av_packet_unref(packet);
    return buffer_size;
}

void WJAudioPlayer::close() {
    if (formatContext) {
        avformat_close_input(&formatContext);
    }
    if (codecContext) {
        avcodec_free_context(&codecContext);
    }
    if (packet) {
        av_packet_free(&packet);
    }
    if (frame) {
        av_frame_free(&frame);
    }
    delete[] out_buffer;

}