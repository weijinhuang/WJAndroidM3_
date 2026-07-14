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

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            // 项目里 Android Java 编译已经配置为 Java 21。
            // Kotlin 2.2 会校验 Java/Kotlin 目标版本是否一致；如果 Kotlin 仍用默认 17，
            // 编译会在进入业务代码前失败，所以这里统一所有子模块的 Kotlin bytecode 目标。
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

//task clean(type: Delete) {
//    delete rootProject.buildDir
//}
task("clean", Delete::class) {
    delete(rootProject.buildDir)
    println("----clear build 1----")
}
