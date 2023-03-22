//
// Created by HWJ on 2023/1/6.
//

#ifndef WJANDROIDM3_MEDIA_RECORD_CONTEXT_H
#define WJANDROIDM3_MEDIA_RECORD_CONTEXT_H

extern "C" {
#include <cstdint>
#include "jni.h"
#include "libavutil/avutil.h"
#include "wj_log.h"

}

#include <mutex>
#include "single_audio_recorder.h"

#define RECORDER_TYPE_SINGLE_VIDEO 0    //仅录制视频
#define RECORDER_TYPE_SINGLE_AUDIO 1    //仅录制音频
#define RECORDER_TYPE_AV                //同时录制音视频

class MediaRecorderContext {


public :
    MediaRecorderContext();

    ~MediaRecorderContext();

    static void createContext(JNIEnv *env, jobject instance);

    static void storeContext(JNIEnv *env, jobject instance, MediaRecorderContext *pContext);

    static void deleteContext(JNIEnv *env, jobject instance);

    static MediaRecorderContext *getContext(JNIEnv *env, jobject instance);

    void onAudioData(uint8_t *pData, int size);

    int UnInit();

    int Init();

    int startRecord(int recorderType, const char *outUrl, int frameWidth, int frameHeight,
                    long videoBitRate, int fps);

    int stopRecord();
private:
    static jfieldID s_ContextHandle;
    std::mutex m_mutex;
    SingleAudioRecorder *m_pAudioRecorder = nullptr;

};


#endif //WJANDROIDM3_MEDIA_RECORD_CONTEXT_H
