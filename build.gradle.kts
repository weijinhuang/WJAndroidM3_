// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
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

