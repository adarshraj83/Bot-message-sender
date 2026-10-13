plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
}

android {
namespace = "com.ghantas.embedsender"
compileSdk = 35

defaultConfig {
    applicationId = "com.ghantas.embedsender"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}

}