//
// Created by HWJ on 2023/3/7.
//

#include "ffmpeg/include/WJACCEncoder.h"

int WJACCEncoder::EncodeFrame(AVCodecContext *pCodecCtx, AVFrame *pFrame) {
    int ret = avcodec_send_frame(pCodecCtx, audioFrame);
    if (ret < 0) {
        LOGE(LOG_TAG, "发送帧数据到编码器失败,%d", ret);
        return -1;
    } else {
        LOGI(LOG_TAG, "发送帧数据到编码器成功");
    }
    while (avcodec_receive_packet(pCodecCtx, &audioPacket) == 0) {
        audioPacket.stream_index = audioStream->index;
        ret = av_interleaved_write_frame(pFormatCtx, &audioPacket);
        if (ret == 0) {
            LOGI(LOG_TAG, "帧数据编码成功");
        }
        av_packet_unref(&audioPacket);
    }
    return ret;
}

int WJACCEncoder::EncodeStart(const char *aacPath) {
    LOGI(LOG_TAG, "WJACCEncoder::EncodeStart(const char *aacPath) %s", aacPath);
    //注册组件
    av_register_all();
    //获取输出文件的上下文环境
    avformat_alloc_output_context2(&pFormatCtx, nullptr, nullptr, aacPath);
    fmt = pFormatCtx->oformat;
    //打开输出文件
    int ret = avio_open(&pFormatCtx->pb, aacPath, AVIO_FLAG_READ_WRITE);
    if (ret < 0) {
        LOGE(LOG_TAG, "打开文件失败 %d ", ret);
        return -1;
    }
    audioStream = avformat_new_stream(pFormatCtx, nullptr);
    if (audioStream == nullptr) {
        LOGE(LOG_TAG, "创建输出流失败");
        return -1;
    }
    pCodec = avcodec_find_encoder(fmt->audio_codec);
    if (pCodec == nullptr) {
        LOGE(LOG_TAG, "未能找到编码器");
        return -1;
    }
    //设置编码器参数
    pCodecCtx = audioStream->codec;
    pCodecCtx->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    pCodecCtx->codec_id = fmt->audio_codec;
    pCodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
    pCodecCtx->sample_fmt = AV_SAMPLE_FMT_FLTP;
    pCodecCtx->sample_rate = 44100;
    pCodecCtx->channel_layout = AV_CH_LAYOUT_STEREO;
    pCodecCtx->channels = av_get_channel_layout_nb_channels(pCodecCtx->channel_layout);
    pCodecCtx->bit_rate = 96000;
    if (pFormatCtx->oformat->flags & AVFMT_GLOBALHEADER) {
        pCodecCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    }
    //打开音频编码器
    int result = avcodec_open2(pCodecCtx, pCodec, nullptr);
    if (result < 0) {
        LOGE(LOG_TAG, "打开编码器失败");
        return -1;
    }

    audioFrame = av_frame_alloc();
    audioFrame->nb_samples = pCodecCtx->frame_size;
    audioFrame->format = pCodecCtx->sample_fmt;

    bufferSize = av_samples_get_buffer_size(nullptr, pCodecCtx->channels, pCodecCtx->frame_size, pCodecCtx->sample_fmt, 1);
    audioBuffer = (uint8_t *) av_malloc(bufferSize);
    avcodec_fill_audio_frame(audioFrame, pCodecCtx->channels, pCodecCtx->sample_fmt, (const uint8_t *) audioBuffer, bufferSize, 1);
    //写文件头
    avformat_write_header(pFormatCtx, nullptr);
    av_new_packet(&audioPacket, bufferSize);
    //音频转码
    swr = swr_alloc();

    av_opt_set_channel_layout(swr, "in_channel_layout", AV_CH_LAYOUT_STEREO, 0);
    av_opt_set_channel_layout(swr, "out_channel_layout", AV_CH_LAYOUT_STEREO, 0);
    av_opt_set_int(swr, "in_sample_rate", 44100, 0);
    av_opt_set_int(swr, "out_sample_rate", 44100, 0);
    av_opt_set_sample_fmt(swr, "in_sample_fmt", AV_SAMPLE_FMT_S16, 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt", AV_SAMPLE_FMT_FLTP, 0);
    swr_init(swr);
    return 0;
}

int WJACCEncoder::EncodeBuffer(const unsigned char *pcmBuffer, int length) {
    LOGI(LOG_TAG, "native层接收到音频数据：size:%d", length);
    uint8_t *outs[2];
    outs[0] = new uint8_t[length];
    outs[1] = new uint8_t[length];
    int count = swr_convert(swr, (uint8_t **) &outs, length * 4, &pcmBuffer, length / 4);
    audioFrame->data[0] = outs[0];
    audioFrame->data[1] = outs[1];
    if (count >= 0) {
        EncodeFrame(pCodecCtx, audioFrame);
    } else {
        char errorMsg[1024] = {0};
        av_strerror(length, errorMsg, sizeof(errorMsg));
        LOGE(LOG_TAG, "转码失败:%s", errorMsg);
    }
    delete outs[0];
    delete outs[1];
    return 0;
}

int WJACCEncoder::EncodeStop() {
    EncodeFrame(pCodecCtx, nullptr);
    //写文件尾
    av_write_trailer(pFormatCtx);

    avcodec_close(pCodecCtx);
    av_free(audioFrame);
    av_free(audioBuffer);
    avio_close(pFormatCtx->pb);
    avformat_free_context(pFormatCtx);
    return 0;
}
