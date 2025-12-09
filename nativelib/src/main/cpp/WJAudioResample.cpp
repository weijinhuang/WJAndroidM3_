//
// Created by HWJ on 2025/1/14.
//
#include "WJAudioReSample.h"
#include <string>
#include <iostream>
#include <fstream>
#include <ostream>

void freeResource(struct SwrContext **pSwrContext, void *inData, void *outData, FILE *inFile, FILE *outFile);

WJAudioReSample::WJAudioReSample() {

}

WJAudioReSample::~WJAudioReSample() {

}

int WJAudioReSample::startResample2(const char *inFilePath, const char *outFilePath) {
    LOGI(LOG_TAG, "startResample2 start");
    LOGI(LOG_TAG, "输入文件：%s", inFilePath);
    LOGI(LOG_TAG, "输出文件：%s", outFilePath);
    // 输入音频参数（需根据实际情况调整）
    const AVSampleFormat in_sample_fmt = AV_SAMPLE_FMT_S16;
    const int in_sample_rate = 44100;
    const uint64_t in_ch_layout = AV_CH_LAYOUT_STEREO;
    const int in_channels = av_get_channel_layout_nb_channels(in_ch_layout);
    const int in_bytes_per_sample = av_get_bytes_per_sample(in_sample_fmt);
    LOGI(LOG_TAG, "输入参数: out_ch_layout:%d, out_sample_fmt:%d, AV_SAMPLE_FMT_FLT %d", in_ch_layout, in_sample_fmt, in_sample_rate);

    // 输出音频参数（需根据实际情况调整）
    const AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_FLT;
    const int out_sample_rate = 48000;
    const uint64_t out_ch_layout = AV_CH_LAYOUT_MONO;
    const int out_channels = av_get_channel_layout_nb_channels(out_ch_layout);
    const int out_bytes_per_sample = av_get_bytes_per_sample(out_sample_fmt);

    LOGI(LOG_TAG, "输出参数: out_ch_layout:%d, out_sample_fmt:%d, AV_SAMPLE_FMT_FLT %d", AV_CH_LAYOUT_MONO, out_sample_fmt, out_sample_rate);
    // 打开文件
    FILE *in_file = std::fopen(inFilePath, "rb");
    FILE *out_file = std::fopen(outFilePath, "wb");
    if (!in_file || !out_file) {
        LOGI(LOG_TAG, "Error opening files");
        return -1;
    }

    int seekRet = fseek(in_file, 0, SEEK_END);
    if (seekRet != 0) {
        LOGE(LOG_TAG, "文件移动到尾部出错");
        std::fclose(in_file);
        std::fclose(out_file);
        return -1;
    }
    long fileSize = ftell(in_file);
    LOGI(LOG_TAG, "文件长度：%ld", fileSize);

    fseek(in_file, 0, SEEK_SET);
    if (feof(in_file)) {
        LOGE(LOG_TAG, "文件已经到尾部,重置文件");
    }
    if (feof(in_file)) {
        LOGE(LOG_TAG, "文件已经到尾部");
        std::fclose(in_file);
        std::fclose(out_file);
        return -1;
    }

    // 初始化重采样器
    LOGI(LOG_TAG, "初始化重采样器");
    SwrContext *swr = swr_alloc();
    av_opt_set_int(swr, "in_channel_layout", in_ch_layout, 0);
    av_opt_set_int(swr, "in_sample_rate", in_sample_rate, 0);
    av_opt_set_sample_fmt(swr, "in_sample_fmt", in_sample_fmt, 0);
    av_opt_set_int(swr, "out_channel_layout", out_ch_layout, 0);
    av_opt_set_int(swr, "out_sample_rate", out_sample_rate, 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt", out_sample_fmt, 0);

    if (swr_init(swr) < 0) {
        LOGI(LOG_TAG, "Failed to initialize resampler");
        std::fclose(in_file);
        std::fclose(out_file);
        return -1;
    }

    LOGI(LOG_TAG, "分配输入帧");
    // 分配输入帧
    const int in_frame_samples = 1024; // 每次读取的采样数
    AVFrame *in_frame = av_frame_alloc();
    in_frame->format = in_sample_fmt;
    in_frame->channel_layout = in_ch_layout;
    in_frame->sample_rate = in_sample_rate;
    in_frame->nb_samples = in_frame_samples;
    if (av_frame_get_buffer(in_frame, 0) < 0) {
        LOGI(LOG_TAG, "Failed to allocate input frame");
        swr_free(&swr);
        std::fclose(in_file);
        std::fclose(out_file);
        return -1;
    }

    // 输出帧（动态调整大小）
    LOGI(LOG_TAG, "输出帧（动态调整大小）");
    AVFrame *out_frame = av_frame_alloc();
    out_frame->format = out_sample_fmt;
    out_frame->channel_layout = out_ch_layout;
    out_frame->sample_rate = out_sample_rate;
    out_frame->nb_samples = 0;

    int ret = 0;
    const size_t in_buffer_size = in_frame_samples * in_channels * in_bytes_per_sample;

    LOGI(LOG_TAG, "in_buffer_size: %d", in_buffer_size);
    while (true) {
        // 读取PCM数据
        size_t read_size = std::fread(in_frame->data[0], 1, in_buffer_size, in_file);
        LOGI(LOG_TAG, "读取PCM数据 read_size: %lu", read_size);
        if (read_size == 0) break;

        // 计算实际读取的样本数
        const int samples_read = static_cast<int>(read_size / (in_channels * in_bytes_per_sample));

        LOGI(LOG_TAG, "计算实际读取的样本数 samples_read: %d", samples_read);
        // 计算需要的输出样本数
        const int64_t delay = swr_get_delay(swr, in_sample_rate);
        int req_samples = av_rescale_rnd(delay + samples_read,
                                         out_sample_rate, in_sample_rate,
                                         AV_ROUND_UP);

//        LOGI(LOG_TAG, "计算需要的输出样本数req_samples: %d", req_samples);
//        // 调整输出帧大小
//        if (req_samples > out_frame->nb_samples) {
//            LOGI(LOG_TAG, "调整输出帧大小 req_samples:%d out_frame->nb_samples:%d", req_samples, out_frame->nb_samples);
//            av_frame_unref(out_frame);
//            out_frame->nb_samples = req_samples;
//            if (av_frame_get_buffer(out_frame, 0) < 0) {
//                LOGI(LOG_TAG, "Failed to resize output frame");
//                ret = -1;
//                break;
//            }
//        }

        // 执行重采样
        const int converted = swr_convert(swr,
                                          out_frame->data,
                                          req_samples,
                                          const_cast<const uint8_t **>(in_frame->data),
                                          samples_read);

        LOGI(LOG_TAG, "执行重采样 %d", converted);
        if (converted < 0) {
            LOGI(LOG_TAG, "Resampling failed");
            ret = -1;
            break;
        }

        // 写入输出文件
        const size_t out_size = converted * out_channels * out_bytes_per_sample;

        LOGI(LOG_TAG, "写入输出文件 out_size:%d", out_size);
        if (std::fwrite(out_frame->data[0], 1, out_size, out_file) != out_size) {
            LOGI(LOG_TAG, "Write failed");
            ret = -1;
            break;
        }
    }

    // 刷新重采样缓冲区
    LOGI(LOG_TAG, "刷新重采样缓冲区");
    if (ret == 0) {
        while (true) {
            const int64_t delay = swr_get_delay(swr, in_sample_rate);
            const int req_samples = av_rescale_rnd(delay, out_sample_rate, in_sample_rate, AV_ROUND_UP);

            if (req_samples <= 0) break;

            if (req_samples > out_frame->nb_samples) {
                av_frame_unref(out_frame);
                out_frame->nb_samples = req_samples;
                if (av_frame_get_buffer(out_frame, 0) < 0) {
                    LOGI(LOG_TAG, "Failed to resize output frame");
                    break;
                }
            }

            const int converted = swr_convert(swr, out_frame->data, req_samples, nullptr, 0);
            if (converted <= 0) break;

            const size_t out_size = converted * out_channels * out_bytes_per_sample;
            if (std::fwrite(out_frame->data[0], 1, out_size, out_file) != out_size) {
                LOGI(LOG_TAG, "Write failed");
                break;
            }
        }
    }

    // 清理资源
    av_frame_free(&in_frame);
    av_frame_free(&out_frame);
    swr_free(&swr);
    std::fclose(in_file);
    std::fclose(out_file);
    LOGI(LOG_TAG, "重采样完毕");
    return ret;
}

int WJAudioReSample::simpleCopy(const char *inFilePath, int src_sample_rate, int src_channel_count, int src_sample_format, const char *outFilePath,
                                int dst_sample_rate, int dst_channel_count, int dst_sample_format) {
    LOGI(LOG_TAG, "重采样开始，源文件%s", inFilePath);
    LOGI(LOG_TAG, "输出文件%s", outFilePath);

    int64_t in_ch_layout = (src_channel_count == 1) ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
    AVSampleFormat in_sample_fmt = src_sample_format == 4 ? AV_SAMPLE_FMT_FLT : AV_SAMPLE_FMT_S16;
    int in_sample_rate = src_sample_rate;

    LOGI(LOG_TAG, "输入参数: in_sample_rate:%d, in_ch_layout:%d, in_sample_fmt %d", in_sample_rate, in_ch_layout, in_sample_fmt);

    int64_t out_ch_layout = dst_channel_count == 1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
    AVSampleFormat out_sample_fmt = dst_sample_format == 4 ? AV_SAMPLE_FMT_FLT : AV_SAMPLE_FMT_S16;
    int out_sample_rate = dst_sample_rate;
    LOGI(LOG_TAG, "输入参数: out_sample_rate:%d, out_ch_layout:%d, out_sample_rate %d", out_sample_rate, out_ch_layout, out_sample_rate);

    //创建重采样上下文
    SwrContext *ctx = nullptr;
    ctx = swr_alloc_set_opts(nullptr,
                             out_ch_layout, out_sample_fmt, out_sample_rate,
                             in_ch_layout, in_sample_fmt, in_sample_rate,
                             0,
                             NULL);
    int ret = 0;
    if (!ctx) {
        LOGE(LOG_TAG, "初始化重采样上下文失败");
        return -1;
    }

    //初始化重采样上下文
    ret = swr_init(ctx);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "初始化重采样上下文失败：%s", errbuf);
        swr_free(&ctx);
        return ret;
    }


    //创建输入缓冲
    uint8_t **inData = nullptr;
    int inLineSize = 0;//缓冲区大小
    int inChs = av_get_channel_layout_nb_channels(in_ch_layout);
    int inSamples = 1024;
    int inBytesPerSample = inChs * av_get_bytes_per_sample(in_sample_fmt);
    LOGI(LOG_TAG, "输入声道：%d", inChs);
    LOGI(LOG_TAG, "输入样本每样本大小：%d", inBytesPerSample);
    ret = av_samples_alloc_array_and_samples(&inData, &inLineSize, inChs, inSamples, in_sample_fmt, 1);
    LOGI(LOG_TAG, "创建输入缓冲区 inLineSize:%d,", inLineSize);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "创建输入缓冲区失败：%s", errbuf);
        swr_free(&ctx);
        av_freep(&inData);
        return ret;
    }

    //创建输出缓冲
    uint8_t **outData = nullptr;
    int outLineSize = 0;//缓冲区大小
    int outChs = av_get_channel_layout_nb_channels(out_ch_layout);
    LOGI(LOG_TAG, "输出声道：%d", outChs);
    int outSamples = 1024;
    int outBytesPerSample = outChs * av_get_bytes_per_sample(out_sample_fmt);
    LOGI(LOG_TAG, "输出样本每样本大小：%d", outBytesPerSample);
    ret = av_samples_alloc_array_and_samples(&outData, &outLineSize, outChs, outSamples, out_sample_fmt, 1);
    LOGI(LOG_TAG, "创建输出缓冲区 outLineSize:%d,", outLineSize);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "创建输出缓冲区失败：%s", errbuf);
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return ret;
    }

    std::ifstream inputFile(inFilePath, std::ios::binary);
    if (!inputFile.is_open()) {
        LOGE(LOG_TAG, "无法打开文件:%s", inFilePath);
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return -1;
    }
    std::ofstream outputFile(outFilePath, std::ios::binary);
    if (!outputFile.is_open()) {
        LOGE(LOG_TAG, "无法打开文件:%s", outFilePath);
        inputFile.close();
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return -1;
    }
