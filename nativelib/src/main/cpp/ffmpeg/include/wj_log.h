//
// Created by HWJ on 2022/11/17.
//

#ifndef WJANDROIDM3_WJ_LOG_H
#define WJANDROIDM3_WJ_LOG_H

#include "android/log.h"

#define LOG_TAG "wjandroid"

#define LOGI(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, TAG, FORMAT, ##__VA_ARGS__)
#define LOGE(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, FORMAT, ##__VA_ARGS__)
#endif //WJANDROIDM3_WJ_LOG_H
