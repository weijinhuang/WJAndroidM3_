plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}


android {
    compileSdk = Versions.COMPILE_SDK

    defaultConfig {
        minSdk = Versions.MIN_SDK
        targetSdk = Versions.TARGET_SDK
//        versionCode = Versions.VERSION
//        versionName = Versions.VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        consumerProguardFiles "consumer-rules.pro"
        consumerProguardFiles.add(File("consumer-rules.pro"))

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }



    buildTypes {
        getByName("release") {
            isMinifyEnabled = false

            isJniDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isJniDebuggable = true
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

//    implementation 'androidx.core:core-ktx:1.7.0'
//    implementation 'androidx.appcompat:appcompat:1.4.2'
//    implementation 'com.google.android.material:material:1.6.1'
//    testImplementation 'junit:junit:4.13.2'
//    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
//    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
    Libs.commonDep(this)
    implementation(project(mapOf("path" to ":baseComponent")))
}