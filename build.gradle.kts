// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.android") version "1.7.10" apply false
    id("com.android.application") version "7.2.1" apply false
    id("com.android.library") version "7.2.1" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.12")
    }
}

//task clean(type: Delete) {
//    delete rootProject.buildDir
//}
task("clean", Delete::class) {
    delete(rootProject.buildDir)
    println("----clear build 1----")
}

