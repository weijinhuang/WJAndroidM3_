//
// Created by HWJ on 2023/4/30.
//

#include "ffmpeg/WJWavRecorder.h"

WJWavRecorder::WJWavRecorder() {

}

int WJWavRecorder::init(char *filePath) {
    file = fopen(filePath, "r+");
    if (nullptr != file) {
        LOGI(LOG_TAG, "打开文件成功，%s", filePath);
        return 1;
    } else {
        LOGI(LOG_TAG, "打开文件失败");
        return -1;
    }

}

int WJWavRecorder::writeFile(unsigned char *chars, int len) {
    fwrite(chars, sizeof(unsigned char), len, file);
    return 0;
}

void WJWavRecorder::stopWriteFile() {
    fclose(file);
}
