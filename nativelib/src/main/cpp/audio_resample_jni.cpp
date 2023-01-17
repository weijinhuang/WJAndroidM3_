//
// Created by HWJ on 2022/11/26.
//

#include <jni.h>
#include "ff_audio_resample.h"

#define  TAG "AUDIO_RESAMPLE"
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_audioResample(JNIEnv *env, jobject thiz, jstring input_path, jstring output_path, jint sample_rate) {

    const char *in_path = env->GetStringUTFChars(input_path, JNI_FALSE);
    const char *out_path = env->GetStringUTFChars(output_path, JNI_FALSE);
    FFAudioResample *audioResample = new FFAudioResample();
    audioResample->resampling(in_path, out_path, sample_rate);
    LOGE(TAG, "done...");
    delete audioResample;

}