//    char buffer[inLineSize];
    while (inputFile) {
        inputFile.read((char *) *inData, inLineSize);
        size_t bytesRead = inputFile.gcount();
        if (bytesRead == 0) {
            inputFile.close();
            swr_free(&ctx);
            av_freep(&inData);
            av_freep(&outData);
            break;
        }
        LOGI(LOG_TAG, "读取样本size：%d", bytesRead);
//        inSamples = bytesRead / inBytesPerSample;
//        ret = swr_convert(ctx,
//                          outData, outSamples,
//                          (const uint8_t **) inData, inSamples);
//        if (ret < 0) {
//            break;
//        } else {
        outputFile.write((const char *) inData[0], bytesRead);
//        }

//        if (bytesRead < inLineSize) {
//            LOGE(LOG_TAG, "编码结束");
//            break;
//        }
    }
    LOGI(LOG_TAG, "重采样完毕 %s", outFilePath);
    swr_free(&ctx);
    av_freep(&inData);
    av_freep(&outData);
    inputFile.close();
    outputFile.close();
    LOGI(LOG_TAG, "释放资源");
    return 1;
}

/**
 * PCM重采样
 *
(提示:.7z文件需使用7z解压软件解压,使用其它解压软件会提示压缩包损坏或失败)
 *
 * @param inFilePath
 * @param src_sample_rate
 * @param src_channel_count
 * @param src_sample_format
 * @param outFilePath
 * @param dst_sample_rate
 * @param dst_channel_count
 * @param dst_sample_format
 * @return
 */
