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


class MediaRecorderContext {


public :
    MediaRecorderContext();

    ~MediaRecorderContext();

    static void createContext(JNIEnv *env, jobject instance);

    static void storeContext(JNIEnv *env, jobject instance, MediaRecorderContext *pContext);

    static void deleteContext(JNIEnv *env, jobject instance);

    static MediaRecorderContext* getContext(JNIEnv *env,jobject instance);

    int UnInit();

    int Init();

    int startRecord(int recorderType, const char* outUrl, int frameWidth, int frameHeight,  long videoBitRate, int fps);

private:
    static jfieldID s_ContextHandle;


};

}

#endif //WJANDROIDM3_MEDIA_RECORD_CONTEXT_H
