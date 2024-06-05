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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
}

dependencies {

    implementation(project(mapOf("path" to ":baseComponent")))
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
//    implementation(project(mapOf("path" to ":nativelib")))

    implementation(project(mapOf("path" to ":nativelib")))
    implementation("androidx.graphics:graphics-core:1.0.0")
    Libs.commonDep(this)

}
