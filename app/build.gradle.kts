plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    compileSdk = Versions.COMPILE_SDK

    defaultConfig {
        minSdk = Versions.MIN_SDK
        targetSdk = Versions.TARGET_SDK
        versionCode = Versions.VERSION
        versionName = Versions.VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
    flavorDimensions += "ver"
    flavorDimensions += "env"

    productFlavors {
        create("free") {

            dimension = "ver"
            applicationId = Versions.APPLICATION_ID_FREE
        }

        create("full") {

            dimension = "ver"
            applicationId = Versions.APPLICATION_ID_FULL
        }
        create("cn") {
            dimension = "env"
        }

        create("en") {

            dimension = "env"
        }
        create("orange") {

            dimension = "env"
        }

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isJniDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    sourceSets {
        getByName("main") {
            aidl {
                srcDirs("src\\main\\aidl", "src\\main\\aidl")
            }
        }
    }
    namespace = "com.wj.androidm3"
}

dependencies {

    implementation(project(mapOf("path" to ":baseComponent")))
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // Room 2.6.x cannot read this project's Kotlin 2.2 metadata.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
//    implementation(project(mapOf("path" to ":nativelib")))

    implementation(project(mapOf("path" to ":nativelib")))
    implementation("androidx.graphics:graphics-core:1.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")
    // WebRTC 负责实时音视频里最难的部分：摄像头/麦克风采集、硬件编解码、
    // RTP/RTCP 传输、抖动缓冲、丢包恢复、回声消除，以及远端音视频播放。
    // 本功能只需要自己实现局域网内的“请求/接受/SDP/ICE”信令交换。
    implementation("org.webrtc:google-webrtc:1.0.32006")
//    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
//    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    Libs.commonDep(this)

}
