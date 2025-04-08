#include <jni.h>

//
// Created by HWJ on 2023/1/3.
//

#include <media_record_context.h>
#include <cstdio>
#include <cstring>
#include "jni.h"
#include "JNITest.h"

#include <chrono>

extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_CreateContext(JNIEnv *env, jobject thiz) {
    MediaRecorderContext::createContext(env, thiz);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_DestroyContext(JNIEnv *env, jobject thiz) {
    MediaRecorderContext::deleteContext(env, thiz);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_MediaRecorderContext_Init(JNIEnv *env, jobject thiz) {
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env, thiz);
    if (pContext) return pContext->Init();
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_MediaRecorderContext_StartRecord(JNIEnv *env, jobject thiz, jint recorder_type, jstring out_url, jint frame_width,
                                                       jint frame_height, jlong video_bit_rate, jint fps) {
    const char *url = env->GetStringUTFChars(out_url, nullptr);
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env, thiz);
    env->ReleaseStringUTFChars(out_url, url);
    if (pContext) {
        return pContext->startRecord(recorder_type, url, frame_width, frame_height, video_bit_rate, fps);
    }
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_OnAudioData(JNIEnv *env, jobject thiz, jbyteArray data, jint len) {
    int arrayLen = env->GetArrayLength(data);
    unsigned char *buf = new unsigned char[len];
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(buf));
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env, thiz);
    if (pContext) {
        pContext->onAudioData(buf, arrayLen);
    }
    delete[] buf;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_OnPreviewFrame(JNIEnv *env, jobject thiz, jint format, jbyteArray data, jint width, jint height) {

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_MediaRecorderContext_StopRecord(JNIEnv *env, jobject thiz) {
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env, thiz);
    if (pContext) {
        return pContext->stopRecord();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_SetTransformMatrix(JNIEnv *env, jobject thiz, jfloat translate_x, jfloat trans_late_y,
                                                              jfloat scale_x, jfloat scale_y, jint degree, jint mirror) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_OnSurfaceCreated(JNIEnv *env, jobject thiz) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_OnSurfaceChanged(JNIEnv *env, jobject thiz, jint width, jint height) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_OnDrawFrame(JNIEnv *env, jobject thiz) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_SetFilterData(JNIEnv *env, jobject thiz, jint index, jint format, jint width, jint height,
                                                         jbyteArray bytes) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_MediaRecorderContext_SetFragShader(JNIEnv *env, jobject thiz, jint index, jstring str) {

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_MediaRecorderContext_UnInit(JNIEnv *env, jobject thiz) {
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env, thiz);
    if (pContext) {
        return pContext->UnInit();
    }
    return 0;
}


JNITest *mJniTest;
extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_jinTest(JNIEnv *env, jobject thiz) {
    if (!mJniTest) {
        LOGE("TEST", "创建JNITest");
        JNITest *jniTest = new JNITest();
        mJniTest = jniTest;
        auto now = std::chrono::system_clock::now(); // 获取当前时间点
        auto duration = now.time_since_epoch();
        long long timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count(); // 转换为毫秒级的时间戳
        mJniTest->id = timestamp;
    }

    LOGE("TEST", "JINTest i = %lld", mJniTest->id);
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_WJAudioResample(JNIEnv *env, jobject thiz,
                                                       jstring src_pcmfile_path, jint src_sample_rate, jint src_channel_count, jint src_sample_format,
                                                       jstring dst_pcmfile, jint dst_sample_rate, jint dst_channel_count, jint dst_sample_format) {
    jclass threadClass = env->FindClass("java/lang/Thread");
    jmethodID currentThreadMethod = env->GetStaticMethodID(
            threadClass,
            "currentThread",
            "()Ljava/lang/Thread;"
    );
    jobject currentThread = env->CallStaticObjectMethod(threadClass, currentThreadMethod);
    jmethodID getNameMethod = env->GetMethodID(
            threadClass,
            "getName",
            "()Ljava/lang/String;"
    );
    jstring jThreadName = (jstring)env->CallObjectMethod(currentThread, getNameMethod);
    const char* cThreadName = env->GetStringUTFChars(jThreadName, nullptr);
    LOGI(LOG_TAG,"当前线程：%s", cThreadName );
    env->ReleaseStringUTFChars(jThreadName, cThreadName);
    env->DeleteLocalRef(jThreadName);
    env->DeleteLocalRef(currentThread);

    const char *inFilePath = env->GetStringUTFChars(src_pcmfile_path, 0);
    const char *outFilePath = env->GetStringUTFChars(dst_pcmfile, 0);

    WJAudioReSample wjAudioReSample = WJAudioReSample();
    wjAudioReSample.startResample(inFilePath, src_sample_rate, src_channel_count, src_sample_format,
                                  outFilePath, dst_sample_rate, dst_channel_count, dst_sample_format);
}