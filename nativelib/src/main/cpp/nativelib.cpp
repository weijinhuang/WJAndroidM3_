#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_wj_nativelib_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavcodec/jni.h>

JNIEXPORT jstring JNICALL
Java_com_wj_nativelib_NativeLib_ffmpegVersion(JNIEnv *env, jobject thiz) {
    char info[40000] = {0};
    const AVCodec *c_temp = nullptr;
    void *i = 0;

    while ((c_temp = av_codec_iterate(&i))) {
        if (c_temp->decode != NULL) {
            sprintf(info, "%sdecode:", info);
            switch (c_temp->type) {
                case AVMEDIA_TYPE_VIDEO:
                    sprintf(info, "%s(video):", info);
                    break;
                case AVMEDIA_TYPE_AUDIO:
                    sprintf(info, "%s(audio):", info);
                    break;
                default:
                    sprintf(info, "%s(other):", info);
                    break;
            }
            sprintf(info, "%s[%10s]\n", info, c_temp->name);
        } else {
            sprintf(info, "%sencode:", info);
        }
    }
    return env->NewStringUTF(info);
}
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_NativeLib_startRecord(JNIEnv *env, jobject thiz) {

    const char *fmtName = "dshow";
    AVInputFormat *fmt = av_find_input_format(fmtName);
    if (!fmt) {
        printf("获取输入格式对象失败");
    }
    AVFormatContext *ctx;
    const char *deviceName = "dshow";
    int ret = avformat_open_input(&ctx, deviceName, fmt, nullptr);
    if (ret < 0) {
//        av_strerror()
    }

}