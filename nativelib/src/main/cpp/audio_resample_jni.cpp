//
// Created by HWJ on 2022/11/26.
//

#include <jni.h>
#include "ff_audio_resample.h"
#include "ffmpeg/include/WJACCEncoder.h"

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

extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_pcm2aac(JNIEnv *env, jobject thiz, jstring pcm_file_path, jstring aac_file_path, jint sample_rate,
                                               jint channel_count, jint sample_format) {
    const char *pcm = env->GetStringUTFChars(pcm_file_path, JNI_FALSE);
    const char *aac = env->GetStringUTFChars(aac_file_path, JNI_FALSE);
    WJACCEncoder encoder;
    int ret = encoder.EncodeStart(aac, sample_rate, channel_count, sample_format);
    if (ret < 0) {
        LOGE(TAG, "编码器初始化失败");
        env->ReleaseStringUTFChars(pcm_file_path, pcm);
        env->ReleaseStringUTFChars(aac_file_path, aac);
        return;
    }
    FILE *fp = fopen(pcm, "rb");
    if (!fp) {
        LOGE(TAG, "打开PCM文件失败: %s", pcm);
        encoder.EncodeStop();
        env->ReleaseStringUTFChars(pcm_file_path, pcm);
        env->ReleaseStringUTFChars(aac_file_path, aac);
        return;
    }
    const int BUF_SIZE = 4096;
    unsigned char *buf = (unsigned char *) malloc(BUF_SIZE);
    size_t readBytes = 0;
    while ((readBytes = fread(buf, 1, BUF_SIZE, fp)) > 0) {
        encoder.EncodeBuffer(buf, (int) readBytes);
    }
    free(buf);
    fclose(fp);
    encoder.EncodeStop();
    env->ReleaseStringUTFChars(pcm_file_path, pcm);
    env->ReleaseStringUTFChars(aac_file_path, aac);
}
