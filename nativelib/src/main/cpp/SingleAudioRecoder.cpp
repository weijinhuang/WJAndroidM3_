//
// Created by HWJ on 2023/2/1.
//

#include <unistd.h>
#include "single_audio_recorder.h"

SingleAudioRecorder::SingleAudioRecorder(const char *outUrl, int sampleRate, int channelLayout,
                                         int sampleFormat) {
    LOGI(LOG_TAG,
         "SingleAudioRecorder::SingleAudioRecorder outUrl=%s, sampleRate=%d, channelLayout=%d, sampleFormat=%d",
         outUrl, sampleRate, channelLayout, sampleFormat);
    strcpy(m_outUrl, outUrl);
    m_sampleRate = sampleRate;
    m_channelLayout = channelLayout;
    m_sampleFormat = sampleFormat;
}

SingleAudioRecorder::~SingleAudioRecorder() {}

int SingleAudioRecorder::StartRecord() {
    int result = -1;
    do {
        result = avformat_alloc_output_context2(&m_pFormatCtx, nullptr, nullptr, m_outUrl);
        if (result < 0) {
            LOGE(LOG_TAG, "SingleAudioRecorder::StartRecord avformat_alloc_output_context2 ret=%d", result);
            break;
        }
        result = avio_open(&m_pFormatCtx->pb, m_outUrl, AVIO_FLAG_READ_WRITE);
        if (result < 0) {
            LOGE(LOG_TAG, "SingleAudioRecorder::StartRecord avio_open ret=%d", result);
            break;
        }
        m_pStream = avformat_new_stream(m_pFormatCtx, nullptr);
        if (m_pStream == nullptr) {
            result = -1;
            LOGE(LOG_TAG, "SingleAudioRecorder::StartRecord avformat_new_stream fail. ret=%d", result);
            break;
        }

        AVOutputFormat *avOutputFormat = m_pFormatCtx->oformat;
        m_pCodec = avcodec_find_encoder(avOutputFormat->audio_codec);
        if (m_pCodec == nullptr) {
            result = -1;
            LOGE(LOG_TAG, "SingleAudioRecorder::StartRecord avcodec_find_encoder fail. ret=%d", result);
            break;
        }
        m_pCodecCtx = m_pStream->codec;
        m_pCodecCtx->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
        LOGI(LOG_TAG, "SingleAudioRecorder::StartRecord avOutputFormat->audio_codec=%d", avOutputFormat->audio_codec);
        m_pCodecCtx->codec_id = AV_CODEC_ID_AAC;
        m_pCodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
        m_pCodecCtx->sample_fmt = AV_SAMPLE_FMT_FLTP;//float planar 4字节
        m_pCodecCtx->sample_rate = DEFAULT_SAMPLE_RATE;
        m_pCodecCtx->channel_layout = DEFAULT_CHANNEL_LAYOUT;
        m_pCodecCtx->channels = av_get_channel_layout_nb_channels(m_pCodecCtx->channel_layout);
        m_pCodecCtx->bit_rate = 96000;
        result = avcodec_open2(m_pCodecCtx, m_pCodec, nullptr);
        if (result < 0) {
            LOGE(LOG_TAG, "SingleAudioRecorder::StartRecord avcodec_open2 ret=%d", result);
            break;
        }

        av_dump_format(m_pFormatCtx, 0, m_outUrl, 1);

        m_PFrame = av_frame_alloc();
        m_PFrame->nb_samples = m_pCodecCtx->frame_size;
        m_PFrame->format = m_pCodecCtx->sample_fmt;
        m_PFrame->channel_layout = m_pCodecCtx->channel_layout;
        m_frameBufferSize = av_samples_get_buffer_size(nullptr, m_pCodecCtx->channels,
                                                       m_pCodecCtx->frame_size,
                                                       m_pCodecCtx->sample_fmt, 1);
        LOGI(LOG_TAG, "SingleAudioRecorder::StartRecord m_frameBufferSize=%d, nb_samples=%d", m_frameBufferSize, m_PFrame->nb_samples);
        m_pFrameBuffer = (uint8_t *) av_malloc(m_frameBufferSize);
        avcodec_fill_audio_frame(m_PFrame, m_pCodecCtx->channels, m_pCodecCtx->sample_fmt, (const uint8_t *) m_pFrameBuffer, m_frameBufferSize, 1);

        //写文件头
        av_new_packet(&m_avPacket, m_frameBufferSize);

        //音频转码
        m_swrCtx = swr_alloc();
        av_opt_set_channel_layout(m_swrCtx, "in_channel_layout", m_channelLayout, 0);
        av_opt_set_channel_layout(m_swrCtx, "out_channel_layout", AV_CH_LAYOUT_STEREO, 0);
        av_opt_set_int(m_swrCtx, "in_sample_rate", m_sampleRate, 0);
        av_opt_set_int(m_swrCtx, "out_sample_rate", DEFAULT_SAMPLE_RATE, 0);
        av_opt_set_sample_fmt(m_swrCtx, "in_sample_fmt", AVSampleFormat(m_sampleFormat), 0);
        av_opt_set_sample_fmt(m_swrCtx, "out_sample_fmt", AV_SAMPLE_FMT_FLTP, 0);
        swr_init(m_swrCtx);
    } while (false);

    if (result >= 0) {
        LOGI(LOG_TAG, "JIN创建录制线程");
        m_encodeThread = new thread(StartACCEncoderThread, this);
    }
    return 0;
}

