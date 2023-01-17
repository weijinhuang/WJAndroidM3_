//
// Created by HWJ on 2023/1/6.
//

#include "media_record_context.h"

jfieldID MediaRecorderContext::s_ContextHandle = 0L;

MediaRecorderContext::MediaRecorderContext() {
}

MediaRecorderContext::~MediaRecorderContext() {

}

void MediaRecorderContext::createContext(JNIEnv *env, jobject instance) {
    MediaRecorderContext *pContext = new MediaRecorderContext();
    storeContext(env, instance, pContext);
}

void MediaRecorderContext::storeContext(JNIEnv *env, jobject instance, MediaRecorderContext *pContext) {
    jclass cls = env->GetObjectClass(instance);
    if (cls == NULL) {
        LOGE(LOG_TAG, "MediaRecorderContext::StoreContext cls == NULL");
        return;
    }
    s_ContextHandle = env->GetFieldID(cls, "mNativeContextHandle", "J");
    if (NULL == s_ContextHandle) {
        LOGE(LOG_TAG, "MediaRecorderContext::StoreContext s_ContextHandle == NULL");
        return;
    }
    env->SetLongField(instance, s_ContextHandle, reinterpret_cast<jlong>(pContext));
}

void MediaRecorderContext::deleteContext(JNIEnv *env, jobject instance) {
    if (s_ContextHandle == NULL) {
        LOGE(LOG_TAG, "MediaRecorderContext::deleteContext:Could not fin render context");
        return;
    }
    MediaRecorderContext *pContext = reinterpret_cast<MediaRecorderContext *>(env->GetLongField(instance, s_ContextHandle));
    if (pContext) {
        delete pContext;
    }
    env->SetLongField(instance, s_ContextHandle, 0L);

}

MediaRecorderContext *MediaRecorderContext::getContext(JNIEnv *env, jobject instance) {
    if (s_ContextHandle == NULL) {
        LOGE(LOG_TAG, "could not find render context");
        return nullptr;
    }
    MediaRecorderContext *pContext = reinterpret_cast<MediaRecorderContext *>(env->GetLongField(instance, s_ContextHandle));
    return pContext;
}

int MediaRecorderContext::UnInit() {
    return 0;
}

int MediaRecorderContext::Init() {
    return 0;
}

int MediaRecorderContext::startRecord(int recorderType, const char *outUrl, int frameWidth, int frameHeight, long videoBitRate, int fps) {
    LOGE(LOG_TAG, "MediaRecorderContext::start");
//    std::unique_lock<std::mutex> lock(m_mutex);
    return 0;
}

