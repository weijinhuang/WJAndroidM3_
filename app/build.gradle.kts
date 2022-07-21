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

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

    Libs.commonDep(this)

}
