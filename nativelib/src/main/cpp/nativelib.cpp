#include <jni.h>
#include <string>
#include "WJACCEncoder.h"

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

WJACCEncoder *pAACEncoder = nullptr;
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJNativeAudioEncoder_encodeAudioStart(JNIEnv *env, jobject thiz, jstring aac_path) {
    if (nullptr == pAACEncoder) {
        LOGI(LOG_TAG, "创建native aac encoder -> pAACEncoder = new WJACCEncoder();");
        pAACEncoder = new WJACCEncoder();
        const char *aacPath = env->GetStringUTFChars(aac_path, NULL);
        int ret = pAACEncoder->EncodeStart(aacPath);
        if (ret < 0) {
            LOGE(LOG_TAG, "JNI编码初始化失败");
        } else {
            LOGI(LOG_TAG, "JNI编码初始化完成");
        }
    } else {
        LOGI(LOG_TAG, "pAACEncoder != nullptr");
    }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJNativeAudioEncoder_encodeAudioStop(JNIEnv *env, jobject thiz) {
    if (nullptr != pAACEncoder) {
        pAACEncoder->EncodeStop();
        delete pAACEncoder;
        pAACEncoder = nullptr;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJNativeAudioEncoder_onAudioFrame(JNIEnv *env, jobject thiz, jbyteArray pcm_data, jint len) {
    if (nullptr != pAACEncoder) {
        jbyte *buffer = env->GetByteArrayElements(pcm_data, nullptr);
        pAACEncoder->EncodeBuffer((unsigned char *) buffer, len);
        env->ReleaseByteArrayElements(pcm_data, buffer, NULL);
    }
}