int SingleAudioRecorder::OnFrame2Encode(AudioFrame *inputFrame) {
    LOGI(LOG_TAG,
         "SingleAudioRecorder::OnFrame2Encode inputFrame->data=%p, inputFrame->dataSize=%d",
         inputFrame->data, inputFrame->dataSize);
    if (m_exit) return 0;
    auto *pAudioFrame = new AudioFrame(inputFrame->data, inputFrame->dataSize);
    m_frameQueue.push(pAudioFrame);
    return 0;
}

int SingleAudioRecorder::StopRecord() {
    m_exit = 1;
    if (m_encodeThread) {
        m_encodeThread->join();
        delete m_encodeThread;
        m_encodeThread = nullptr;

        int result = EncodeFrame(nullptr);
        if (result >= 0) {
            av_write_trailer(m_pFormatCtx);
        }
    }
    while (!m_frameQueue.empty()) {
        AudioFrame *pAudioFrame = m_frameQueue.pop();
        delete pAudioFrame;
    }
    if (m_swrCtx) {
        swr_free(&m_swrCtx);
        m_swrCtx = nullptr;
    }
    if (m_pCodecCtx) {
        avcodec_close(m_pCodecCtx);
        avcodec_free_context(&m_pCodecCtx);
        m_pCodecCtx = nullptr;
    }
    if (m_PFrame) {
        av_frame_free(&m_PFrame);
        m_PFrame = nullptr;
    }
    if (m_pFrameBuffer) {
        av_free(m_pFrameBuffer);
        m_pFrameBuffer = nullptr;
    }
    if (m_pFormatCtx) {
        avio_close(m_pFormatCtx->pb);
        m_pFormatCtx = nullptr;
    }
    return 0;
}

void SingleAudioRecorder::StartACCEncoderThread(SingleAudioRecorder *recorder) {
    LOGI(LOG_TAG, "开启JNI线程");
    while (!recorder->m_exit || !recorder->m_frameQueue.empty()) {
        if (recorder->m_frameQueue.empty()) {
            usleep(10 * 1000);
            continue;
        }
        LOGI(LOG_TAG, "获取到数据");
        AudioFrame *audioFrame = recorder->m_frameQueue.pop();
        AVFrame *pFrame = recorder->m_PFrame;
        int result = swr_convert(recorder->m_swrCtx, pFrame->data, pFrame->nb_samples,
                                 (const uint8_t **) &(audioFrame->data), audioFrame->dataSize / 4);
        if (result >= 0) {
            LOGI(LOG_TAG, "swr转换成功 %d", result);
            pFrame->pts = recorder->m_frameIndex++;
            LOGI(LOG_TAG, "recorder->m_frameIndex %d", recorder->m_frameIndex);
            recorder->EncodeFrame(pFrame);
        } else {
            LOGI(LOG_TAG, "swr转换失败 %d", result);
        }
        delete audioFrame;
    }
    LOGI(LOG_TAG, "SingleAudioRecorder::StartAACEncoderThread end");
}

int SingleAudioRecorder::EncodeFrame(AVFrame *pFrame) {
//    LOGI(LOG_TAG, "SingleAudioRecorder::EncodeFrame pFrame->nb_samples=%d", pFrame != nullptr ? pFrame->nb_samples : 0);
    int result = avcodec_send_frame(m_pCodecCtx, pFrame);
    if (result < 0) {
        LOGI(LOG_TAG, "发送帧数据到编码器 fail. ret=%d", result);
        return result;
    } else {
        LOGI(LOG_TAG, "发送帧数据到编码器 success. ret=%d", result);
    }
    while (!result) {
        result = avcodec_receive_packet(m_pCodecCtx, &m_avPacket);
        if (result == AVERROR(EAGAIN)) {
            LOGE(LOG_TAG, "avcodec_receive_packet failed EAGAIN %d", result);
            return result;
        } else if (result == AVERROR_EOF) {
            LOGE(LOG_TAG, "avcodec_receive_packet failed AVERROR_EOF %d",result);
            return result;
        } else if (result < 0) {
            LOGE(LOG_TAG, "avcodec_receive_packet failed %d", result);
            return result;
        } else {
            LOGI(LOG_TAG, "avcodec_receive_packet success frame pts=%ld, size=%d", m_avPacket.pts, m_avPacket.size);
        }
        m_avPacket.stream_index = m_pStream->index;
        int writeFrameResult = av_interleaved_write_frame(m_pFormatCtx, &m_avPacket);
        av_packet_unref(&m_avPacket);
        if (writeFrameResult < 0) {
            LOGE(LOG_TAG, "写文件失败");
        } else {
            LOGI(LOG_TAG, "写文件成功：%d", writeFrameResult);
        }
    }
    LOGI(LOG_TAG, "Encode Frame return 0");
    return 0;
}