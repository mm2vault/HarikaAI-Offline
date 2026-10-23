plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val buildNumber = providers.gradleProperty("buildNumber").orNull?.toIntOrNull() ?: 1
android {
    namespace = "ai.harika.jarvis"
    compileSdk = 35
    defaultConfig {
        applicationId = "ai.harika.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
}
