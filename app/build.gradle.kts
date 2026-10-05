plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ktakata.setcam"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ktakata.setcam"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.effects)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.preference)
    testImplementation(libs.junit)
}
