import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler


object Libs {

    fun commonDep(dependency: DependencyHandler) {
        dependency.apply {
            implementation("androidx.core:core-ktx:1.8.0")
            implementation("androidx.appcompat:appcompat:1.4.2")
            implementation("com.google.android.material:material:1.7.0-alpha02")
            implementation("androidx.constraintlayout:constraintlayout:2.1.4")
            implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
            implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
            implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
            implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
            implementation("com.jakewharton.timber:timber:5.0.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.2")
            implementation("androidx.startup:startup-runtime:1.2.0-alpha01")

            testImplementation("junit:junit:4.13.2")
            androidTestImplementation("androidx.test.ext:junit:1.1.3")
            androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")


            implementation (platform("com.google.firebase:firebase-bom:30.2.0"))
            implementation ("com.google.firebase:firebase-analytics-ktx")
        }
    }


}

fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? =
    add("testImplementation", dependencyNotation)

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? =
    add("androidTestImplementation", dependencyNotation)
