plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ai.harika.jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.harika.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("AURA_VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("AURA_VERSION_NAME").get()
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
