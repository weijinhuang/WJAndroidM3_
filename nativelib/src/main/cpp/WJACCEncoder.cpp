//
// Created by HWJ on 2023/3/7.
//

#include "ffmpeg/include/WJACCEncoder.h"

int WJACCEncoder::EncodeFrame(AVCodecContext *pCodecCtx, AVFrame *pFrame) {
    int ret = avcodec_send_frame(pCodecCtx, pFrame);
    if (ret < 0) {
        char err[256] = {0};
        av_strerror(ret, err, sizeof(err));
        LOGE(LOG_TAG, "发送帧数据到编码器失败,%d(%s)", ret, err);
        return -1;
    } else {
        LOGI(LOG_TAG, "发送帧数据到编码器成功");
    }
    while (avcodec_receive_packet(pCodecCtx, &audioPacket) == 0) {
        audioPacket.stream_index = audioStream->index;
        ret = av_interleaved_write_frame(pFormatCtx, &audioPacket);
        if (ret == 0) {
            LOGI(LOG_TAG, "帧数据编码成功");
        }else{
            LOGE(LOG_TAG,"幀數據編碼失敗,%d", ret);

        }
        av_packet_unref(&audioPacket);
    }
    return ret;
}

int WJACCEncoder::EncodeStart(const char *aacPath, int sampleRate, int channelCount, int sampleFormat) {
    LOGI(LOG_TAG, "JNI::WJACCEncoder::EncodeStart(const char *aacPath) %s", aacPath);
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
    //输入参数保存
    in_sample_rate = sampleRate;
    in_channels = channelCount;
    in_channel_layout = (channelCount == 1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO);
    switch (sampleFormat) {
        case 3: // ENCODING_PCM_8BIT
            in_sample_fmt = AV_SAMPLE_FMT_U8;
            break;
        case 2: // ENCODING_PCM_16BIT
            in_sample_fmt = AV_SAMPLE_FMT_S16;
            break;
        default: // 24-bit packed or others
            in_sample_fmt = AV_SAMPLE_FMT_S32;
            break;
    }

    //设置编码器参数
    LOGI(LOG_TAG, "JNI 设置编码器参数,sample_rate:%d channels:%d sample_fmt:%s bit_rate:%d", sampleRate, channelCount, "AV_SAMPLE_FMT_FLTP", 96000);
    pCodecCtx = audioStream->codec;
    pCodecCtx->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    pCodecCtx->codec_id = fmt->audio_codec;
    pCodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
    pCodecCtx->sample_fmt = AV_SAMPLE_FMT_FLTP;
    pCodecCtx->sample_rate = sampleRate;
    pCodecCtx->channel_layout = in_channel_layout;
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
    audioFrame->format = pCodecCtx->sample_fmt;
    audioFrame->channel_layout = pCodecCtx->channel_layout;
    audioFrame->sample_rate = pCodecCtx->sample_rate;
    audioFrame->channels = pCodecCtx->channels;
    audioFrame->nb_samples = pCodecCtx->frame_size;
    // time base for pts
    pCodecCtx->time_base.num = 1;
    pCodecCtx->time_base.den = pCodecCtx->sample_rate;
    // allocate internal buffers for frame
    int retBuf = av_frame_get_buffer(audioFrame, 0);
    if (retBuf < 0) {
        LOGE(LOG_TAG, "分配音频帧缓冲失败: %d", retBuf);
        return retBuf;
    }
    //写文件头
    int writeHeaderRet = avformat_write_header(pFormatCtx, nullptr);
    if (writeHeaderRet < 0) {
        LOGE(LOG_TAG, "写文件头失败：%d", writeHeaderRet);
    }
    bufferSize = av_samples_get_buffer_size(nullptr, pCodecCtx->channels, pCodecCtx->frame_size, pCodecCtx->sample_fmt, 1);
    av_new_packet(&audioPacket, bufferSize);
    //音频转码
    swr = swr_alloc();
    av_opt_set_channel_layout(swr, "in_channel_layout", in_channel_layout, 0);
    av_opt_set_channel_layout(swr, "out_channel_layout", pCodecCtx->channel_layout, 0);
    av_opt_set_int(swr, "in_sample_rate", in_sample_rate, 0);
    av_opt_set_int(swr, "out_sample_rate", pCodecCtx->sample_rate, 0);
    av_opt_set_sample_fmt(swr, "in_sample_fmt", in_sample_fmt, 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt", pCodecCtx->sample_fmt, 0);
    swr_init(swr);
    return 0;
}

int WJACCEncoder::EncodeBuffer(const unsigned char *pcmBuffer, int length) {
    int in_bps = av_get_bytes_per_sample(in_sample_fmt);
    int in_samples = (in_bps > 0 && in_channels > 0) ? length / (in_bps * in_channels) : 0;
    if (in_samples <= 0) {
        LOGE(LOG_TAG, "输入样本数无效: len=%d", length);
        return -1;
    }
    // 最大可写入的输出样本数
    int max_out = audioFrame->nb_samples;
    int out_count = swr_convert(swr, audioFrame->data, max_out, &pcmBuffer, in_samples);
    if (out_count < 0) {
        char errorMsg[1024] = {0};
        av_strerror(out_count, errorMsg, sizeof(errorMsg));
        LOGE(LOG_TAG, "重采样失败:%s", errorMsg);
        return -1;
    }
    audioFrame->nb_samples = out_count;
    audioFrame->pts = index;
    index += out_count;
    return EncodeFrame(pCodecCtx, audioFrame);
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