int WJAudioReSample::startResample(const char *inFilePath, int src_sample_rate, int src_channel_count, int src_sample_format,
                                   const char *outFilePath, int dst_sample_rate, int dst_channel_count, int dst_sample_format) {

    LOGI(LOG_TAG, "重采样开始，源文件%s", inFilePath);
    LOGI(LOG_TAG, "输出文件%s", outFilePath);

    int64_t in_ch_layout = (src_channel_count == 1) ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
    const char *in_ch_layoutDes = (src_channel_count == 1) ? "AV_CH_LAYOUT_MONO" : "AV_CH_LAYOUT_STEREO";
    AVSampleFormat in_sample_fmt = src_sample_format == 4 ? AV_SAMPLE_FMT_FLT : AV_SAMPLE_FMT_S16;
    const char *in_sample_fmtDes = src_sample_format == 4 ? "AV_SAMPLE_FMT_FLT" : "AV_SAMPLE_FMT_S16";
    int in_sample_rate = src_sample_rate;

    LOGI(LOG_TAG, "输入参数: in_sample_rate:%d, in_ch_layout:%s, in_sample_fmt %s", in_sample_rate, in_ch_layoutDes, in_sample_fmtDes);

    int64_t out_ch_layout = dst_channel_count == 1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
    const char *out_ch_layoutDes = dst_channel_count == 1 ? "AV_CH_LAYOUT_MONO" : "AV_CH_LAYOUT_STEREO";
    AVSampleFormat out_sample_fmt = dst_sample_format == 4 ? AV_SAMPLE_FMT_FLT : AV_SAMPLE_FMT_S16;
    const char *out_sample_fmtDes = dst_sample_format == 4 ? "AV_SAMPLE_FMT_FLT" : "AV_SAMPLE_FMT_S16";
    int out_sample_rate = dst_sample_rate;
    LOGI(LOG_TAG, "输入参数: out_sample_rate:%d, out_ch_layout:%s, out_sample_rate %s", out_sample_rate, out_ch_layoutDes, out_sample_fmtDes);

    //创建重采样上下文
    SwrContext *ctx = nullptr;
    ctx = swr_alloc_set_opts(nullptr,
                             out_ch_layout, out_sample_fmt, out_sample_rate,
                             in_ch_layout, in_sample_fmt, in_sample_rate,
                             0,
                             NULL);
    int ret = 0;
    if (!ctx) {
        LOGE(LOG_TAG, "初始化重采样上下文失败");
        return -1;
    }

    //初始化重采样上下文
    ret = swr_init(ctx);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "初始化重采样上下文失败：%s", errbuf);
        swr_free(&ctx);
        return ret;
    }


    //创建输入缓冲
    uint8_t **inData = nullptr;
    int inLineSize = 0;//缓冲区大小
    int inChs = av_get_channel_layout_nb_channels(in_ch_layout);//通过声道布局获取声道数
    int inSamples = 1024;
    int inBytesPerSample = inChs * av_get_bytes_per_sample(in_sample_fmt);//获取每个样本的大小
    LOGI(LOG_TAG, "输入声道数：%d，每样本大小：%d", inChs, inBytesPerSample);
    //分配输入缓冲区
    ret = av_samples_alloc_array_and_samples(&inData, &inLineSize, inChs, inSamples, in_sample_fmt, 1);
    LOGI(LOG_TAG, "创建输入缓冲区 inLineSize:%d,", inLineSize);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "创建输入缓冲区失败：%s", errbuf);
        swr_free(&ctx);
        av_freep(&inData);
        return ret;
    }

    //创建输出缓冲
    uint8_t **outData = nullptr;
    int outLineSize = 0;//缓冲区大小
    int outChs = av_get_channel_layout_nb_channels(out_ch_layout);//通过声道布局获取声道数
    int outSamples = 1024;
    int outBytesPerSample = outChs * av_get_bytes_per_sample(out_sample_fmt);//输出的每个样本大小
    LOGI(LOG_TAG, "输出声道：%d， 输出样本每样本大小：%d", outChs, outBytesPerSample);
    ret = av_samples_alloc_array_and_samples(&outData, &outLineSize, outChs, outSamples, out_sample_fmt, 1);
    LOGI(LOG_TAG, "创建输出缓冲区 outLineSize:%d,", outLineSize);
    if (ret < 0) {
        ERROR_BUF(ret);
        LOGE(LOG_TAG, "创建输出缓冲区失败：%s", errbuf);
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return ret;
    }

    std::ifstream inputFile(inFilePath, std::ios::binary);
    if (!inputFile.is_open()) {
        LOGE(LOG_TAG, "无法打开文件:%s", inFilePath);
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return -1;
    }
    std::ofstream outputFile(outFilePath, std::ios::binary);
    if (!outputFile.is_open()) {
        LOGE(LOG_TAG, "无法打开文件:%s", outFilePath);
        inputFile.close();
        swr_free(&ctx);
        av_freep(&inData);
        av_freep(&outData);
        return -1;
    }
