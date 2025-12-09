package com.wj.androidm3.business.ui.audio

import androidx.lifecycle.MutableLiveData
import com.wj.basecomponent.vm.BaseViewModel
import java.io.File

class AudioTestViewModel : BaseViewModel() {

    val isRecording = MutableLiveData(false)
    val isPlaying = MutableLiveData(false)
    val pcmFiles = MutableLiveData<List<File>>(emptyList())
    val selectedIndex = MutableLiveData<Int?>(null)
    val selectedFilePath = MutableLiveData<String?>(null)

    // TODO: Implement audio recording logic
    fun toggleRecording() {
        isRecording.value = !(isRecording.value ?: false)
    }

    fun loadPcmFiles(directory: File, aacDirectory: File? = null) {
        val files = directory.listFiles { _, name -> name.endsWith(".pcm") }
        val aacFiles = aacDirectory?.listFiles { _, name -> name.endsWith(".aac") }
        val mutableList = mutableListOf<File>()
        if(!files.isNullOrEmpty()){
            mutableList.addAll(files)
        }
        if(!aacFiles.isNullOrEmpty()){
            mutableList.addAll(aacFiles)
        }
        pcmFiles.postValue(mutableList)
    }
}
