#include <jni.h>

//
// Created by HWJ on 2023/1/3.
//

#include <media_record_context.h>
#include <cstdio>
#include <cstring>
#include "jni.h"

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
    MediaRecorderContext *pContext = MediaRecorderContext::getContext(env,thiz);
    if(pContext){
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