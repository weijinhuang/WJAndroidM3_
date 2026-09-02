#include <jni.h>
#include <cstdio>
#include <cstdint>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/frame.h>
#include <libswscale/swscale.h>
}

namespace {

struct OpenVideo {
    AVFormatContext *format = nullptr;
    AVCodecContext *decoder = nullptr;
    AVStream *stream = nullptr;
    int streamIndex = -1;

    ~OpenVideo() {
        if (decoder) avcodec_free_context(&decoder);
        if (format) avformat_close_input(&format);
    }
};

int openVideo(const char *path, OpenVideo &video) {
    // 该项目携带的是仍保留 av_register_all 的 FFmpeg 版本；重复调用是安全的。
    av_register_all();
    int result = avformat_open_input(&video.format, path, nullptr, nullptr);
    if (result < 0) return -10;
    result = avformat_find_stream_info(video.format, nullptr);
    if (result < 0) return -11;

    for (unsigned int i = 0; i < video.format->nb_streams; ++i) {
        if (video.format->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video.streamIndex = static_cast<int>(i);
            video.stream = video.format->streams[i];
            break;
        }
    }
    if (video.streamIndex < 0) return -12;

    AVCodec *codec = avcodec_find_decoder(video.stream->codecpar->codec_id);
    if (!codec) return -13;
    video.decoder = avcodec_alloc_context3(codec);
    if (!video.decoder) return -14;
    if (avcodec_parameters_to_context(video.decoder, video.stream->codecpar) < 0) return -15;
    // 截图只需要一个确定性解码结果；FFmpeg 内部仍可按 codec 能力选择优化。
    video.decoder->thread_count = 0;
    if (avcodec_open2(video.decoder, codec, nullptr) < 0) return -16;
    return 0;
}

int copyFrame(AVFrame *destination, AVFrame *source) {
    av_frame_unref(destination);
    return av_frame_ref(destination, source);
}

int decodeFrameAt(OpenVideo &video, int64_t timestampUs, AVFrame *selected) {
    int64_t target = av_rescale_q(
            timestampUs < 0 ? 0 : timestampUs,
            AV_TIME_BASE_Q,
            video.stream->time_base);
    // ExoPlayer 的 position 从 0 开始；部分 TS/MOV 流的 PTS 从非零 start_time 开始。
    if (video.stream->start_time != AV_NOPTS_VALUE) target += video.stream->start_time;
    // 从目标之前的关键帧开始并逐帧向前解码，不能使用“最近关键帧”冒充当前帧。
    if (av_seek_frame(video.format, video.streamIndex, target, AVSEEK_FLAG_BACKWARD) < 0) return -20;
    avcodec_flush_buffers(video.decoder);

    AVPacket packet;
    av_init_packet(&packet);
    packet.data = nullptr;
    packet.size = 0;
    AVFrame *decoded = av_frame_alloc();
    if (!decoded) return -21;

    bool haveFrame = false;
    bool done = false;
    while (!done && av_read_frame(video.format, &packet) >= 0) {
        if (packet.stream_index == video.streamIndex) {
            int sendResult = avcodec_send_packet(video.decoder, &packet);
            if (sendResult >= 0 || sendResult == AVERROR(EAGAIN)) {
                while (true) {
                    int receiveResult = avcodec_receive_frame(video.decoder, decoded);
                    if (receiveResult == AVERROR(EAGAIN) || receiveResult == AVERROR_EOF) break;
                    if (receiveResult < 0) {
                        av_packet_unref(&packet);
                        av_frame_free(&decoded);
                        return -22;
                    }
                    int64_t pts = av_frame_get_best_effort_timestamp(decoded);
                    if (pts == AV_NOPTS_VALUE) pts = decoded->pts;
                    if (pts == AV_NOPTS_VALUE) pts = target;

                    if (pts <= target || !haveFrame) {
                        // 若 seek 后第一个可解码帧已晚于目标（极短/异常时间戳视频），仍返回首帧。
                        if (copyFrame(selected, decoded) < 0) {
                            av_packet_unref(&packet);
                            av_frame_free(&decoded);
                            return -23;
                        }
                        haveFrame = true;
                    }
                    if (pts > target && haveFrame) {
                        done = true;
                        break;
                    }
                    av_frame_unref(decoded);
                }
            }
        }
        av_packet_unref(&packet);
    }
    av_packet_unref(&packet);
    av_frame_free(&decoded);
    return haveFrame ? 0 : -24;
}

int encodePng(AVFrame *source, const char *outputPath) {
    AVCodec *encoder = avcodec_find_encoder(AV_CODEC_ID_PNG);
    if (!encoder) return -30;
    AVCodecContext *context = avcodec_alloc_context3(encoder);
    if (!context) return -31;
    context->width = source->width;
    context->height = source->height;
    context->pix_fmt = AV_PIX_FMT_RGB24;
    context->time_base = AVRational{1, 25};
    if (avcodec_open2(context, encoder, nullptr) < 0) {
        avcodec_free_context(&context);
        return -32;
    }

    AVFrame *rgb = av_frame_alloc();
    if (!rgb) {
        avcodec_free_context(&context);
        return -33;
    }
    rgb->format = context->pix_fmt;
    rgb->width = context->width;
    rgb->height = context->height;
    if (av_frame_get_buffer(rgb, 32) < 0) {
        av_frame_free(&rgb);
        avcodec_free_context(&context);
        return -34;
    }

    SwsContext *scale = sws_getContext(
            source->width, source->height, static_cast<AVPixelFormat>(source->format),
            rgb->width, rgb->height, AV_PIX_FMT_RGB24,
            SWS_BICUBIC, nullptr, nullptr, nullptr);
    if (!scale) {
        av_frame_free(&rgb);
        avcodec_free_context(&context);
        return -35;
    }
    sws_scale(scale, source->data, source->linesize, 0, source->height, rgb->data, rgb->linesize);
    rgb->pts = 0;

    int result = avcodec_send_frame(context, rgb);
    AVPacket output;
    av_init_packet(&output);
    output.data = nullptr;
    output.size = 0;
    if (result >= 0) result = avcodec_receive_packet(context, &output);
    if (result >= 0) {
        FILE *file = std::fopen(outputPath, "wb");
        if (!file) {
            result = -36;
        } else {
            const size_t written = std::fwrite(output.data, 1, static_cast<size_t>(output.size), file);
            std::fclose(file);
            if (written != static_cast<size_t>(output.size)) result = -37;
        }
    } else {
        result = -38;
    }

    av_packet_unref(&output);
    sws_freeContext(scale);
    av_frame_free(&rgb);
    avcodec_free_context(&context);
    return result < 0 ? result : 0;
}

} // namespace

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_wj_nativelib_FFmpegVideoFrameExtractor_probeVideo(
        JNIEnv *env, jobject /* thiz */, jstring inputPath) {
    const char *path = env->GetStringUTFChars(inputPath, nullptr);
    OpenVideo video;
    const int result = openVideo(path, video);
    env->ReleaseStringUTFChars(inputPath, path);
    if (result < 0) return env->NewLongArray(0);

    AVRational rate = video.stream->avg_frame_rate;
    if (rate.num <= 0 || rate.den <= 0) rate = video.stream->r_frame_rate;
    int64_t durationMs = 0;
    if (video.stream->duration != AV_NOPTS_VALUE) {
        durationMs = av_rescale_q(video.stream->duration, video.stream->time_base, AVRational{1, 1000});
    } else if (video.format->duration != AV_NOPTS_VALUE) {
        durationMs = video.format->duration / (AV_TIME_BASE / 1000);
    }
    jlong values[5] = {
            video.decoder->width,
            video.decoder->height,
            durationMs,
            rate.num,
            rate.den
    };
    jlongArray output = env->NewLongArray(5);
    env->SetLongArrayRegion(output, 0, 5, values);
    return output;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_wj_nativelib_FFmpegVideoFrameExtractor_extractFrameToPng(
        JNIEnv *env, jobject /* thiz */, jstring inputPath, jstring outputPath, jlong timestampUs) {
    const char *input = env->GetStringUTFChars(inputPath, nullptr);
    const char *output = env->GetStringUTFChars(outputPath, nullptr);
    OpenVideo video;
    int result = openVideo(input, video);
    if (result == 0) {
        AVFrame *selected = av_frame_alloc();
        if (!selected) {
            result = -40;
        } else {
            result = decodeFrameAt(video, static_cast<int64_t>(timestampUs), selected);
            if (result == 0) result = encodePng(selected, output);
            av_frame_free(&selected);
        }
    }
    env->ReleaseStringUTFChars(inputPath, input);
    env->ReleaseStringUTFChars(outputPath, output);
    return result;
}