//    char buffer[inLineSize];
    while (inputFile) {
        inputFile.read((char *) *inData, inLineSize);
        size_t bytesRead = inputFile.gcount();
        if (bytesRead == 0) {
            inputFile.close();
            swr_free(&ctx);
            av_freep(&inData);
            av_freep(&outData);
            break;
        }
        LOGI(LOG_TAG, "读取样本size：%d", bytesRead);
        inSamples = bytesRead / inBytesPerSample;
        ret = swr_convert(ctx,
                          outData, outSamples,
                          (const uint8_t **) inData, inSamples);
        if (ret < 0) {
            break;
        } else {
            LOGE(LOG_TAG, "编码成功：%d", ret);
            outputFile.write((const char *) outData[0], ret * outBytesPerSample);
        }

        if (bytesRead < inLineSize) {
            LOGE(LOG_TAG, "编码结束");
            break;
        }
    }
    LOGI(LOG_TAG, "重采样完毕 %s", outFilePath);
    swr_free(&ctx);
    av_freep(&inData);
    av_freep(&outData);
    inputFile.close();
    outputFile.close();
    LOGI(LOG_TAG, "释放资源");
    return 1;
}

void freeResource(struct SwrContext **pSwrContext, void *inData, void *outData, FILE *inFile, FILE *outFile) {
    swr_free(pSwrContext);
    av_freep(inData);
    av_freep(outData);
    fclose(inFile);
    fclose(outFile);
}

int WJAudioReSample::writeFile(const char *dstFile) {
    std::ofstream outputFile(dstFile, std::ios::binary);
    if (!outputFile.is_open()) {
        LOGE(LOG_TAG, "无法打开输出文件");
        return -1;
    }


}

int WJAudioReSample::readFile(const char *dstFile) {

}

