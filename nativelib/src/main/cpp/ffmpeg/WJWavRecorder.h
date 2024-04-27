//
// Created by HWJ on 2023/4/30.
//

#ifndef WJANDROIDM3_WJWAVRECORDER_H
#define WJANDROIDM3_WJWAVRECORDER_H

#include <stdio.h>
#include "wj_log.h"

class WJWavRecorder {
public :
    WJWavRecorder();

    int init(char *filePath);

    int writeFile(unsigned char[], int len);

    void stopWriteFile();

private:
    FILE *file;
};

#endif //WJANDROIDM3_WJWAVRECORDER_H

