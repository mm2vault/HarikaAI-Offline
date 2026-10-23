plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val buildNumber = providers.gradleProperty("buildNumber").orNull?.toIntOrNull() ?: 1
val signingStoreFile = System.getenv("AURA_STORE_FILE")
val signingStorePassword = System.getenv("AURA_STORE_PASSWORD")
val signingKeyAlias = System.getenv("AURA_KEY_ALIAS")
val signingKeyPassword = System.getenv("AURA_KEY_PASSWORD")
val hasReleaseSigning = !signingStoreFile.isNullOrBlank() &&
    !signingStorePassword.isNullOrBlank() &&
    !signingKeyAlias.isNullOrBlank() &&
    !signingKeyPassword.isNullOrBlank()

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
    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
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
