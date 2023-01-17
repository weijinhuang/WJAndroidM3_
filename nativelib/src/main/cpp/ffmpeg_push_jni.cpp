//
// Created by HWJ on 2022/11/30.
//

#include "ff_rtmp_pusher.h"
#include "jni.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_push(JNIEnv *env, jobject thiz, jstring inputPath, jstring outputPath) {
    int ret;
    if (!inputPath || !outputPath) {
        return -1;
    }

    const char *input_path = env->GetStringUTFChars(inputPath, JNI_FALSE);
    const char *output_path = env->GetStringUTFChars(outputPath, JNI_FALSE);

    auto *pusher = new FFRtmpPusher();
    ret = pusher->open(input_path, output_path);
    if (ret < 0) {
        LOGE(LOG_TAG, "pusher->open ERROR:%d", ret);
        return ret;
    }
    ret = pusher->push();

    pusher->close();
    env->ReleaseStringUTFChars(inputPath, input_path);
    env->ReleaseStringUTFChars(outputPath, output_path);
    return ret;
}
