//
// Created by HWJ on 2022/11/17.
//

#include "wj_audio_player.h"
#include "wj_log.h"
#include <jni.h>

#include <unistd.h>

#define SLEEP_TIME (8000)
extern "C"
JNIEXPORT void JNICALL
Java_com_wj_nativelib_WJMediaJNIHepler_playAudio(JNIEnv *env, jobject thiz, jstring file_path) {
    if (file_path == nullptr) {
        return;
    }
    const char *native_path = env->GetStringUTFChars(file_path, JNI_FALSE);
    int result = 0;
    WJAudioPlayer *wjAudioPlayer = new WJAudioPlayer();
    int ret = wjAudioPlayer->open(native_path);
    if (ret < 0) {
        LOGI("AudioPlayer", "wjAudioPlayer->open(native_path) %d", ret);
        return;
    }
    jclass audio_class = env->GetObjectClass(thiz);
    jmethodID createAudioTrackJava = env->GetMethodID(audio_class, "createAudioTrack", "(II)Landroid/media/AudioTrack;");
    jobject androidAudioTrack = env->CallObjectMethod(thiz, createAudioTrackJava, wjAudioPlayer->getSimpleRate(), wjAudioPlayer->getChannel());
    jclass audio_track_class = env->GetObjectClass(androidAudioTrack);
    jmethodID play_method = env->GetMethodID(audio_track_class, "play", "()V");
    env->CallVoidMethod(androidAudioTrack, play_method);
    jmethodID write_method = env->GetMethodID(audio_track_class, "write", "([BII)I");
    result = wjAudioPlayer->decodeAudio();
    while (result >= 0) {
        result = wjAudioPlayer->decodeAudio();
        if (result == 0) {
            continue;
        } else if (result < 0) {
            break;
        }
        int size = result;
        jbyteArray audio_array = env->NewByteArray(size);
        jbyte *data_address = env->GetByteArrayElements(audio_array, JNI_FALSE);
        memcpy(data_address, wjAudioPlayer->getDecodeFrame(), size);
        env->ReleaseByteArrayElements(audio_array, data_address, 0);
        env->CallIntMethod(androidAudioTrack, write_method, audio_array, 0, size);
        env->DeleteLocalRef(audio_array);

//        usleep(SLEEP_TIME);
    }
    env->ReleaseStringUTFChars(file_path, native_path);
    jmethodID release_method = env->GetMethodID(audio_class, "releaseAudioTrack", "()V");
    env->CallVoidMethod(thiz, release_method);
    wjAudioPlayer->close();
    delete wjAudioPlayer;
}