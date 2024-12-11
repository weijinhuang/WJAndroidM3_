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
    LOGI(LOG_TAG,"create Native MediaRecorderContext()");
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
    LOGI(LOG_TAG, "存储MediaRecorderContext");
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
    LOGI(LOG_TAG, "获取MediaRecorderContext%d", pContext);
    return pContext;
}

int MediaRecorderContext::UnInit() {
    return 0;
}

int MediaRecorderContext::Init() {
    return 0;
}

int MediaRecorderContext::startRecord(int recorderType, const char *outUrl, int frameWidth, int frameHeight, long videoBitRate, int fps) {
    LOGI(LOG_TAG, "MediaRecorderContext::start recorderType=%d, outUrl=%s, [w,h]=[%d,%d], videoBitRate=%ld, fps=%d", recorderType, outUrl, frameWidth, frameHeight, videoBitRate, fps);
    std::unique_lock<std::mutex> lock(m_mutex);
    switch (recorderType) {
        case RECORDER_TYPE_SINGLE_AUDIO:
            if (m_pAudioRecorder == nullptr) {
                LOGI(LOG_TAG, "JNI创建录制线程");
                m_pAudioRecorder = new SingleAudioRecorder(outUrl, DEFAULT_SAMPLE_RATE, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16);
                m_pAudioRecorder->StartRecord();
            }
            break;
        default:
            break;
    }
    return 0;
}

void MediaRecorderContext::onAudioData(uint8_t *pData, int size) {
    LOGI(LOG_TAG, "MediaRecorderContext::OnAudioData pData=%p, dataSize=%d", pData, size);
    AudioFrame audioFrame(pData, size, false);
    if (m_pAudioRecorder != nullptr) {
        int ret = m_pAudioRecorder->OnFrame2Encode(&audioFrame);
        if (ret == 0) {
            LOGI(LOG_TAG, "Frame Encode success");
        }
    }
}

int MediaRecorderContext::stopRecord() {
    std::unique_lock<std::mutex> lock(m_mutex);
    if (m_pAudioRecorder != nullptr) {
        m_pAudioRecorder->StopRecord();
        delete m_pAudioRecorder;
        m_pAudioRecorder = nullptr;
    }
    return 0;
}

